// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class DvvModificationsRequest(val viimeisinKirjausavain: Long, val hetulista: List<String>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DvvModificationsResponse(
    val viimeisinKirjausavain: String,
    val muutokset: List<DvvModification>,
    val ajanTasalla: Boolean,
)

data class DvvModification(
    val muutospv: String?,
    val henkilotunnus: String,
    val tietoryhmat: List<Tietoryhma>,
)

data class DvvBasicInfoRequest(val hetulista: List<String>, val tietoryhmat: List<String>)

data class DvvBasicInfoResponse(val perustiedot: List<DvvBasicInfo>, val ajanTasalla: Boolean?)

data class DvvBasicInfo(val henkilotunnus: String, val tietoryhmat: List<Tietoryhma>)
