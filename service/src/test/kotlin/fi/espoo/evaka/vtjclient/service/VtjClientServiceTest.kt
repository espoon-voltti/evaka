// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import com.fasterxml.jackson.core.JsonGenerator
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.VtjEnv
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.identity.ExternalIdentifier.SSN
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.vtjclient.config.SoapRequestAdapter
import fi.espoo.evaka.vtjclient.mapper.VTJResponseMapper
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.HUOLTAJA_HUOLLETTAVA
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.RequestType.PERUSSANOMA3
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService.VTJQuery
import fi.espoo.evaka.vtjclient.service.vtjclient.VtjClientService
import fi.espoo.evaka.vtjclient.soap.HenkiloTunnusKyselyReqBody
import fi.espoo.evaka.vtjclient.soap.HenkiloTunnusKyselyResBody
import fi.espoo.evaka.vtjclient.soap.ObjectFactory
import fi.espoo.evaka.vtjclient.soap.VTJHenkiloVastaussanoma.Henkilo
import jakarta.xml.bind.JAXBElement
import java.time.LocalDate
import java.util.UUID
import net.logstash.logback.marker.MapEntriesAppendingMarker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.ws.client.core.WebServiceMessageCallback
import org.springframework.ws.client.core.WebServiceTemplate

const val STATUS_CREATE_QUERY = "creating request"
const val STATUS_RESPONSE_RECEIVED = "response received"
const val STATUS_NO_RESPONSE_OR_PARSING_ERROR = "response parsing failure"
const val STATUS_ERROR_DURING_REQUEST = "error during request"

const val DEFAULT_PERSON_SSN = "270372-905L"

@ExtendWith(MockitoExtension::class)
class VtjClientServiceTest {

    @Spy var vtjObjectFactory: ObjectFactory = ObjectFactory()

    @Mock lateinit var mockWSTemplate: WebServiceTemplate

    @Mock lateinit var mockVtjProps: VtjEnv

    @Mock lateinit var mockRequestAdapter: SoapRequestAdapter

    @Mock lateinit var mockCallback: WebServiceMessageCallback

    @Mock lateinit var responseMapper: VTJResponseMapper

    @InjectMocks lateinit var service: VtjClientService

    lateinit var responseBody: HenkiloTunnusKyselyResBody

    lateinit var response: JAXBElement<HenkiloTunnusKyselyResBody>

    /* TODO:  This is a lot of mocks for a single service which might indicate the target service has too many responsibilities
    is there a split that could be done here? */

    @BeforeEach
    fun setup() {
        responseBody = vtjObjectFactory.createHenkiloTunnusKyselyResBody()
        response = vtjObjectFactory.createHenkilonTunnusKyselyResponse(responseBody)
    }

    @Test
    fun `logger should be called with endUser details when querying for data from VTJ`() {
        val mockAppender = mockAppender()

        val ssn = "050482-9741"
        val requestedBy = createPerson(externalId = SSN.getInstance(ssn))
        val query =
            VTJQuery(requestingUserId = requestedBy.id.raw, type = HUOLTAJA_HUOLLETTAVA, ssn = ssn)

        whenever(mockRequestAdapter.createCallback(query)).thenReturn(mockCallback)
        // see: NB1
        whenever(mockWSTemplate.marshalSendAndReceive(any<Any>(), eq(mockCallback)))
            .thenReturn(response)
        whenever(responseMapper.mapResponseToHenkilo(response))
            .thenReturn(createPersonResponse(forSsn = ssn))

        service.query(query)

        argumentCaptor<ILoggingEvent>().apply {
            verify(mockAppender, times(2)).doAppend(capture())
            val queryLogEvent = firstValue

            assertThat(queryLogEvent.message).isEqualTo("VTJ Query of type: Huoltaja-Huollettavat")

            queryLogEvent.assertLogArgumentsContain(query = query, status = STATUS_CREATE_QUERY)

            val responseLogEvent = secondValue

            assertThat(responseLogEvent.message).isEqualTo("VTJ results received")

            responseLogEvent.assertLogArgumentsContain(
                query = query,
                status = STATUS_RESPONSE_RECEIVED,
            )
        }
    }

