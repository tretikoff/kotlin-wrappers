package styled

import kotlinext.js.*
import kotlinx.css.*
import kotlinx.html.*
import react.*
import react.dom.*

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

interface StyledElementBuilder<P : WithClassName> : RElementBuilder<P>, StyledBuilder<P> {
    fun create(): ReactElement

    companion object {
        operator fun <P : WithClassName> invoke(
            type: Any,
            attrs: P = jsObject()
        ): StyledElementBuilder<P> = StyledElementBuilderImpl(type, attrs)
    }
}

class StyledElementBuilderImpl<P : WithClassName>(
    override val type: Any,
    attrs: P = jsObject()
) : StyledElementBuilder<P>, RElementBuilderImpl<P>(attrs) {
    override val css = CSSBuilder()

    override fun create() = Styled.createElement(type, css, attrs, childList)
}

@ReactDsl
interface StyledDOMBuilder<out T : Tag> : RDOMBuilder<T>, StyledBuilder<DOMProps> {
    override val type: Any get() = attrs.tagName

    override fun create() = Styled.createElement(type, css, domProps, childList)

    companion object {
        operator fun <T : Tag> invoke(factory: (TagConsumer<Unit>) -> T): StyledDOMBuilder<T> =
            StyledDOMBuilderImpl(factory)
    }
}

class StyledDOMBuilderImpl<out T : Tag>(factory: (TagConsumer<Unit>) -> T) : StyledDOMBuilder<T>,
    RDOMBuilderImpl<T>(factory) {
    override val css = CSSBuilder()
}

inline fun CustomStyledProps.css(noinline handler: RuleSet) {
    if (css == null) {
        css = ArrayList()
    }
    css!!.add(handler)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <P : CustomStyledProps> RElementBuilder<P>.css(noinline handler: RuleSet) = attrs.css(handler)

fun css(styles: Array<String>): Array<String> {
    return styles
}

external interface StyledProps : WithClassName {
    var css: CSSBuilder
}

fun customStyled(type: String): RClass<StyledProps> {
    val fc = forwardRef<StyledProps> { props, rRef ->
        val css = props.css
        val classNames = useMemo(css) {
            GlobalStyles.getInjectedClassNames(css).joinToString(" ").also {
                GlobalStyles.injectScheduled()
            }
        }

        val newProps = clone(props)
        newProps.className = (if (props.className != undefined) props.className else "") + classNames
        newProps.ref = rRef
        newProps.asDynamic().css = undefined
        child(createElement(type, newProps))
    }
    fc.asDynamic().displayName = "styled${type.replaceFirstChar { it.titlecase() }}"
    return fc
}

object Styled {
    private val cache = mutableMapOf<dynamic, dynamic>()

    private fun wrap(type: dynamic) =
        cache.getOrPut<dynamic, RClass<StyledProps>>(type) {
            customStyled(type)
        }

    fun createElement(type: Any, css: CSSBuilder, props: WithClassName, children: List<Any>): ReactElement {
        val wrappedType = wrap(type)
        val styledProps = props.unsafeCast<StyledProps>()
        styledProps.css = css
        return createElement(wrappedType, styledProps, *children.toTypedArray())
    }
}
