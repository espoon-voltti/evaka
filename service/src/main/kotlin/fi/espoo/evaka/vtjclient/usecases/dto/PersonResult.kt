// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.usecases.dto

import fi.espoo.evaka.vtjclient.dto.VtjPersonDTO

sealed class PersonResult {
    data class NotFound(val msg: String = "Person not found") : PersonResult()
    data class Error(val msg: String = "Error retrieving details") : PersonResult()
    data class Result(val vtjPersonDTO: VtjPersonDTO) : PersonResult()
}
