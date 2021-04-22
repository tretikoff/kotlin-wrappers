package styled

import kotlinext.js.asJsObject
import kotlinext.js.clone
import kotlinext.js.getOwnPropertyNames
import kotlinext.js.jsObject
import kotlinx.browser.window
import kotlinx.css.CSSBuilder
import kotlinx.css.CssClass
import kotlinx.css.CssRules
import kotlinx.css.RuleSet
import kotlinx.html.*
import org.w3c.dom.Element
import react.*
import react.dom.*
import kotlin.js.Promise

typealias AnyTagStyledBuilder = StyledDOMBuilder<CommonAttributeGroupFacade>
typealias AnyBuilder = AnyTagStyledBuilder.() -> Unit

typealias HTMLTagBuilder = StyledDOMBuilder<HTMLTag>.() -> Unit

typealias ABuilder = StyledDOMBuilder<A>.() -> Unit
typealias DIVBuilder = StyledDOMBuilder<DIV>.() -> Unit
typealias SPANBuilder = StyledDOMBuilder<SPAN>.() -> Unit
typealias INPUTBuilder = StyledDOMBuilder<INPUT>.() -> Unit
typealias CssToClassMap = HashMap<String, String>

// TODO somehow remove classes which are no longer used
private val styledClasses = CssToClassMap()

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

    val css = strings.joinToString(" ")
    var className = styledClasses[css]
    if (className == null) {
        className = generateClassName("ksc-keyframe-")
        injectGlobalKeyframeStyle(className, strings[0])
        styledClasses[css] = className
    }

    return Keyframes(className, stringifyRules(strings, className, "@keyframes"))
}

class Keyframes(private val name: String, val rules: Array<String>) {
    fun getName(): String {
        return name
    }
}

fun stringifyRules(rules: Array<String>, selector: String, prefix: String): Array<String> {
    val commentRegex = Regex("^\\s*//.*$")
    val flatCSS = rules.joinToString(" ").replace(commentRegex, "") // replace JS comments
    val cssStr = "$prefix $selector { $flatCSS }"
    return arrayOf(cssStr)
}

fun css(styles: Array<String>): Array<String> {
    return styles
}

/**
 * @deprecated Use [keyframes] and [css] instead
 */
fun keyframesName(string: String): String {
    val keyframes = keyframes(arrayOf(string))
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

fun injectGlobals(strings: Array<String>) {
    if (strings.isEmpty()) return
    Promise.resolve(Unit).then {
        GlobalStyles.add(strings.toList())
    }
}

private external interface GlobalStylesComponentProps : RProps {
    var globalStyles: List<Any>
}

private object GlobalStyles {
    private val component = functionalComponent<GlobalStylesComponentProps> { props ->
        child("style", jsObject {}, props.globalStyles)
    }

    private val styles = mutableListOf<String>()

    fun add(globalStyle: List<String>) {
        styles.addAll(globalStyle)
        val reactElement = createElement<GlobalStylesComponentProps>(component, jsObject {
            this.globalStyles = styles
        })
        render(reactElement, root)
    }

    private val root by kotlin.lazy {
        val element = window.document.body!!.appendChild(window.document.createElement("div")) as Element
        element.setAttribute("id", "ksc-global-styles")
        element
    }
}

/**
 * @deprecated Use [createGlobalStyleComponent] instead
 */
fun injectGlobal(string: String) {
    Promise.resolve(Unit).then {
        GlobalStyles.add(listOf(string))
    }
}

@JsModule("react")
@JsNonModule
external object ReactModule

/**
 * @deprecated Use [createGlobalStyleComponent] instead
 */
fun injectGlobal(handler: CSSBuilder.() -> Unit) {
    injectGlobal(CSSBuilder().apply { handler() }.toString())
}

fun generateClassName(prefix: String): String = prefix + List(6) {
    (('a'..'z') + ('A'..'Z')).random()
}.joinToString("")

fun createStyleSheet(cssClasses: CssToClassMap) {
    val rules = cssClasses.map {
        val css = it.key
        val className = it.value
        if (css.contains("&")) {
            css.replace("&", ".${className}")
        } else {
            ".$className {\n$css}"
        }
    }
    injectGlobals(rules.toTypedArray())
}

external interface StyledProps : WithClassName {
    var css_rules: CssRules?
    var css_classes: ArrayList<CssClass>?
}

fun customStyled(type: String): FunctionalComponent<StyledProps> {
    var fc = forwardRef<StyledProps> { props, rRef ->
        val rules = props.css_rules
        val newProps = clone(props)
        val cssClasses = CssToClassMap()
        useStructMemo(arrayOf(rules)) {
            if (rules != null) {
                val it = rules.iterator()
                while (it.hasNext()) {
                    val cssKey = it.next()
                    var className = styledClasses[cssKey]
                    if (className == null) {
                        className = generateClassName("ksc-")
                        cssClasses[cssKey] = className
                        styledClasses[cssKey] = className
                    } else {
                        it.remove()
                    }
                    newProps.className += " $className"
                }
            }
        }

        val classes = props.css_classes
        useStructEffect(listOf(cssClasses)) { createStyleSheet(cssClasses) }
        useStructEffect(listOf(classes)) { classes?.forEach { it.inject() } }
        newProps.css_classes = null
        newProps.css_rules = null
        newProps.ref = rRef
        child(createElement(type, newProps))
    }.unsafeCast<FunctionalComponent<StyledProps>>()
    fc = memo(fc)
    { oldProps: StyledProps, newProps: StyledProps ->
        if (Children.toArray(oldProps.children).isNotEmpty() || Children.toArray(newProps.children).isNotEmpty()) {
            false
        } else {
            for (key in oldProps.getOwnPropertyNames()) {
                if (!(oldProps.asDynamic()[key].equals(newProps.asDynamic()[key]))) {
                    console.log("not equals ", oldProps, newProps)
                    return@memo false
                }
            }
            console.log("empty equals", oldProps.className)
            return@memo true
        }
    }
    fc.asDynamic().displayName = "styled${type.capitalize()}"
    return fc
}

object Styled {
    private val cache = mutableMapOf<dynamic, dynamic>()

    private fun wrap(type: dynamic) =
        cache.getOrPut<dynamic, FunctionalComponent<StyledProps>>(type) {
            customStyled(type)
        }

    fun createElement(type: Any, css: CSSBuilder, props: WithClassName, children: List<Any>): ReactElement {
        val wrappedType = wrap(type)
        val styledProps = props.unsafeCast<StyledProps>()
        if (css.rules.isNotEmpty() || css.multiRules.isNotEmpty() || css.declarations.isNotEmpty()) {
            val cssRules = css.buildCssRules()
            styledProps.css_rules = cssRules
        }
        styledProps.css_classes = css.cssClasses
        styledProps.className = css.classes.joinToString(separator = " ")
        if (css.styleName.isNotEmpty()) {
            styledProps.asDynamic()["data-style"] = css.styleName.joinToString(separator = " ")
        }
        return createElement(wrappedType, styledProps, *children.toTypedArray())
    }
}
