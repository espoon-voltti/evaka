// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.service.vtjclient

import evaka.core.VtjEnv
import evaka.core.vtjclient.config.SoapRequestAdapter
import evaka.core.vtjclient.mapper.VTJResponseMapper
import evaka.core.vtjclient.service.vtjclient.QueryStatus.CREATING_REQUEST
import evaka.core.vtjclient.service.vtjclient.QueryStatus.ERROR_DURING_REQUEST
import evaka.core.vtjclient.service.vtjclient.QueryStatus.RESPONSE_PARSING_FAILURE
import evaka.core.vtjclient.service.vtjclient.QueryStatus.RESPONSE_RECEIVED
import evaka.core.vtjclient.soap.HenkiloTunnusKyselyReqBody
import evaka.core.vtjclient.soap.HenkiloTunnusKyselyReqBodyTiedot
import evaka.core.vtjclient.soap.HenkiloTunnusKyselyResBody
import evaka.core.vtjclient.soap.ObjectFactory
import evaka.core.vtjclient.soap.VTJHenkiloVastaussanoma
import evaka.core.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo
import fi.espoo.voltti.logging.loggers.auditVTJ
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.xml.bind.JAXBElement
import java.util.UUID
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.ws.client.core.WebServiceMessageCallback
import org.springframework.ws.client.core.WebServiceTemplate

interface VtjSoapTransport {

    data class VTJQuery(val requestingUserId: UUID, val type: RequestType, val ssn: String)

    fun query(query: VTJQuery): VTJHenkiloVastaussanoma.Henkilo?

    enum class RequestType(val queryName: String) {
        HUOLTAJA_HUOLLETTAVA("Huoltaja-Huollettavat"),
        HUOLLETTAVA_HUOLTAJAT("Huollettava-Huoltajat"),
        PERUSSANOMA3("Perussanoma 3"),
        VANHEMPI_LAPSET_JA_HUOLLETTAVAT("Vanhempi -lapset ja huollettavat"),
        ASUKASMAARA("ASUKASLKM2"),
    }
}

@Service
@Profile("production", "vtj-dev", "integration-test")
class XroadVtjSoapTransport(
    private val vtjObjectFactory: ObjectFactory,
    private val wsTemplate: WebServiceTemplate,
    private val vtjEnv: VtjEnv,
    private val requestAdapter: SoapRequestAdapter,
    private val responseMapper: VTJResponseMapper,
) : VtjSoapTransport {

    private val logger = KotlinLogging.logger {}

    override fun query(query: VtjSoapTransport.VTJQuery): Henkilo? {
        val request = query.toRequest()

        logger.auditVTJ(toLogParamsMap(query = query, status = CREATING_REQUEST)) {
            "VTJ Query of type: ${query.type.queryName}"
        }

        val headerAddingCallback = requestAdapter.createCallback(query)

        val response: JAXBElement<HenkiloTunnusKyselyResBody> =
            try {
                wsTemplate.marshalSendAndReceiveAsType(request, headerAddingCallback)
            } catch (e: Exception) {
                logger.auditVTJ(toLogParamsMap(query = query, status = ERROR_DURING_REQUEST)) {
                    "There was an error requesting VTJ data. Results were not received."
                }
                throw e
            }

        val person = responseMapper.mapResponseToHenkilo(response)
        if (person == null) {
            logger.auditVTJ(toLogParamsMap(query = query, status = RESPONSE_PARSING_FAILURE)) {
                "Did not receive VTJ results"
            }
        } else {
            logger.auditVTJ(toLogParamsMap(query = query, status = RESPONSE_RECEIVED)) {
                "VTJ results received"
            }
        }
        return person
    }

    private inline fun <reified T> WebServiceTemplate.marshalSendAndReceiveAsType(
        request: Any,
        callback: WebServiceMessageCallback,
    ): JAXBElement<T> =
        marshalSendAndReceive(request, callback)
            .let {
                it as? JAXBElement<*>
                    ?: throw IllegalStateException("Unexpected VTJ response : $it")
            }
            .let {
                if (it.declaredType == T::class.java) {
                    @Suppress("UNCHECKED_CAST")
                    it as JAXBElement<T>
                } else {
                    throw IllegalStateException("Unexpected VTJ response type: ${it.declaredType}")
                }
            }

    fun VtjSoapTransport.VTJQuery.toRequest(): JAXBElement<HenkiloTunnusKyselyReqBody> {
        val requestContent =
            HenkiloTunnusKyselyReqBodyTiedot().also {
                it.soSoNimi = type.queryName
                it.kayttajatunnus = vtjEnv.username
                it.salasana = vtjEnv.password?.value
                it.loppukayttaja = "voltti-id: $requestingUserId"
                it.henkilotunnus = ssn
            }

        return HenkiloTunnusKyselyReqBody()
            .apply { request = requestContent }
            .let { vtjObjectFactory.createHenkilonTunnusKysely(it) }
    }
}

fun toLogParamsMap(query: VtjSoapTransport.VTJQuery, status: QueryStatus) =
    mapOf(
        "meta" to mapOf("queryName" to query.type.queryName),
        "status" to status.value,
        "targetId" to if (query.ssn.length >= 6) query.ssn.subSequence(0, 6) else query.ssn,
    )

enum class QueryStatus(val value: String) {
    CREATING_REQUEST("creating request"),
    RESPONSE_RECEIVED("response received"),
    RESPONSE_PARSING_FAILURE("response parsing failure"),
    ERROR_DURING_REQUEST("error during request"),
}
