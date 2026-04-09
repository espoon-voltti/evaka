// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.message.config

import evaka.core.decision.DecisionSendAddress
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.message.IMessageProvider
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

internal const val PREFIX: String = "evaka.instance.tampere.MessageProvider"

@Configuration
class MessageConfiguration {

    @Bean
    fun messageProvider(): IMessageProvider {
        val messageSource = YamlMessageSource(ClassPathResource("messages-tampere.yaml"))
        return TampereMessageProvider(messageSource)
    }
}

internal class TampereMessageProvider(val messageSource: MessageSource) : IMessageProvider {

    override fun getDecisionHeader(lang: OfficialLanguage): String =
        messageSource.getMessage("$PREFIX.DECISION_HEADER", null, resolveLocale(lang))

    override fun getDecisionContent(
        lang: OfficialLanguage,
        skipGuardianApproval: Boolean?,
    ): String = messageSource.getMessage("$PREFIX.DECISION_CONTENT", null, resolveLocale(lang))

    override fun getChildDocumentDecisionHeader(lang: OfficialLanguage): String =
        getDecisionHeader(lang)

    override fun getChildDocumentDecisionContent(lang: OfficialLanguage): String =
        """
        Lapsellenne on tehty päätös. Voit katsella päätöstä eVakassa.

        Koska olette ottanut Suomi.fi -palvelun käyttöönne, on päätös myös luettavissa alla olevista liitteistä.
        """
            .trimIndent()

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
            OfficialLanguage.FI ->
                DecisionSendAddress(
                    street = "PL 487",
                    postalCode = "33101",
                    postOffice = "Tampere",
                    row1 = "Varhaiskasvatus ja esiopetus",
                    row2 = "Asiakaspalvelu",
                    row3 = "PL 487, 33101 Tampere",
                )

            OfficialLanguage.SV ->
                DecisionSendAddress(
                    street = "PL 487",
                    postalCode = "33101",
                    postOffice = "Tampere",
                    row1 = "Varhaiskasvatus ja esiopetus",
                    row2 = "Asiakaspalvelu",
                    row3 = "PL 487, 33101 Tampere",
                )
        }

    override fun getDefaultFinancialDecisionAddress(lang: OfficialLanguage): DecisionSendAddress =
        getDefaultDecisionAddress(lang)

    override fun getPlacementToolHeader(lang: OfficialLanguage): String =
        throw UnsupportedOperationException()

    override fun getPlacementToolContent(lang: OfficialLanguage): String =
        throw UnsupportedOperationException()

    private fun resolveLocale(lang: OfficialLanguage): Locale {
        if (OfficialLanguage.SV == lang) return resolveLocale(OfficialLanguage.FI)
        return Locale.of(lang.name.lowercase())
    }
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
