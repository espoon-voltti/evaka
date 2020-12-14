// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.sfi

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import fi.espoo.evaka.msg.config.CryptoBuildHelper
import fi.espoo.evaka.msg.config.ResourceBundleConfig
import fi.espoo.evaka.msg.config.SfiErrorResponseHandlerConfig
import fi.espoo.evaka.msg.config.SfiSoapClientConfig
import fi.espoo.evaka.msg.config.SoapCryptoConfig
import fi.espoo.evaka.msg.config.TrustManagerConfig
import fi.espoo.evaka.msg.mapper.SfiMapper
import fi.espoo.evaka.msg.properties.SfiMessageProperties
import fi.espoo.evaka.msg.properties.SfiSoapProperties
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.EmailNotification
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.MessageDetails
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.MessageMetadata
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.PdfDetails
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.Recipient
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.RecipientAddress
import fi.espoo.evaka.msg.sficlient.soap.KyselyWS2A
import fi.espoo.evaka.msg.sficlient.soap.Viranomainen
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.util.ResourceUtils
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(
    classes = [
        SfiClientService::class, SfiSoapClientConfig::class, TrustManagerConfig::class,
        SoapCryptoConfig::class, SfiMapper::class, CryptoBuildHelper::class, SfiErrorResponseHandlerConfig::class,
        ResourceBundleConfig::class
    ]
)
@EnableConfigurationProperties(SfiSoapProperties::class, SfiMessageProperties::class)
@ActiveProfiles("sfi-dev, test")
@TestPropertySource(properties = ["fi.espoo.evaka.msg.sfi.ws.address=https://localhost:15267/Asiointitili/ViranomaispalvelutWSInterface"])
@Disabled
class SfiApiTest {

    @MockBean
    lateinit var authService: SfiAccountDetailsService

    @Autowired
    lateinit var service: SfiClientService

    // FIXME: - This test is for directly testing the QA of SFI messages and needs an SSH pipe to an ec2 instance
    //        -  Remove this class or convert it to an actual unit test when prudent

    // Note! Test will fail unless localhost:15267 is piped to SFI MSG QA
    // This test has not been tested after migrating to spring boot 2.4.x and junit 5
    @Test //
    fun sendMessage() {

        whenever(authService.getAuthorityDetails(any())).thenReturn(createViranomainen())
        whenever(authService.createQueryWithPrintingDetails()).thenReturn(createKysely())

        val metadata = createMetadata(pdfDetails = createPdfDetails(), recipient = createRecipient())

        service.sendMessage(metadata)
    }

    private fun createPdfDetails(pdfLocation: String = "kerhopaatos.pdf") = PdfDetails(
        fileDescription = "Kerhopäätös",
        fileName = "Päätös-${(100..500).random()}.pdf",
        content = ResourceUtils.getFile("classpath:$pdfLocation").readBytes()

    )

    private fun createMetadata(pdfDetails: PdfDetails, recipient: Recipient): MessageMetadata {
        return MessageMetadata(
            message = MessageDetails(
                messageId = null,
                uniqueCaseIdentifier = UUID.randomUUID().toString(),
                content = "",
                header = "",
                sendDate = LocalDate.now(),
                senderName = "Evaka",
                pdfDetails = pdfDetails,
                recipient = recipient,
                emailNotification = EmailNotification(header = "Sinulle on uusi viesti", message = "Katso viesti suomi.fi palvelusta")
            )
        )
    }

    private fun createRecipient(ssn: String = "020190-9521") = Recipient(
        ssn = ssn,
        name = "Popov Anna Alexandra",
        email = null,
        phone = null,
        address = RecipientAddress(
            streetAddress = "Kurteninkatu 13 H 45",
            postalCode = "65100",
            postOffice = "VAASA"
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
        laskutus = createBillingDetails()
    }

    private fun createBillingDetails() = KyselyWS2A.Laskutus().apply {
        tunniste = "123123"
        salasana = "1234" // a value for salasana is needed if the "tunniste" is not empty
    }
}
