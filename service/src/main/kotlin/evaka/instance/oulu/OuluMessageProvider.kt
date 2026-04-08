// SPDX-FileCopyrightText: 2021 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu

import evaka.core.decision.DecisionSendAddress
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.message.IMessageProvider
import java.text.MessageFormat
import java.util.Locale
import java.util.Properties
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.context.MessageSource
import org.springframework.context.support.AbstractMessageSource
import org.springframework.core.io.Resource

internal const val PREFIX: String = "evaka.instance.oulu.MessageProvider"

class OuluMessageProvider(val messageSource: MessageSource) : IMessageProvider {
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
            OfficialLanguage.FI,
            OfficialLanguage.SV -> {
                DecisionSendAddress(
                    street = "PL 75",
                    postalCode = "90015",
                    postOffice = "Oulu",
                    row1 = "Varhaiskasvatuksen palveluohjaus",
                    row2 = "Asiakaspalvelu",
                    row3 = "PL 75, 90015 Oulu",
                )
            }
        }

    override fun getDefaultFinancialDecisionAddress(lang: OfficialLanguage): DecisionSendAddress =
        getDefaultDecisionAddress(lang)

    override fun getPlacementToolHeader(lang: OfficialLanguage): String =
        "Esitäytetty hakemus esiopetukseen / Pre-filled application for preschool education"

    override fun getPlacementToolContent(lang: OfficialLanguage): String =
        """
Olemme tehneet lapsellenne esitäytetyn hakemuksen esiopetukseen. Hakemus on tehty lapsen oppilaaksiottoalueen mukaiseen esiopetusyksikköön.

Mikäli haluatte hakeutua muuhun kuin lapsellenne osoitettuun paikkaan, voitte muokata hakemusta eVakassa.

Jos taas hyväksytte osoitetun esiopetuspaikan, teidän ei tarvitse tehdä mitään.

In English:

We have made a pre-filled application for preschool education for your child. The application has been submitted to the pre-school unit according to the child's pupil enrollment area.

If you want to apply for a place other than the one assigned to your child, you can edit the application in eVaka.

If you accept the assigned pre-school place, you don't have to do anything.
                """

    override fun getChildDocumentDecisionHeader(lang: OfficialLanguage): String =
        when (lang) {
            OfficialLanguage.FI,
            OfficialLanguage.SV -> "Oulun varhaiskasvatukseen liittyvät päätökset"
        }

    override fun getChildDocumentDecisionContent(lang: OfficialLanguage): String =
        when (lang) {
            OfficialLanguage.FI,
            OfficialLanguage.SV -> {
                """
                Lapsellenne on tehty päätös. Voit katsella päätöstä eVakassa.

                Koska olette ottanut Suomi.fi -palvelun käyttöönne, on päätös myös luettavissa alla olevista liitteistä.

                In English:

                A decision has been made for your child. You can view the decision on eVaka.

                As you are a user of Suomi.fi, you can also find the decision in the attachments below.
                """
                    .trimIndent()
            }
        }

    private fun resolveLocale(lang: OfficialLanguage): Locale {
        if (OfficialLanguage.SV == lang) return resolveLocale(OfficialLanguage.FI)
        return Locale.of(lang.name.lowercase())
    }
}

class YamlMessageSource(resource: Resource) : AbstractMessageSource() {
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
