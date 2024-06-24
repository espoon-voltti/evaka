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
            val maybeClass = className?.let { " class=\"${HtmlEscape.escapeHtml5Xml(it)}\"" } ?: ""
            return if (children.isEmpty()) {
                "<$tag$maybeClass/>"
            } else {
                """
                <$tag$maybeClass>
                    ${children.joinToString("\n") { it.toHtml() }}
                </$tag>
                """.trimIndent()
            }
        }
    }

    data class Text(
        val text: String
    ) : HtmlElement() {
        override fun toHtml(): String = HtmlEscape.escapeHtml5Xml(text)
    }
}

data object HtmlBuilder {
    fun text(text: String) = HtmlElement.Text(text)

    fun multilineText(text: String) = text.split("\n").flatMap { listOf(text(it), br()) }

    fun body(children: HtmlBuilder.() -> List<HtmlElement> = { emptyList() }) = HtmlElement.Tag("body", HtmlBuilder.children())

    fun br(
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        className: String? = null
    ) = HtmlElement.Tag("br", emptyList(), className)

    fun div(
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        className: String? = null,
        children: HtmlBuilder.() -> List<HtmlElement> = { emptyList() }
    ) = HtmlElement.Tag("div", HtmlBuilder.children(), className)

    fun div(
        text: String,
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        className: String? = null
    ) = HtmlElement.Tag("div", listOf(HtmlElement.Text(text)), className)

    fun h1(
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        className: String? = null,
        children: HtmlBuilder.() -> List<HtmlElement> = { emptyList() }
    ) = HtmlElement.Tag("h1", HtmlBuilder.children(), className)

    fun h1(
        text: String,
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        className: String? = null
    ) = HtmlElement.Tag("h1", listOf(HtmlElement.Text(text)), className)

    fun h2(
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        className: String? = null,
        children: HtmlBuilder.() -> List<HtmlElement> = { emptyList() }
    ) = HtmlElement.Tag("h2", HtmlBuilder.children(), className)

    fun h2(
        text: String,
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        className: String? = null
    ) = HtmlElement.Tag("h2", listOf(HtmlElement.Text(text)), className)

    fun h3(
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        className: String? = null,
        children: HtmlBuilder.() -> List<HtmlElement> = { emptyList() }
    ) = HtmlElement.Tag("h3", HtmlBuilder.children(), className)

    fun h3(
        text: String,
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        className: String? = null
    ) = HtmlElement.Tag("h3", listOf(HtmlElement.Text(text)), className)

    fun label(
        className: String? = null,
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        children: HtmlBuilder.() -> List<HtmlElement>
    ) = HtmlElement.Tag("label", HtmlBuilder.children(), className)

    fun label(
        text: String,
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        className: String? = null
    ) = HtmlElement.Tag("label", listOf(HtmlElement.Text(text)), className)

    fun li(
        text: String,
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        className: String? = null
    ) = HtmlElement.Tag("li", listOf(HtmlElement.Text(text)), className)

    fun ul(
        className: String? = null,
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        children: HtmlBuilder.() -> List<HtmlElement.Tag>
    ) = HtmlElement.Tag("ul", HtmlBuilder.children(), className)

    fun p(
        text: String,
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        className: String? = null
    ) = HtmlElement.Tag("p", listOf(HtmlElement.Text(text)), className)

    fun span(
        className: String? = null,
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        children: HtmlBuilder.() -> List<HtmlElement>
    ) = HtmlElement.Tag("span", HtmlBuilder.children(), className)

    fun span(
        text: String,
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        className: String? = null
    ) = HtmlElement.Tag("span", listOf(HtmlElement.Text(text)), className)

    fun table(
        className: String? = null,
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        children: HtmlBuilder.() -> List<HtmlElement>
    ) = HtmlElement.Tag("table", HtmlBuilder.children(), className)

    fun td(
        text: String,
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        className: String? = null
    ) = HtmlElement.Tag("td", listOf(HtmlElement.Text(text)), className)

    fun th(
        text: String,
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        className: String? = null
    ) = HtmlElement.Tag("th", listOf(HtmlElement.Text(text)), className)

    fun tr(
        className: String? = null,
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        children: HtmlBuilder.() -> List<HtmlElement>
    ) = HtmlElement.Tag("tr", HtmlBuilder.children(), className)
}

class UseNamedArguments private constructor()
