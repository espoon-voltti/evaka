// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.message.config

import fi.espoo.evaka.decision.DecisionSendAddress
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.shared.message.IMessageProvider
import java.text.MessageFormat
import java.util.Locale
import java.util.Properties
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.AbstractMessageSource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource

internal const val PREFIX: String = "fi.espoo.evaka.turku.MessageProvider"

@Configuration
class MessageConfiguration {
    @Bean
    fun messageProvider(): IMessageProvider {
        val messageSource = YamlMessageSource(ClassPathResource("turku/messages.yaml"))
        return TurkuMessageProvider(messageSource)
    }
}

internal class TurkuMessageProvider(val messageSource: MessageSource) : IMessageProvider {
    override fun getDecisionHeader(lang: OfficialLanguage): String =
        messageSource.getMessage("$PREFIX.DECISION_HEADER", null, resolveLocale(lang))

    override fun getDecisionContent(
        lang: OfficialLanguage,
        skipGuardianApproval: Boolean?,
    ): String = messageSource.getMessage("$PREFIX.DECISION_CONTENT", null, resolveLocale(lang))

    override fun getFeeDecisionHeader(lang: OfficialLanguage): String =
        messageSource.getMessage("$PREFIX.FEE_DECISION_HEADER", null, resolveLocale(lang))

    override fun getFeeDecisionContent(lang: OfficialLanguage): String =
        messageSource.getMessage("$PREFIX.FEE_DECISION_CONTENT", null, resolveLocale(lang))

    override fun getVoucherValueDecisionHeader(lang: OfficialLanguage): String =
        messageSource.getMessage("$PREFIX.VOUCHER_VALUE_DECISION_HEADER", null, resolveLocale(lang))

    override fun getVoucherValueDecisionContent(lang: OfficialLanguage): String =
        messageSource.getMessage(
            "$PREFIX.VOUCHER_VALUE_DECISION_CONTENT",
            null,
            resolveLocale(lang),
        )

    override fun getDefaultDecisionAddress(lang: OfficialLanguage): DecisionSendAddress =
        when (lang) {
            OfficialLanguage.FI -> {
                DecisionSendAddress(
                    street = "PL 355",
                    postalCode = "20101",
                    postOffice = "Turku",
                    row1 = "Kasvatuksen ja opetuksen palvelukokonaisuus",
                    row2 = "Varhaiskasvatuksen ja esiopetuksen asiakaspalvelu",
                    row3 = "PL 355, 20101 Turku",
                )
            }

            OfficialLanguage.SV -> {
                DecisionSendAddress(
                    street = "PB 355",
                    postalCode = "20101",
                    postOffice = "Åbo stad",
                    row1 = "Servicehelheten för fostran och undervisning",
                    row2 = "Klientavgifter för småbarnspedagogik",
                    row3 = "PB 355, 20101 Åbo stad",
                )
            }
        }

    override fun getDefaultFinancialDecisionAddress(lang: OfficialLanguage): DecisionSendAddress =
        when (lang) {
            OfficialLanguage.FI -> {
                DecisionSendAddress(
                    street = "PL 355",
                    postalCode = "20101",
                    postOffice = "Turku",
                    row1 = "Kasvatuksen ja opetuksen palvelukokonaisuus",
                    row2 = "Varhaiskasvatuksen ja esiopetuksen asiakaspalvelu",
                    row3 = "PL 355, 20101 Turku",
                )
            }

            OfficialLanguage.SV -> {
                DecisionSendAddress(
                    street = "PB 355",
                    postalCode = "20101",
                    postOffice = "Åbo stad",
                    row1 = "Servicehelheten för fostran och undervisning",
                    row2 = "Klientavgifter för småbarnspedagogik",
                    row3 = "PB 355, 20101 Åbo stad",
                )
            }
        }

    override fun getPlacementToolHeader(lang: OfficialLanguage): String =
        when (lang) {
            OfficialLanguage.FI -> {
                "Esitäytetty hakemus esiopetukseen / Pre-filled application for preschool education"
            }

            OfficialLanguage.SV -> {
                "Förfyllad ansökan om förskoleundervisning"
            }
        }

    override fun getPlacementToolContent(lang: OfficialLanguage): String =
        when (lang) {
            OfficialLanguage.FI -> {
                """
                Olemme tehneet lapsellenne esitäytetyn hakemuksen esiopetukseen. Hakemus on tehty lapsen oppilaaksiottoalueen mukaiseen esiopetusyksikköön.

                Mikäli haluatte hakeutua muuhun kuin lapsellenne osoitettuun paikkaan, voitte muokata hakemusta eVakassa.

                Jos taas hyväksytte osoitetun esiopetuspaikan, teidän ei tarvitse tehdä mitään.

                Terveisin

                Turun kaupungin Varhaiskasvatus

                In English:

                We have made a pre-filled application for preschool education for your child. The application has been submitted to the pre-school unit according to the child's pupil enrollment area.

                If you want to apply for a place other than the one assigned to your child, you can edit the application in eVaka.

                If you accept the assigned pre-school place, you don't have to do anything.

                Best regards

                Early childhood education in the city of Turku
                """
                    .trimIndent()
            }

            OfficialLanguage.SV -> {
                """
                Vi har gjort en förifylld ansökan om förskoleundervisning för ditt barn.
                """
                    .trimIndent()
            }
        }

    override fun getChildDocumentDecisionHeader(lang: OfficialLanguage): String =
        when (lang) {
            OfficialLanguage.FI -> "Turun varhaiskasvatukseen liittyvät päätökset"
            OfficialLanguage.SV -> "Beslut gällande Åbo småbarnspedagogik"
        }

    override fun getChildDocumentDecisionContent(lang: OfficialLanguage): String =
        when (lang) {
            OfficialLanguage.FI -> {
                """
                Lapsellenne on tehty päätös. Voit katsella päätöstä eVakassa.

                Koska olette ottanut Suomi.fi -palvelun käyttöönne, on päätös myös luettavissa alla olevista liitteistä.

                In English:

                A decision has been made for your child. You can view the decision on eVaka.

                As you are a user of Suomi.fi, you can also find the decision in the attachments below.
                """
                    .trimIndent()
            }

            OfficialLanguage.SV -> {
                """
                Beslut har fattats för ditt barn. Du kan se beslutet i eVaka.

                Eftersom du har tagit Suomi.fi-tjänsten i bruk, kan du också läsa beslutet i bilagorna nedan.
                """
                    .trimIndent()
            }
        }

    private fun resolveLocale(lang: OfficialLanguage): Locale = Locale.of(lang.name.lowercase())
}

internal class YamlMessageSource(resource: Resource) : AbstractMessageSource() {
    private val properties: Properties =
        YamlPropertiesFactoryBean()
            .apply {
                setResources(resource)
                afterPropertiesSet()
            }
            .`object`!!

    override fun resolveCode(code: String, locale: Locale): MessageFormat? =
        properties.getProperty("$code.${locale.language.lowercase()}")?.let {
            MessageFormat(it, locale)
        }
}
