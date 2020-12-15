// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.sfi

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fi.espoo.evaka.msg.mapper.SfiMapper
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.EmailNotification
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.MessageDetails
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.MessageMetadata
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.PdfDetails
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.Recipient
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.RecipientAddress
import fi.espoo.evaka.msg.service.sfi.SfiErrorResponseHandler.SFiMessageDeliveryException
import fi.espoo.evaka.msg.sficlient.soap.KyselyWS2A
import fi.espoo.evaka.msg.sficlient.soap.LahetaViesti
import fi.espoo.evaka.msg.sficlient.soap.LahetaViestiResponse
import fi.espoo.evaka.msg.sficlient.soap.ObjectFactory
import fi.espoo.evaka.msg.sficlient.soap.TilaKoodiWS
import fi.espoo.evaka.msg.sficlient.soap.VastausWS2A
import fi.espoo.evaka.msg.sficlient.soap.Viranomainen
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.util.ResourceUtils
import org.springframework.ws.client.core.WebServiceTemplate
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SfiClientServiceTest {

    @Mock
    lateinit var accountDetailsService: SfiAccountDetailsService

    @Mock
    lateinit var sfiObjectFactory: ObjectFactory

    @Mock
    lateinit var wsTemplate: WebServiceTemplate

    @Mock
    lateinit var mapper: SfiMapper

    @Mock
    lateinit var errorResponseHandler: SfiErrorResponseHandler

    @InjectMocks
    lateinit var service: SfiClientService

    @Test
    fun `service uses billing info from configured account details in message`() {

        whenever(accountDetailsService.getAuthorityDetails(any())).thenReturn(createViranomainen())

        val query = createKysely()
        whenever(accountDetailsService.createQueryWithPrintingDetails()).thenReturn(query)

        val expectedBilling = query.laskutus
        whenever(sfiObjectFactory.createLahetaViesti()).thenReturn(LahetaViesti())

        val metadata = createMetadata(pdfDetails = createPdfDetails(), recipient = createRecipient())

        whenever(wsTemplate.marshalSendAndReceive(any())).thenReturn(createResponse())

        service.sendMessage(metadata)

        argumentCaptor<LahetaViesti>().apply {
            verify(wsTemplate).marshalSendAndReceive(capture())
            assertThat(firstValue.kysely.laskutus).isEqualTo(expectedBilling)
        }
    }

    @Test
    fun `sending message successfully should return ok`() {

        whenever(accountDetailsService.getAuthorityDetails(any())).thenReturn(createViranomainen())
        whenever(accountDetailsService.createQueryWithPrintingDetails()).thenReturn(createKysely())
        whenever(sfiObjectFactory.createLahetaViesti()).thenReturn(LahetaViesti())

        val metadata = createMetadata(pdfDetails = createPdfDetails(), recipient = createRecipient())

        whenever(wsTemplate.marshalSendAndReceive(any())).thenReturn(createResponse())

        val response = service.sendMessage(metadata)
        assertThat(response.code).isEqualTo(MSG_SENDING_SUCCESSFUL)
    }

    @Test
    fun `when request fails, an exception should be thrown`() {

        whenever(accountDetailsService.getAuthorityDetails(any())).thenReturn(createViranomainen())
        whenever(accountDetailsService.createQueryWithPrintingDetails()).thenReturn(createKysely())
        whenever(sfiObjectFactory.createLahetaViesti()).thenReturn(LahetaViesti())

        val metadata = createMetadata(pdfDetails = createPdfDetails(), recipient = createRecipient())

        val message = "this is an error message 25"
        whenever(wsTemplate.marshalSendAndReceive(any())).thenThrow(RuntimeException(message))

        try {
            service.sendMessage(metadata)
            fail<Nothing>("Expecting an exception")
        } catch (e: Exception) {
            assertThat(e).hasCause(RuntimeException(message))
        }
    }

    @Test
    fun `when request returns an error, an exception should be thrown`() {

        whenever(accountDetailsService.getAuthorityDetails(any())).thenReturn(createViranomainen())
        whenever(accountDetailsService.createQueryWithPrintingDetails()).thenReturn(createKysely())
        whenever(sfiObjectFactory.createLahetaViesti()).thenReturn(LahetaViesti())

        val metadata = createMetadata(pdfDetails = createPdfDetails(), recipient = createRecipient())

        val expectedCode = 404
        val expectedMessage = "Palvelutunnus ei vastaa viranomaistunnusta"
        val expectedResponse = createResponse(statusCode = expectedCode, statusText = expectedMessage)
        val expectedError = "erro 12333 11"
        whenever(wsTemplate.marshalSendAndReceive(any())).thenReturn(expectedResponse)
        whenever(errorResponseHandler.handleError(SfiResponse(code = expectedCode, text = expectedMessage)))
            .thenThrow(SFiMessageDeliveryException(expectedError))

        try {
            service.sendMessage(metadata)
            fail<Nothing>("Expecting an exception")
        } catch (e: Exception) {
            assertThat(e).isExactlyInstanceOf(SFiMessageDeliveryException::class.java)
            assertThat(e).hasMessage(expectedError)
        }
    }

    private fun createPdfDetails(pdfLocation: String = "kerhopaatos.pdf") = PdfDetails(
        fileDescription = "Kerhopäätös",
        fileName = "Päätös-${(100..500).random()}.pdf",
        content = ResourceUtils.getFile("classpath:$pdfLocation").readBytes()

    )

    private fun createMetadata(pdfDetails: PdfDetails, recipient: Recipient) = MessageMetadata(
        message = MessageDetails(
            messageId = null,
            uniqueCaseIdentifier = UUID.randomUUID().toString(),
            content = "Evaka testiviesti",
            header = "Päätös kerhopaikasta",
            sendDate = LocalDate.now(),
            senderName = "Evaka",
            pdfDetails = pdfDetails,
            recipient = recipient,
            emailNotification = EmailNotification(header = "Sinulle on uusi viesti", message = "Katso viesti suomi.fi palvelusta")
        )
    )

    private fun createRecipient(ssn: String = "110106A998M") = Recipient(
        ssn = ssn,
        name = "Popov Anna Alexandra",
        email = null,
        phone = null,
        address = RecipientAddress(
            streetAddress = "Demo Nordealankuja 5",
            postalCode = "123456",
            postOffice = "Espoo"
        )
    )

    private fun createViranomainen(): Viranomainen = Viranomainen()
        .apply {
            viranomaisTunnus = "espoo_ws_vaka"
            palveluTunnus = "espoo_ws_vaka"
            sanomaTunniste = UUID.randomUUID().toString()
            sanomaVersio = "1.0"
            sanomaVarmenneNimi = "Evaka-staging-Suomi.fi_Viestit"
        }

    private fun createKysely() = KyselyWS2A().apply {
        isLahetaTulostukseen = false
        tulostustoimittaja = "Edita"
        isPaperi = false
        laskutus = KyselyWS2A.Laskutus().apply {
            tunniste = "123456"
            salasana = "1234"
        }
    }

    private fun createResponse(
        statusCode: Int = 202,
        statusText: String = "Kutsu on onnistunut ja viestit on tallennettu " +
            "Viestit-palvelun käsittelyjonoon, mutta viesti tai viestit ei vielä näy asiakkaalle tai asiakkaille."
    ) = LahetaViestiResponse().apply {
        lahetaViestiResult = VastausWS2A().apply {
            tilaKoodi = TilaKoodiWS().apply {
                tilaKoodi = statusCode
                tilaKoodiKuvaus = statusText
            }
        }
    }
}
