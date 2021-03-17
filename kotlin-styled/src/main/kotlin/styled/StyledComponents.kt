package styled

import kotlinext.js.*
import kotlinx.browser.window
import kotlinx.css.*
import kotlinx.html.*
import org.w3c.dom.*
import react.*
import react.dom.*
import kotlin.js.*

// TODO test all the corner cases
// TODO fix js console warning -  Render methods should be a pure function of props and state
// TODO reuse css styles
// TODO check the compiler warnings
typealias AnyTagStyledBuilder = StyledDOMBuilder<CommonAttributeGroupFacade>
typealias AnyBuilder = AnyTagStyledBuilder.() -> Unit

typealias HTMLTagBuilder = StyledDOMBuilder<HTMLTag>.() -> Unit

typealias ABuilder = StyledDOMBuilder<A>.() -> Unit
typealias DIVBuilder = StyledDOMBuilder<DIV>.() -> Unit
typealias SPANBuilder = StyledDOMBuilder<SPAN>.() -> Unit
typealias INPUTBuilder = StyledDOMBuilder<INPUT>.() -> Unit

external interface CustomStyledProps : RProps {
    var css: ArrayList<RuleSet>?
}

inline fun CustomStyledProps.forwardCss(builder: CSSBuilder) {
    css?.forEach { it(builder) }
}

inline fun CustomStyledProps.forwardCss(props: CustomStyledProps) {
    css?.forEach { c ->
        if (props.css == null) {
            props.css = ArrayList()
        }
        props.css!!.add(c)
    }
}

interface StyledBuilder<P : WithClassName> {
    val css: CSSBuilder
    val type: Any
}

inline fun StyledBuilder<*>.css(handler: RuleSet) = css.handler()

class StyledElementBuilder<P : WithClassName>(
    override val type: Any,
    attrs: P = jsObject()
) : RElementBuilder<P>(attrs), StyledBuilder<P> {
    override val css = CSSBuilder()

    fun create() = Styled.createElement(type, css, attrs, childList)
}

@ReactDsl
class StyledDOMBuilder<out T : Tag>(factory: (TagConsumer<Unit>) -> T) : RDOMBuilder<T>(factory),
    StyledBuilder<DOMProps> {
    override val type: Any = attrs.tagName
    override val css = CSSBuilder()

    override fun create() = Styled.createElement(type, css, props, childList)
}

typealias StyledHandler<P> = StyledElementBuilder<P>.() -> Unit

fun <P : WithClassName> styled(type: RClass<P>): RBuilder.(StyledHandler<P>) -> ReactElement = { handler ->
    child(with(StyledElementBuilder<P>(type)) {
        handler()
        create()
    })
}

