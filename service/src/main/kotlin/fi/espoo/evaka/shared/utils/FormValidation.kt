//  SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
//  SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

val PHONE_PATTERN = "^[0-9 \\-+()]{6,20}$".toRegex()
val EMAIL_PATTERN = "^\\S+@\\S+$".toRegex()
