// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

data class BadRequest(override val message: String, val errorCode: String? = null) : RuntimeException()

data class NotFound(override val message: String, val errorCode: String? = null) : RuntimeException()

data class Conflict(override val message: String, val errorCode: String? = null) : RuntimeException()

data class Forbidden(override val message: String, val errorCode: String? = null) : RuntimeException()

data class Unauthorized(override val message: String, val errorCode: String? = null) : RuntimeException()
