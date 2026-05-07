// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.FullApplicationTest
import evaka.core.daycare.domain.Language
import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.DocumentTemplateId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.UiLanguage
import evaka.core.shared.security.PilotFeature
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class ChildDocumentsReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var controller: ChildDocumentsReport

    private val clock = MockEvakaClock(2024, 3, 1, 15, 0)
    private val area = DevCareArea()
    private val supervisor = DevEmployee()

    private data class TestSetup(
        val unitId: DaycareId,
        val templateId: DocumentTemplateId,
        val childId: ChildId,
    )

    private fun setupUnitWithChildAndTemplate(
        unitLanguage: Language,
        templateLanguage: UiLanguage,
    ): TestSetup {
        val daycare =
            DevDaycare(
                areaId = area.id,
                language = unitLanguage,
                enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC),
            )
        val group = DevDaycareGroup(daycareId = daycare.id)
        val child = DevPerson()
        val template =
            DevDocumentTemplate(
                type = ChildDocumentType.VASU,
                name = "VASU ${templateLanguage.name}",
                language = templateLanguage,
                validity = DateRange(clock.today().minusYears(1), null),
                content = DocumentTemplateContent(sections = emptyList()),
            )
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(supervisor, unitRoles = mapOf(daycare.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(group)
            tx.insert(child, type = DevPersonType.CHILD)
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = clock.today().minusMonths(1),
                        endDate = clock.today().plusMonths(1),
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = group.id,
                    startDate = clock.today().minusMonths(1),
                    endDate = clock.today().plusMonths(1),
                )
            )
            tx.insert(template)
        }
        return TestSetup(unitId = daycare.id, templateId = template.id, childId = child.id)
    }

    @Suppress("unused")
    fun languageMatrix(): Stream<Arguments> =
        listOf(
                Arguments.of(Language.fi, UiLanguage.FI, 1),
                Arguments.of(Language.fi, UiLanguage.SV, 0),
                Arguments.of(Language.fi, UiLanguage.EN, 0),
                Arguments.of(Language.sv, UiLanguage.FI, 0),
                Arguments.of(Language.sv, UiLanguage.SV, 1),
                Arguments.of(Language.sv, UiLanguage.EN, 0),
                Arguments.of(Language.en, UiLanguage.FI, 1),
                Arguments.of(Language.en, UiLanguage.SV, 0),
                Arguments.of(Language.en, UiLanguage.EN, 1),
            )
            .stream()

    @ParameterizedTest(name = "{0} unit, {1} template -> {2} documents counted")
    @MethodSource("languageMatrix")
    fun `report counts VASU documents according to language matrix`(
        unitLanguage: Language,
        templateLanguage: UiLanguage,
        expectedCount: Int,
    ) {
        val setup = setupUnitWithChildAndTemplate(unitLanguage, templateLanguage)
        db.transaction { tx ->
            tx.insert(
                DevChildDocument(
                    status = DocumentStatus.COMPLETED,
                    childId = setup.childId,
                    templateId = setup.templateId,
                    content = DocumentContent(answers = emptyList()),
                    modifiedAt = clock.now(),
                    modifiedBy = supervisor.evakaUserId,
                    contentLockedAt = clock.now(),
                    contentLockedBy = supervisor.id,
                )
            )
        }
        val rows =
            controller.getChildDocumentsReport(
                db = dbInstance(),
                clock = clock,
                user = supervisor.user,
                templateIds = setOf(setup.templateId),
                unitIds = setOf(setup.unitId),
            )
        val row = rows.single()
        assertEquals(expectedCount, row.total)
        assertEquals(expectedCount, row.completed)
    }
}
