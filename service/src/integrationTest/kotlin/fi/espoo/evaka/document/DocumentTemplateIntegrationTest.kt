// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.caseprocess.DocumentConfidentiality
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.document.childdocument.ChildDocumentController
import fi.espoo.evaka.document.childdocument.ChildDocumentCreateRequest
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.UiLanguage
import fi.espoo.evaka.shared.security.PilotFeature
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class DocumentTemplateIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var controller: DocumentTemplateController
    @Autowired lateinit var childDocumentController: ChildDocumentController

    private val now = MockEvakaClock(2022, 1, 1, 15, 0)
    private val area = DevCareArea()
    private val daycare =
        DevDaycare(
            areaId = area.id,
            language = Language.sv,
            enabledPilotFeatures =
                setOf(
                    PilotFeature.VASU_AND_PEDADOC,
                    PilotFeature.OTHER_DECISION,
                    PilotFeature.CITIZEN_BASIC_DOCUMENT,
                ),
        )
    private val employee = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val child = DevPerson()

    val testContent =
        DocumentTemplateContent(
            sections =
                listOf(
                    Section(
                        id = "s1",
                        label = "Eka",
                        questions =
                            listOf(
                                Question.TextQuestion(id = "q1", label = "kysymys 1"),
                                Question.CheckboxQuestion(id = "q2", label = "kysymys 2"),
                            ),
                    )
                )
        )

    val testCreationRequest =
        DocumentTemplateBasicsRequest.Regular(
            name = "test",
            type = ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
            placementTypes = PlacementType.entries.toSet(),
            language = UiLanguage.FI,
            confidentiality = DocumentConfidentiality(100, "Laki § 100"),
            legalBasis = "§42",
            validity = DateRange(LocalDate.of(2022, 7, 1), null),
            processDefinitionNumber = "123.456.789",
            archiveDurationMonths = 120,
            endDecisionWhenUnitChanges = null,
        )

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = now.today(),
                    endDate = now.today().plusDays(5),
                )
            )
        }
    }

    @Test
    fun `test basic crud`() {
        val created =
            controller.createTemplate(dbInstance(), employee.user, now, testCreationRequest)

        assertEquals(testCreationRequest.name, created.name)
        assertEquals(testCreationRequest.type, created.type)
        assertEquals(testCreationRequest.language, created.language)
        assertEquals(testCreationRequest.confidentiality, created.confidentiality)
        assertEquals(testCreationRequest.legalBasis, created.legalBasis)
        assertEquals(testCreationRequest.validity, created.validity)
        assertEquals(testCreationRequest.processDefinitionNumber, created.processDefinitionNumber)
        assertEquals(testCreationRequest.archiveDurationMonths, created.archiveDurationMonths)
        assertEquals(DocumentTemplateContent(sections = emptyList()), created.content)

        val summaries = controller.getTemplates(dbInstance(), employee.user, now)
        assertEquals(
            listOf(
                DocumentTemplateSummary(
                    id = created.id,
                    name = created.name,
                    type = created.type,
                    placementTypes = PlacementType.entries.toSet(),
                    language = created.language,
                    validity = created.validity,
                    documentCount = 0,
                    published = false,
                )
            ),
            summaries,
        )

        controller.updateDraftTemplateBasics(
            dbInstance(),
            employee.user,
            now,
            created.id,
            testCreationRequest.copy(
                name = "name2",
                language = UiLanguage.SV,
                type = ChildDocumentType.PEDAGOGICAL_REPORT,
                confidentiality = null,
                legalBasis = "$42b",
                processDefinitionNumber = "123.456.789b",
                archiveDurationMonths = 132,
            ),
        )

        controller.updateDraftTemplateContent(
            dbInstance(),
            employee.user,
            now,
            created.id,
            testContent,
        )
        val newValidity = DateRange(LocalDate.of(2022, 5, 1), LocalDate.of(2022, 9, 1))
        controller.updateTemplateValidity(dbInstance(), employee.user, now, created.id, newValidity)

        val fetched = controller.getTemplate(dbInstance(), employee.user, now, created.id)
        assertEquals(
            created.copy(
                name = "name2",
                language = UiLanguage.SV,
                type = ChildDocumentType.PEDAGOGICAL_REPORT,
                confidentiality = null,
                legalBasis = "$42b",
                content = testContent,
                validity = newValidity,
                processDefinitionNumber = "123.456.789b",
                archiveDurationMonths = 132,
            ),
            fetched,
        )

        controller.deleteDraftTemplate(dbInstance(), employee.user, now, created.id)

        val summaries2 = controller.getTemplates(dbInstance(), employee.user, now)
        assertEquals(emptyList(), summaries2)
        assertThrows<NotFound> {
            controller.getTemplate(dbInstance(), employee.user, now, created.id)
        }
    }

    @Test
    fun `test publishing, after which basics and content cannot be updated or template deleted`() {
        val created =
            controller.createTemplate(dbInstance(), employee.user, now, testCreationRequest)
        controller.updateDraftTemplateContent(
            dbInstance(),
            employee.user,
            now,
            created.id,
            testContent,
        )
        controller.publishTemplate(dbInstance(), employee.user, now, created.id)

        assertTrue(controller.getTemplate(dbInstance(), employee.user, now, created.id).published)

        assertThrows<BadRequest> {
            controller.updateDraftTemplateBasics(
                dbInstance(),
                employee.user,
                now,
                created.id,
                testCreationRequest.copy(name = "changed"),
            )
        }

        assertThrows<BadRequest> {
            controller.updateDraftTemplateContent(
                dbInstance(),
                employee.user,
                now,
                created.id,
                testContent,
            )
        }

        assertThrows<BadRequest> {
            controller.deleteDraftTemplate(dbInstance(), employee.user, now, created.id)
        }

        // validity period can still be updated
        controller.updateTemplateValidity(
            dbInstance(),
            employee.user,
            now,
            created.id,
            DateRange(LocalDate.of(2000, 1, 1), null),
        )
    }

    @Test
    fun `document count`() {
        val otherChild = DevPerson()
        db.transaction { tx -> tx.insert(otherChild, DevPersonType.CHILD) }

        val created =
            controller.createTemplate(
                dbInstance(),
                employee.user,
                now,
                testCreationRequest.copy(validity = DateRange(now.today(), null)),
            )
        controller.publishTemplate(dbInstance(), employee.user, now, created.id)

        controller.getTemplates(dbInstance(), employee.user, now).also {
            assertEquals(1, it.size)
            assertEquals(0, it.first().documentCount)
        }

        childDocumentController.createDocument(
            dbInstance(),
            employee.user,
            now,
            ChildDocumentCreateRequest(otherChild.id, created.id),
        )

        controller.getTemplates(dbInstance(), employee.user, now).also {
            assertEquals(1, it.size)
            assertEquals(1, it.first().documentCount)
        }
    }

    @Test
    fun `template can be duplicated`() {
        val created =
            controller.createTemplate(dbInstance(), employee.user, now, testCreationRequest)
        controller.updateDraftTemplateContent(
            dbInstance(),
            employee.user,
            now,
            created.id,
            testContent,
        )
        controller.publishTemplate(dbInstance(), employee.user, now, created.id)

        val newValidity = DateRange(LocalDate.of(2022, 5, 1), LocalDate.of(2022, 9, 1))
        val copy =
            controller.duplicateTemplate(
                dbInstance(),
                employee.user,
                now,
                created.id,
                DocumentTemplateBasicsRequest.Regular(
                    name = "another",
                    type = ChildDocumentType.PEDAGOGICAL_REPORT,
                    placementTypes = PlacementType.entries.toSet(),
                    language = UiLanguage.SV,
                    DocumentConfidentiality(100, "Laki § 100"),
                    legalBasis = "",
                    validity = newValidity,
                    processDefinitionNumber = "123.456.789b",
                    archiveDurationMonths = 1200,
                    endDecisionWhenUnitChanges = null,
                ),
            )

        assertNotEquals(created.id, copy.id)
        assertEquals("another", copy.name)
        assertEquals(ChildDocumentType.PEDAGOGICAL_REPORT, copy.type)
        assertEquals(UiLanguage.SV, copy.language)
        assertEquals(DocumentConfidentiality(100, "Laki § 100"), copy.confidentiality)
        assertEquals("", copy.legalBasis)
        assertEquals(newValidity, copy.validity)
        assertEquals("123.456.789b", copy.processDefinitionNumber)
        assertEquals(1200, copy.archiveDurationMonths)
        assertEquals(false, copy.published)
        assertEquals(testContent, copy.content)
        assertEquals(copy, controller.getTemplate(dbInstance(), employee.user, now, copy.id))
    }

    @Test
    fun `template can be exported`() {
        val request = testCreationRequest.copy(placementTypes = setOf(PlacementType.DAYCARE))
        val created = controller.createTemplate(dbInstance(), employee.user, now, request)
        controller.updateDraftTemplateContent(
            dbInstance(),
            employee.user,
            now,
            created.id,
            testContent,
        )

        val exported = controller.exportTemplate(dbInstance(), employee.user, now, created.id)

        assertEquals(request.toExported(testContent), exported.body)
    }

    @Test
    fun `active templates endpoint returns valid templates`() {
        val template =
            controller.createTemplate(
                dbInstance(),
                employee.user,
                now,
                testCreationRequest.copy(
                    language = UiLanguage.SV,
                    validity = DateRange(now.today(), null),
                ),
            )
        controller.publishTemplate(dbInstance(), employee.user, now, template.id)

        val active = controller.getActiveTemplates(dbInstance(), employee.user, now, child.id)
        assertEquals(1, active.size)
        assertEquals(template.id, active.first().id)
    }

    @Test
    fun `active templates endpoint does not return pedagogical templates if relevant pilot feature is not enabled`() {
        val pedaTemplate =
            controller.createTemplate(
                dbInstance(),
                employee.user,
                now,
                testCreationRequest.copy(
                    language = UiLanguage.SV,
                    validity = DateRange(now.today(), null),
                    type = ChildDocumentType.VASU,
                ),
            )
        controller.publishTemplate(dbInstance(), employee.user, now, pedaTemplate.id)
        assertEquals(
            1,
            controller.getActiveTemplates(dbInstance(), employee.user, now, child.id).size,
        )

        removePilotFeature(PilotFeature.VASU_AND_PEDADOC)

        assertEquals(
            0,
            controller.getActiveTemplates(dbInstance(), employee.user, now, child.id).size,
        )
    }

    @Test
    fun `active templates endpoint does not return other decision templates if relevant pilot feature is not enabled`() {
        val template =
            controller.createTemplate(
                dbInstance(),
                employee.user,
                now,
                testCreationRequest.copy(
                    language = UiLanguage.SV,
                    validity = DateRange(now.today(), null),
                    endDecisionWhenUnitChanges = true,
                    type = ChildDocumentType.OTHER_DECISION,
                ),
            )
        controller.publishTemplate(dbInstance(), employee.user, now, template.id)
        assertEquals(
            1,
            controller.getActiveTemplates(dbInstance(), employee.user, now, child.id).size,
        )

        removePilotFeature(PilotFeature.OTHER_DECISION)

        assertEquals(
            0,
            controller.getActiveTemplates(dbInstance(), employee.user, now, child.id).size,
        )
    }

    @Test
    fun `active templates endpoint does not return citizen basic templates if relevant pilot feature is not enabled`() {
        val template =
            controller.createTemplate(
                dbInstance(),
                employee.user,
                now,
                testCreationRequest.copy(
                    language = UiLanguage.SV,
                    validity = DateRange(now.today(), null),
                    type = ChildDocumentType.CITIZEN_BASIC,
                ),
            )
        controller.publishTemplate(dbInstance(), employee.user, now, template.id)
        assertEquals(
            1,
            controller.getActiveTemplates(dbInstance(), employee.user, now, child.id).size,
        )

        removePilotFeature(PilotFeature.CITIZEN_BASIC_DOCUMENT)

        assertEquals(
            0,
            controller.getActiveTemplates(dbInstance(), employee.user, now, child.id).size,
        )
    }

    private fun removePilotFeature(pilotFeature: PilotFeature) {
        db.transaction { tx ->
                tx.createUpdate {
                    sql(
                        """
    UPDATE daycare
    SET enabled_pilot_features = array_remove(enabled_pilot_features, '${pilotFeature.name.uppercase()}')
    WHERE id = ${bind(daycare.id)}
    """
                    )
                }
            }
            .execute()
    }

    @Test
    fun `active templates endpoint does not return draft templates`() {
        controller.createTemplate(
            dbInstance(),
            employee.user,
            now,
            testCreationRequest.copy(
                language = UiLanguage.SV,
                validity = DateRange(now.today(), null),
            ),
        )

        val active = controller.getActiveTemplates(dbInstance(), employee.user, now, child.id)
        assertTrue(active.isEmpty())
    }

    @Test
    fun `active templates endpoint does not return future templates`() {
        val template =
            controller.createTemplate(
                dbInstance(),
                employee.user,
                now,
                testCreationRequest.copy(
                    language = UiLanguage.SV,
                    validity = DateRange(now.today().plusDays(1), null),
                ),
            )
        controller.publishTemplate(dbInstance(), employee.user, now, template.id)

        val active = controller.getActiveTemplates(dbInstance(), employee.user, now, child.id)
        assertTrue(active.isEmpty())
    }

    @Test
    fun `active templates endpoint does not return expired templates`() {
        val template =
            controller.createTemplate(
                dbInstance(),
                employee.user,
                now,
                testCreationRequest.copy(
                    language = UiLanguage.SV,
                    validity = DateRange(now.today().minusDays(10), now.today().minusDays(1)),
                ),
            )
        controller.publishTemplate(dbInstance(), employee.user, now, template.id)

        val active = controller.getActiveTemplates(dbInstance(), employee.user, now, child.id)
        assertTrue(active.isEmpty())
    }

    @Test
    fun `active templates endpoint does not return templates in finnish when placement unit is swedish`() {
        val template =
            controller.createTemplate(
                dbInstance(),
                employee.user,
                now,
                testCreationRequest.copy(
                    language = UiLanguage.FI,
                    validity = DateRange(now.today(), null),
                ),
            )
        controller.publishTemplate(dbInstance(), employee.user, now, template.id)

        val active = controller.getActiveTemplates(dbInstance(), employee.user, now, child.id)
        assertTrue(active.isEmpty())
    }

    @Test
    fun `active templates endpoint does not return templates in swedish when placement unit is finnish`() {
        val childId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea(name = "Other Area", shortName = "other_area"))
                val daycareId =
                    tx.insert(
                        DevDaycare(
                            areaId = areaId,
                            name = "Finnish Daycare",
                            language = Language.fi,
                        )
                    )
                val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = daycareId,
                        startDate = now.today(),
                        endDate = now.today().plusDays(5),
                    )
                )
                childId
            }
        val template =
            controller.createTemplate(
                dbInstance(),
                employee.user,
                now,
                testCreationRequest.copy(
                    language = UiLanguage.SV,
                    validity = DateRange(now.today(), null),
                ),
            )
        controller.publishTemplate(dbInstance(), employee.user, now, template.id)

        val active = controller.getActiveTemplates(dbInstance(), employee.user, now, childId)
        assertTrue(active.isEmpty())
    }

    @Test
    fun `active templates endpoint does not return templates where placement type does not match`() {
        val template =
            controller.createTemplate(
                dbInstance(),
                employee.user,
                now,
                testCreationRequest.copy(
                    placementTypes = setOf(PlacementType.PRESCHOOL),
                    language = UiLanguage.SV,
                    validity = DateRange(now.today(), null),
                ),
            )
        controller.publishTemplate(dbInstance(), employee.user, now, template.id)

        val active = controller.getActiveTemplates(dbInstance(), employee.user, now, child.id)
        assertTrue(active.isEmpty())
    }

    @Test
    fun `active templates endpoint returns templates when placement is changing`() {
        val childId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea(name = "Other Area", shortName = "other_area"))
                val daycareId =
                    tx.insert(
                        DevDaycare(
                            areaId = areaId,
                            name = "Finnish Daycare",
                            language = Language.fi,
                            enabledPilotFeatures = setOf(PilotFeature.CITIZEN_BASIC_DOCUMENT),
                        )
                    )
                val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = daycareId,
                        startDate = now.today().minusMonths(2),
                        endDate = now.today().plusDays(5),
                        type = PlacementType.DAYCARE,
                    )
                )
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = daycareId,
                        startDate = now.today().plusDays(6),
                        endDate = now.today().plusMonths(3),
                        type = PlacementType.DAYCARE,
                    )
                )
                childId
            }
        val template =
            controller.createTemplate(
                dbInstance(),
                employee.user,
                now,
                testCreationRequest.copy(
                    type = ChildDocumentType.CITIZEN_BASIC,
                    placementTypes = setOf(PlacementType.DAYCARE),
                    language = UiLanguage.FI,
                    validity = DateRange(now.today().minusMonths(3), null),
                ),
            )
        controller.publishTemplate(dbInstance(), employee.user, now, template.id)

        val active = controller.getActiveTemplates(dbInstance(), employee.user, now, childId)
        assertEquals(1, active.size)
        assertEquals(template.id, active.first().id)
    }

    @Test
    fun `section ids must be unique`() {
        val created =
            controller.createTemplate(dbInstance(), employee.user, now, testCreationRequest)

        assertThrows<BadRequest> {
            controller.updateDraftTemplateContent(
                dbInstance(),
                employee.user,
                now,
                created.id,
                DocumentTemplateContent(
                    sections =
                        listOf(
                            Section(id = "s1", label = "foo", questions = emptyList()),
                            Section(id = "s1", label = "bar", questions = emptyList()),
                        )
                ),
            )
        }
    }

    @Test
    fun `question ids must be unique`() {
        val created =
            controller.createTemplate(dbInstance(), employee.user, now, testCreationRequest)

        assertThrows<BadRequest> {
            controller.updateDraftTemplateContent(
                dbInstance(),
                employee.user,
                now,
                created.id,
                DocumentTemplateContent(
                    sections =
                        listOf(
                            Section(
                                id = "s1",
                                label = "hello",
                                questions =
                                    listOf(
                                        Question.TextQuestion(id = "q1", label = "foo"),
                                        Question.CheckboxQuestion(id = "q1", label = "bar"),
                                    ),
                            )
                        )
                ),
            )
        }
    }
}

private fun DocumentTemplateBasicsRequest.Regular.toExported(content: DocumentTemplateContent) =
    ExportedDocumentTemplate(
        name,
        type,
        placementTypes,
        language,
        confidentiality,
        legalBasis,
        validity,
        processDefinitionNumber,
        archiveDurationMonths,
        archiveExternally,
        endDecisionWhenUnitChanges,
        content,
    )
