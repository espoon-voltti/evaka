// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

data class CitizenFeatures(
    val messages: Boolean,
    val composeNewMessage: Boolean,
    val reservations: Boolean,
    val childDocumentation: Boolean,
)
