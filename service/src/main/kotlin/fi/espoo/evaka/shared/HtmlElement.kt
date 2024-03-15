// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import org.unbescape.html.HtmlEscape

sealed class HtmlElement {
    abstract fun toHtml(): String

    data class Tag(
        val tag: String,
        val children: List<HtmlElement> = emptyList(),
        val className: String? = null
    ) : HtmlElement() {
        override fun toHtml(): String {
            val maybeClass = className?.let { " class=\"$it\"" } ?: ""
            return if (children.isEmpty()) {
                "<$tag$maybeClass/>"
            } else {
                """
                    <$tag$maybeClass>
                        ${children.joinToString("\n") { it.toHtml() }}
                    </$tag>
                """
                    .trimIndent()
            }
        }
    }

    data class Text(val text: String) : HtmlElement() {
        override fun toHtml(): String {
            return HtmlEscape.escapeHtml5Xml(text)
        }
    }

    companion object {
        fun text(text: String) = Text(text)

        fun body(children: List<HtmlElement> = emptyList()) = Tag("body", children)

        fun body(child: HtmlElement) = body(listOf(child))

        fun br(className: String? = null) = Tag("br", children = emptyList(), className)

        fun div(children: List<HtmlElement> = emptyList(), className: String? = null) =
            Tag("div", children, className)

        fun div(child: HtmlElement, className: String? = null) = div(listOf(child), className)

        fun div(text: String, className: String? = null) = div(text(text), className)

        fun h1(text: String, className: String? = null) = Tag("h1", listOf(text(text)), className)

        fun h2(text: String, className: String? = null) = Tag("h2", listOf(text(text)), className)

        fun h3(text: String, className: String? = null) = Tag("h3", listOf(text(text)), className)

        fun h4(text: String, className: String? = null) = Tag("h4", listOf(text(text)), className)

        fun label(children: List<HtmlElement> = emptyList(), className: String? = null) =
            Tag("label", children, className)

        fun label(child: HtmlElement, className: String? = null) = label(listOf(child), className)

        fun label(text: String, className: String? = null) = label(text(text), className)
    }
}
