// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.mapper

import fi.espoo.evaka.vtjclient.soap.HenkiloTunnusKyselyResBody
import fi.espoo.evaka.vtjclient.soap.HenkiloTunnusKyselyResType
import fi.espoo.evaka.vtjclient.soap.ObjectFactory
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import javax.xml.bind.JAXBElement

private val logger = KotlinLogging.logger {}

class VtjPersonNotFoundException(message: String?) : RuntimeException(message)

@Service
class VTJResponseMapper : IVTJResponseMapper {

    // TODO: Do we want to get some other info from the response than strictly the data about the person?
    override fun mapResponseToHenkilo(response: JAXBElement<HenkiloTunnusKyselyResBody>): VTJHenkiloVastaussanoma.Henkilo? =
        response.value.response
            .let(HenkiloTunnusKyselyResType::mapResponse)
            .let(VTJResponse::mapToHenkiloOrLogError)
}

data class VTJResponse(
    val henkiloSanoma: VTJHenkiloVastaussanoma? = null,
    val faultString: String? = null,
    val faultCode: String? = null
) {

    fun mapToHenkiloOrLogError(): VTJHenkiloVastaussanoma.Henkilo? =
        if (henkiloSanoma == null) {
            if (faultString != "0001") {
                logger.error { "VTJ Responded with faultCode: \"$faultCode\" faultString: \"$faultString\"" }
                null
            } else {
                throw VtjPersonNotFoundException("Vtj person not found")
            }
        } else {
            henkiloSanoma // possible content: any of AsiakasInfo, Paluukoodi, Hakuperusteet and Henkilo
                .asiakasinfoOrPaluukoodiOrHakuperusteet
                ?.mapNotNull { it as? VTJHenkiloVastaussanoma.Henkilo }
                ?.firstOrNull()
                ?: run {
                    logger.error { "VTJ response did not contain results for a person" }
                    null
                }
        }
}

private val faultCodeName = ObjectFactory().createHenkiloTunnusKyselyResTypeFaultString("").name

private fun HenkiloTunnusKyselyResType.mapResponse(): VTJResponse =
    this.vtjHenkiloVastaussanomaAndFaultCodeAndFaultString
        .fold(VTJResponse()) { result: VTJResponse, field: Any? ->
            when (field) {
                is JAXBElement<*> -> if (field.name == faultCodeName) result.copy(faultCode = "${field.value}") else result.copy(
                    faultString = "${field.value}"
                )
                is VTJHenkiloVastaussanoma -> result.copy(henkiloSanoma = field)
                else -> throw IllegalStateException("Unexpected error parsing VTJ response")
            }
        }
