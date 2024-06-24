// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.vtjclient

import fi.espoo.evaka.VtjEnv
import fi.espoo.evaka.vtjclient.config.SoapRequestAdapter
import fi.espoo.evaka.vtjclient.mapper.VTJResponseMapper
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.VTJQuery
import fi.espoo.evaka.vtjclient.service.vtjclient.QueryStatus.CREATING_REQUEST
import fi.espoo.evaka.vtjclient.service.vtjclient.QueryStatus.ERROR_DURING_REQUEST
import fi.espoo.evaka.vtjclient.service.vtjclient.QueryStatus.RESPONSE_PARSING_FAILURE
import fi.espoo.evaka.vtjclient.service.vtjclient.QueryStatus.RESPONSE_RECEIVED
import fi.espoo.evaka.vtjclient.soap.HenkiloTunnusKyselyReqBody
import fi.espoo.evaka.vtjclient.soap.HenkiloTunnusKyselyReqBodyTiedot
import fi.espoo.evaka.vtjclient.soap.HenkiloTunnusKyselyResBody
import fi.espoo.evaka.vtjclient.soap.ObjectFactory
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo
import fi.espoo.voltti.logging.loggers.auditVTJ
import jakarta.xml.bind.JAXBElement
import mu.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.ws.client.core.WebServiceMessageCallback
import org.springframework.ws.client.core.WebServiceTemplate

@Service
@Profile("production", "vtj-dev", "integration-test")
class VtjClientService(
    private val vtjObjectFactory: ObjectFactory,
    private val wsTemplate: WebServiceTemplate,
    private val vtjEnv: VtjEnv,
    private val requestAdapter: SoapRequestAdapter,
    private val responseMapper: VTJResponseMapper
) : IVtjClientService {
    private val logger = KotlinLogging.logger {}

    override fun query(query: VTJQuery): Henkilo? {
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
        callback: WebServiceMessageCallback
    ): JAXBElement<T> =
        marshalSendAndReceive(request, callback)
            .let {
                it as? JAXBElement<*>
                    ?: throw IllegalStateException("Unexpected VTJ response : $it")
            }.let {
                if (it.declaredType == T::class.java) {
                    @Suppress("UNCHECKED_CAST")
                    it as JAXBElement<T>
                } else {
                    throw IllegalStateException("Unexpected VTJ response type: ${it.declaredType}")
                }
            }

    fun VTJQuery.toRequest(): JAXBElement<HenkiloTunnusKyselyReqBody> {
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

fun toLogParamsMap(
    query: VTJQuery,
    status: QueryStatus
) = mapOf(
    "meta" to mapOf("queryName" to query.type.queryName),
    "status" to status.value,
    "targetId" to if (query.ssn.length >= 6) query.ssn.subSequence(0, 6) else query.ssn
)

enum class QueryStatus(
    val value: String
) {
    CREATING_REQUEST("creating request"),
    RESPONSE_RECEIVED("response received"),
    RESPONSE_PARSING_FAILURE("response parsing failure"),
    ERROR_DURING_REQUEST("error during request")
}
