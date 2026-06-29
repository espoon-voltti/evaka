// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision

import evaka.core.decision.reasoning.DecisionIndividualReasoning
import evaka.core.decision.reasoning.DecisionPdfReasoningSource
import evaka.core.decision.reasoning.DecisionReasoningCollectionType
import evaka.core.decision.reasoning.GenericReasoningText
import evaka.core.shared.DecisionIndividualReasoningId
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.OfficialLanguage
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class PdfReasoningTest {
    private val ts = HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(0, 0))

    private fun individual() =
        DecisionIndividualReasoning(
            id = DecisionIndividualReasoningId(UUID.randomUUID()),
            collectionType = DecisionReasoningCollectionType.PRESCHOOL,
            titleFi = "otsikko",
            titleSv = "rubrik",
            textFi = "teksti",
            textSv = "text",
            removedAt = null,
            createdAt = ts,
            modifiedAt = ts,
        )

    private fun source(
        generic: GenericReasoningText? = GenericReasoningText("yleinen fi", "yleinen sv"),
        individual: List<DecisionIndividualReasoning> = listOf(individual()),
    ) = DecisionPdfReasoningSource(generic = generic, individual = individual)

    @Test
    fun `picks Finnish text in Finnish`() {
        val result = buildPdfReasoning(OfficialLanguage.FI, source())

        assertEquals("yleinen fi", result.generic)
        assertEquals(listOf("teksti"), result.individual)
    }

    @Test
    fun `picks Swedish text in Swedish`() {
        val result = buildPdfReasoning(OfficialLanguage.SV, source())

        assertEquals("yleinen sv", result.generic)
        assertEquals(listOf("text"), result.individual)
    }

    @Test
    fun `allows a decision with only generic reasoning and no individual selections`() {
        val result = buildPdfReasoning(OfficialLanguage.FI, source(individual = emptyList()))

        assertEquals("yleinen fi", result.generic)
        assertEquals(emptyList(), result.individual)
    }

    @Test
    fun `throws when there is no generic reasoning`() {
        assertFailsWith<IllegalStateException> {
            buildPdfReasoning(OfficialLanguage.FI, source(generic = null, individual = emptyList()))
        }
    }

    @Test
    fun `throws when the generic text is blank in the chosen language`() {
        assertFailsWith<IllegalStateException> {
            buildPdfReasoning(
                OfficialLanguage.FI,
                source(generic = GenericReasoningText(textFi = "   ", textSv = "yleinen sv")),
            )
        }
    }

    @Test
    fun `throws when a selected individual reasoning has blank text in the chosen language`() {
        assertFailsWith<IllegalStateException> {
            buildPdfReasoning(
                OfficialLanguage.FI,
                source(individual = listOf(individual().copy(textFi = "   "))),
            )
        }
    }
}