    @Test
    fun `logger should be called with failure details when VTJ returns empty results`() {
        val mockAppender = mockAppender()
        val query = createDefaultQuery()

        whenever(mockRequestAdapter.createCallback(query)).thenReturn(mockCallback)
        // see: NB1
        whenever(mockWSTemplate.marshalSendAndReceive(any<Any>(), eq(mockCallback)))
            .thenReturn(response)
        whenever(responseMapper.mapResponseToHenkilo(response)).thenReturn(null)

        service.query(query)

        argumentCaptor<ILoggingEvent>().apply {
            verify(mockAppender, times(2)).doAppend(capture())

            val queryLogEvent = firstValue

            queryLogEvent.assertCreateQueryLogMessage(query)
            queryLogEvent.assertLogArgumentsContain(query = query, status = STATUS_CREATE_QUERY)

            val responseLogEvent = secondValue
            assertThat(responseLogEvent.message).isEqualTo("Did not receive VTJ results")
            responseLogEvent.assertLogArgumentsContain(
                query = query,
                status = STATUS_NO_RESPONSE_OR_PARSING_ERROR,
            )
        }
    }

    @Test
    fun `logger should be called with failure details when VTJ query fails`() {
        val mockAppender = mockAppender()
        val query = createDefaultQuery()

        val expectedException = ClassCastException("some message")

        whenever(mockRequestAdapter.createCallback(query)).thenReturn(mockCallback)
        // see: NB1
        whenever(mockWSTemplate.marshalSendAndReceive(any<Any>(), eq(mockCallback)))
            .thenThrow(expectedException)

        try {
            service.query(query)
            fail<Exception>("Exception not thrown")
        } catch (ex: ClassCastException) {
            assertThat(ex.message).isEqualTo(expectedException.message)

            argumentCaptor<ILoggingEvent>().apply {
                verify(mockAppender, times(2)).doAppend(capture())

                val queryLogEvent = firstValue
                queryLogEvent.assertCreateQueryLogMessage(query)
                queryLogEvent.assertLogArgumentsContain(query = query, status = STATUS_CREATE_QUERY)

                val responseLogEvent = secondValue
                assertThat(responseLogEvent.message)
                    .isEqualTo("There was an error requesting VTJ data. Results were not received.")
                responseLogEvent.assertLogArgumentsContain(
                    query = query,
                    status = STATUS_ERROR_DURING_REQUEST,
                )
            }
        }
    }

    @Test
    fun `logger should be called with office holder details when querying for data from VTJ`() {
        val mockAppender = mockAppender()

        val ssn = "130894-917N"
        val requestedBy = createPerson()
        val query = VTJQuery(requestingUserId = requestedBy.id.raw, type = PERUSSANOMA3, ssn = ssn)

        whenever(mockRequestAdapter.createCallback(query)).thenReturn(mockCallback)
        // NB1: we don't actually care about the mock callback equalling here but mockito returns
        // null
        // from marshalSendAndReceive(any<Any>(), any()) and marshalSendAndReceive(any<Any>(),
        // any<WebServiceMessageCallback())
        // maybe due to how it tries to map the method with similar signature, which causes an
        // exception as
        // the service expects at least non-null JAXBElement<*> as a response
        whenever(mockWSTemplate.marshalSendAndReceive(any<Any>(), eq(mockCallback)))
            .thenReturn(response)
        whenever(responseMapper.mapResponseToHenkilo(response))
            .thenReturn(createPersonResponse(forSsn = ssn))

        service.query(query)

        argumentCaptor<ILoggingEvent>().apply {
            verify(mockAppender, times(2)).doAppend(capture())

            val queryLogEvent = firstValue
            assertThat(queryLogEvent.message).isEqualTo("VTJ Query of type: Perussanoma 3")

            queryLogEvent.assertLogArgumentsContain(query = query, status = STATUS_CREATE_QUERY)

            val responseLogEvent = secondValue
            assertThat(responseLogEvent.message).isEqualTo("VTJ results received")
            responseLogEvent.assertLogArgumentsContain(
                query = query,
                status = STATUS_RESPONSE_RECEIVED,
            )
        }
    }

    @Test
    fun `the service should use the request adapter to add required SOAP headers`() {
        val requestedBy = createPerson()
        val query =
            VTJQuery(
                requestingUserId = requestedBy.id.raw,
                type = HUOLTAJA_HUOLLETTAVA,
                ssn = requestedBy.identity.toString(),
            )

        whenever(mockRequestAdapter.createCallback(query)).thenReturn(mockCallback)
        whenever(mockWSTemplate.marshalSendAndReceive(any<Any>(), eq(mockCallback)))
            .thenReturn(response)
        whenever(responseMapper.mapResponseToHenkilo(response)).thenReturn(createPersonResponse())

        service.query(query)

        verify(mockRequestAdapter).createCallback(query)
    }

