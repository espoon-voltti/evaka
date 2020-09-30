// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.mapper

import fi.espoo.evaka.vtjclient.soap.HenkiloTunnusKyselyResBody
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo
import javax.xml.bind.JAXBElement

interface IVTJResponseMapper {
    fun mapResponseToHenkilo(response: JAXBElement<HenkiloTunnusKyselyResBody>): Henkilo?
}
