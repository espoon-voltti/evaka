// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

inline fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit): T =
    if (condition) this.apply(block) else this
