// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.utils

/**
 * Helper to force when to exhaust options. E.g.
 * ```
 * when(sealed) {
 *  is Sealed.Class -> doSomething()
 * }.exhaust()
 */
fun <T> T.exhaust(): T = this
