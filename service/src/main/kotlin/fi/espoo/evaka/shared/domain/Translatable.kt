// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import org.jdbi.v3.json.Json

@Json data class Translatable(
    val fi: String,
    val sv: String,
    val en: String
)
