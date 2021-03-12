@file:JsModule("styled-components")
@file:JsNonModule

package styled

import kotlinext.js.TemplateTag
import react.dom.WithClassName

external interface StyledProps : WithClassName {
    @JsName("_css")
    var css: String
}

external interface Keyframes {
    val rules: Array<out dynamic>
    fun getName(): String
}

/**
 * A helper method to create keyframes for animations.
 */
external val keyframes: TemplateTag<Nothing, Keyframes>

/**
 * A helper function to generate CSS from a template literal with interpolations.
 * You need to use this if you return a template literal with functions inside an
 * interpolation due to how tagged template literals work in JavaScript.
 */
external val css: TemplateTag<dynamic, String>
