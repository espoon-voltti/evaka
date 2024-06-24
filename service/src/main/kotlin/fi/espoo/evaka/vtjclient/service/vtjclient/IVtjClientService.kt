// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.vtjclient

import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma
import java.util.UUID

interface IVtjClientService {
    data class VTJQuery(
        val requestingUserId: UUID,
        val type: RequestType,
        val ssn: String
    )

    fun query(query: VTJQuery): VTJHenkiloVastaussanoma.Henkilo?

    enum class RequestType(
        val queryName: String
    ) {
        HUOLTAJA_HUOLLETTAVA("Huoltaja-Huollettavat"),
        HUOLLETTAVA_HUOLTAJAT("Huollettava-Huoltajat"),
        PERUSSANOMA3("Perussanoma 3"),
        VANHEMPI_LAPSET_JA_HUOLLETTAVAT("Vanhempi -lapset ja huollettavat"),
        ASUKASMAARA("ASUKASLKM2")
    }
}
