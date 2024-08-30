// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import org.unbescape.html.HtmlEscape

/**
 * Wraps data and escapes its string representation so that it can be safely used in HTML templates
 * without doing manual escaping every time.
 *
 * The wrapped data is intentionally private, so the only meaningful operation you can do is to call
 * `.toString()`, which returns an escaped HTML-safe string representation.
 */
@JvmInline
value class HtmlSafe<T>(private val data: T) {
    override fun toString(): String = HtmlEscape.escapeHtml5(data.toString())
}