    @Test
    fun `the service should create a request using correct details`() {
        val requestedBy = createPerson()
        val query =
            VTJQuery(
                requestingUserId = requestedBy.id.raw,
                type = HUOLTAJA_HUOLLETTAVA,
                ssn = requestedBy.identity.toString(),
            )

        whenever(mockRequestAdapter.createCallback(query)).thenReturn(mockCallback)
        whenever(mockWSTemplate.marshalSendAndReceive(any<Any>(), eq(mockCallback)))
            .thenReturn(response)
        whenever(responseMapper.mapResponseToHenkilo(response)).thenReturn(createPersonResponse())

        val userName = "sdfhj2o123ju1l23"
        whenever(mockVtjProps.username).thenReturn(userName)

        val password = "*/89+74563876t5123v bg12"
        whenever(mockVtjProps.password).thenReturn(Sensitive(password))

        service.query(query)

        argumentCaptor<JAXBElement<HenkiloTunnusKyselyReqBody>>().apply {
            verify(mockWSTemplate).marshalSendAndReceive(capture(), eq(mockCallback))
            val request = firstValue.value.request
            assertThat(request.henkilotunnus).isEqualTo(requestedBy.identity.toString())
            assertThat(request.kayttajatunnus).isEqualTo(userName)
            assertThat(request.salasana).isEqualTo(password)
            assertThat(request.soSoNimi).isEqualTo(query.type.queryName)
            assertThat(request.loppukayttaja).isEqualTo("voltti-id: ${requestedBy.id}")
        }
    }

    private fun createPerson(
        id: PersonId = PersonId(UUID.randomUUID()),
        externalId: ExternalIdentifier = SSN.getInstance(DEFAULT_PERSON_SSN),
        firstName: String = "Tomi",
        lastName: String = "Testing",
        preferredName: String = "Tomi",
        email: String = "email@example.org",
        phone: String = "+89371094",
        backupPhone: String = "+89371099",
        language: String = "fi",
        streetAddress: String = "Jokukatu 8",
        postalOffice: String = "Jokukaupunki",
        postalCode: String = "00100",
        dateOfBirth: LocalDate = LocalDate.of(1972, 3, 27),
    ) =
        PersonDTO(
            id = id,
            duplicateOf = null,
            identity = externalId,
            ssnAddingDisabled = false,
            firstName = firstName,
            lastName = lastName,
            preferredName = preferredName,
            email = email,
            phone = phone,
            backupPhone = backupPhone,
            language = language,
            dateOfBirth = dateOfBirth,
            streetAddress = streetAddress,
            postOffice = postalOffice,
            postalCode = postalCode,
            residenceCode = "",
            restrictedDetailsEnabled = false,
            restrictedDetailsEndDate = null,
        )

    private fun createPersonResponse(forSsn: String? = DEFAULT_PERSON_SSN) =
        Henkilo().apply { henkilotunnus = Henkilo.Henkilotunnus().apply { value = forSsn } }

    private fun createDefaultQuery(
        ssn: String = DEFAULT_PERSON_SSN,
        type: RequestType = HUOLTAJA_HUOLLETTAVA,
    ): VTJQuery {
        val requestedBy = createPerson(externalId = SSN.getInstance(ssn))
        return VTJQuery(requestingUserId = requestedBy.id.raw, type = type, ssn = ssn)
    }

    private fun ILoggingEvent.assertLogArgumentsContain(query: VTJQuery, status: String) {
        // is there an easier way to verify that the values were passed to the logger?
        // this is dumb because it relies on internals of MapEntriesAppendingMarker

        assertThat(argumentArray).hasSize(1)

        val marker = argumentArray[0] as MapEntriesAppendingMarker

        val mockGenerator: JsonGenerator = mock()

        marker.writeTo(mockGenerator)

        argumentCaptor<String>().apply {
            verify(mockGenerator, times(3)).writeFieldName(capture())
            assertThat(allValues).contains("meta", "status", "targetId")
        }

        argumentCaptor<Any>().apply {
            verify(mockGenerator, times(3)).writeObject(capture())
            assertThat(allValues)
                .contains(
                    mapOf("queryName" to query.type.queryName),
                    status,
                    query.ssn.subSequence(0, 6),
                )
        }
    }

    private fun ILoggingEvent.assertCreateQueryLogMessage(query: VTJQuery) {
        assertThat(message).contains("VTJ Query of type: ${query.type.queryName}")
    }

    private fun mockAppender(): Appender<ILoggingEvent> {
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val mockAppender = mock<Appender<ILoggingEvent>>()
        root.addAppender(mockAppender)
        return mockAppender
    }
}