inline fun CustomStyledProps.css(noinline handler: RuleSet) {
    if (css == null) {
        css = ArrayList()
    }
    css!!.add(handler)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <P : CustomStyledProps> RElementBuilder<P>.css(noinline handler: RuleSet) = attrs.css(handler)

fun keyframes(strings: Array<String>): Keyframes {
    /* Warning if you've used keyframes on React Native */
    if (
        js(
            "process.env.NODE_ENV !== 'production' &&" +
                    " typeof navigator !== 'undefined' &&" +
                    " navigator.product === 'ReactNative'"
        ) as Boolean
    ) {
        // eslint-disable-next-line no-console
        console.warn(
            "`keyframes` cannot be used on ReactNative, only on the web. To do animation in ReactNative please use Animated."
        )
    }

    // TODO get same name for the same rules
    // var name = generateAlphabeticName(murmurhash(replaceWhitespace(JSON.stringify(rules))));
    val name = generateClassName("")

    return Keyframes(name, stringifyRules(strings, name, "@keyframes"));
}

class Keyframes(private val name: String, val rules: Array<String>) {
    fun getName(): String {
        return name
    }
}


fun stringifyRules(rules: Array<String>, selector: String, prefix: String): Array<String> {
    val commentRegex = Regex("^\\s*//.*$")
    val flatCSS = rules.joinToString(" ").replace(commentRegex, ""); // replace JS comments
    val cssStr = "$prefix $selector { $flatCSS }"
    return arrayOf(cssStr)
}

fun css(styles: Array<String>): Array<String> {
    return styles;
}

/**
 * @deprecated Use [keyframes] and [css] instead
 */
fun keyframesName(string: String): String {
    val keyframes = keyframes(arrayOf(string))
    val keyframesInternal = css(keyframes.rules).asDynamic()
    val name = keyframes.getName()
    when {
        keyframesInternal is String -> injectGlobalKeyframeStyle(name, keyframesInternal)
        keyframesInternal is Array<String> -> injectGlobalKeyframeStyle(name, keyframesInternal[0])
        else -> injectGlobals(keyframesInternal)
    }
    return keyframes.getName()
}

private fun injectGlobalKeyframeStyle(name: String, style: String) {
    if (style.startsWith("@-webkit-keyframes") || style.startsWith("@keyframes")) {
        injectGlobal(style)
    } else {
        injectGlobals(
            arrayOf(
                "@-webkit-keyframes $name {$style}",
                "@keyframes $name {$style}"
            )
        )
    }
}

private fun injectGlobals(strings: Array<String>) {
    val globalStyle = createGlobalStyleComponent(strings.toList())
    Promise.resolve(Unit).then {
        GlobalStyles.add(globalStyle)
    }
}

private external interface GlobalStylesComponentProps : RProps {
    var globalStyles: List<Any>
}

private object GlobalStyles {
    private val component = functionalComponent<GlobalStylesComponentProps> { props ->
        props.globalStyles.forEach {
            child(it, jsObject {}, emptyList())
        }
    }

    private val styles = mutableListOf<FunctionalComponent<RProps>>()

    fun add(globalStyle: FunctionalComponent<RProps>) {
        styles.add(globalStyle)
        val reactElement = createElement<GlobalStylesComponentProps>(component, jsObject {
            this.globalStyles = styles
        })
        render(reactElement, root)
    }

    private val root by kotlin.lazy {
        val element = window.document.body!!.appendChild(window.document.createElement("div")) as Element
        element.setAttribute("id", "sc-global-styles")
        element
    }
}

/**
 * @deprecated Use [createGlobalStyleComponent] instead
 */
fun injectGlobal(string: String) {
    val globalStyle = createGlobalStyleComponent(listOf(string))
    Promise.resolve(Unit).then {
        GlobalStyles.add(globalStyle)
    }
}

var createGlobalStyleComponent = fun(css: Collection<String>): FunctionalComponent<RProps> {
    val cssStr = css.joinToString("\n")
    return functionalComponent<RProps> { props ->
        style {
            +cssStr
        }
    }
}

@JsModule("react")
@JsNonModule
external object ReactModule

private fun <T> devOverrideUseRef(action: () -> T): T {
    return if (js("process.env.NODE_ENV !== 'production'")) {
        // (Very) dirty hack: styled-components calls useRef() in development mode to check if a component
        // has been created dynamically. We can't allow this call to happen because it breaks rendering, so
        // we temporarily redefine useRef.
        val useRef = ReactModule.asDynamic().useRef
        ReactModule.asDynamic().useRef = {
            throw Error("invalid hook call")
        }
        val result = action()
        ReactModule.asDynamic().useRef = useRef
        result
    } else action()
}

/**
 * @deprecated Use [createGlobalStyleComponent] instead
 */
fun injectGlobal(handler: CSSBuilder.() -> Unit) {
    injectGlobal(CSSBuilder().apply { handler() }.toString())
}

fun generateClassName(prefix: String): String = prefix + List(6) {
    (('a'..'z') + ('A'..'Z')).random()
}.joinToString("")

fun createStyleSheet(cssAmp: CssRules, generatedClassName: String) {
    val rules =
        cssAmp.filter { it.contains("&") }.map { it.replace("&", ".$generatedClassName") }.toMutableList()
    rules.addAll(cssAmp.filter { !it.contains("&") }.map { ".$generatedClassName {\n$it}" }.toMutableList())
    GlobalStyles.add(createGlobalStyleComponent(rules))
}

external interface StyledProps : WithClassName {
    var css_rules: CssRules?
    var generated_class_name: String?
}

fun customStyled(type: String): FunctionalComponent<StyledProps> {
    return functionalComponent("styled${type.capitalize()}") { props ->
        val ampersandCss = props.css_rules
        val generatedClassName = props.generated_class_name
        if (generatedClassName != null && ampersandCss != null) {
            useMemo(ampersandCss) { createStyleSheet(ampersandCss, generatedClassName) }
        }
        val newProps = clone(props)
        newProps.generated_class_name = null
        newProps.css_rules = null
        child(createElement(type, newProps))
    }
}

object Styled {
    private val cache = mutableMapOf<dynamic, dynamic>()

    private fun wrap(type: dynamic) =
        cache.getOrPut(type) {
            customStyled(type)
        }


    fun createElement(type: Any, css: CSSBuilder, props: WithClassName, children: List<Any>): ReactElement {
        val wrappedType = wrap(type)
        val className = generateClassName("ksc-")
        val styledProps = props.unsafeCast<StyledProps>()
        if (css.rules.isNotEmpty() || css.multiRules.isNotEmpty() || css.declarations.isNotEmpty()) {
            css.classes.add(className)
            val cssRules = css.buildCssRules()
            styledProps.css_rules = cssRules
        }
        styledProps.generated_class_name = className
        styledProps.className = css.classes.joinToString(separator = " ")
        if (css.styleName.isNotEmpty()) {
            styledProps.asDynamic()["data-style"] = css.styleName.joinToString(separator = " ")
        }
        return createElement(wrappedType, styledProps, *children.toTypedArray())
    }
}
