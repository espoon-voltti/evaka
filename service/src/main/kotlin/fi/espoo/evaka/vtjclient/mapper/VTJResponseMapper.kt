// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.mapper

import fi.espoo.evaka.vtjclient.soap.HenkiloTunnusKyselyResBody
import fi.espoo.evaka.vtjclient.soap.HenkiloTunnusKyselyResType
import fi.espoo.evaka.vtjclient.soap.ObjectFactory
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma
import jakarta.xml.bind.JAXBElement
import org.springframework.stereotype.Service

@Service
class VTJResponseMapper {
    fun mapResponseToHenkilo(
        response: JAXBElement<HenkiloTunnusKyselyResBody>
    ): VTJHenkiloVastaussanoma.Henkilo? =
        response.value.response
            .let(HenkiloTunnusKyselyResType::mapResponse)
            .let(VTJResponse::mapToHenkiloOrLogError)
}

data class VTJResponse(
    val henkiloSanoma: VTJHenkiloVastaussanoma? = null,
    val faultString: String? = null,
    val faultCode: String? = null,
) {

    fun mapToHenkiloOrLogError(): VTJHenkiloVastaussanoma.Henkilo? =
        if (henkiloSanoma == null) {
            if (faultString != "0001") {
                error("VTJ Responded with faultCode: \"$faultCode\" faultString: \"$faultString\"")
            }
            null
        } else {
            // possible content: any of AsiakasInfo, Paluukoodi, Hakuperusteet and Henkilo
            henkiloSanoma.asiakasinfoOrPaluukoodiOrHakuperusteet
                ?.mapNotNull { it as? VTJHenkiloVastaussanoma.Henkilo }
                ?.firstOrNull() ?: error("VTJ response did not contain results for a person")
        }
}

private val faultCodeName = ObjectFactory().createHenkiloTunnusKyselyResTypeFaultString("").name

private fun HenkiloTunnusKyselyResType.mapResponse(): VTJResponse =
    this.vtjHenkiloVastaussanomaAndFaultCodeAndFaultString.fold(VTJResponse()) {
        result: VTJResponse,
        field: Any? ->
        when (field) {
            is JAXBElement<*> ->
                if (field.name == faultCodeName) {
                    result.copy(faultCode = "${field.value}")
                } else {
                    result.copy(faultString = "${field.value}")
                }
            is VTJHenkiloVastaussanoma -> result.copy(henkiloSanoma = field)
            else -> throw IllegalStateException("Unexpected error parsing VTJ response")
        }
    }
