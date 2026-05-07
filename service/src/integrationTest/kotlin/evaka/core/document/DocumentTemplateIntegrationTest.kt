// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.document

import evaka.core.CitizenCalendarEnv
import evaka.core.FullApplicationTest
import evaka.core.caseprocess.DocumentConfidentiality
import evaka.core.daycare.domain.Language
import evaka.core.document.childdocument.ChildDocumentController
import evaka.core.document.childdocument.ChildDocumentCreateRequest
import evaka.core.placement.PlacementType
import evaka.core.shared.auth.UserRole
import evaka.core.shared.config.testFeatureConfig
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.NotFound
import evaka.core.shared.domain.UiLanguage
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.PilotFeature
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
    @Autowired lateinit var accessControl: AccessControl
    @Autowired lateinit var citizenCalendarEnv: CitizenCalendarEnv

    private fun controllerWithEnglishFlag(enabled: Boolean) =
        DocumentTemplateController(
            accessControl,
            evakaEnv,
            citizenCalendarEnv,
            testFeatureConfig.copy(allowEnglishChildDocumentsForAllTypes = enabled),
        )

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
    fun `active templates endpoint returns same-language templates and english citizen basic when placement unit is finnish`() {
        val childId = insertChildInUnitWithLanguage(Language.fi, name = "Finnish Daycare")
        listOf(UiLanguage.FI, UiLanguage.SV, UiLanguage.EN).forEach { lang ->
            publishPedagogicalAssessmentTemplate(lang)
            publishCitizenBasicTemplate(lang)
        }

        val active = controller.getActiveTemplates(dbInstance(), employee.user, now, childId)
        assertEquals(
            setOf(
                UiLanguage.FI to ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
                UiLanguage.FI to ChildDocumentType.CITIZEN_BASIC,
                UiLanguage.EN to ChildDocumentType.CITIZEN_BASIC,
            ),
            active.map { it.language to it.type }.toSet(),
        )
    }

    @Test
    fun `active templates endpoint returns same-language templates and english citizen basic when placement unit is swedish`() {
        val childId = insertChildInUnitWithLanguage(Language.sv, name = "Swedish Daycare")
        listOf(UiLanguage.FI, UiLanguage.SV, UiLanguage.EN).forEach { lang ->
            publishPedagogicalAssessmentTemplate(lang)
            publishCitizenBasicTemplate(lang)
        }

        val active = controller.getActiveTemplates(dbInstance(), employee.user, now, childId)
        assertEquals(
            setOf(
                UiLanguage.SV to ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
                UiLanguage.SV to ChildDocumentType.CITIZEN_BASIC,
                UiLanguage.EN to ChildDocumentType.CITIZEN_BASIC,
            ),
            active.map { it.language to it.type }.toSet(),
        )
    }

    @Test
    fun `active templates endpoint returns finnish and english templates when placement unit is english`() {
        val childId = insertChildInUnitWithLanguage(Language.en, name = "English Daycare")
        listOf(UiLanguage.FI, UiLanguage.SV, UiLanguage.EN).forEach { lang ->
            publishPedagogicalAssessmentTemplate(lang)
            publishCitizenBasicTemplate(lang)
        }

        val active = controller.getActiveTemplates(dbInstance(), employee.user, now, childId)
        assertEquals(
            setOf(
                UiLanguage.FI to ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
                UiLanguage.EN to ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
                UiLanguage.FI to ChildDocumentType.CITIZEN_BASIC,
                UiLanguage.EN to ChildDocumentType.CITIZEN_BASIC,
            ),
            active.map { it.language to it.type }.toSet(),
        )
    }

    @Test
    fun `active templates by group id endpoint returns finnish and english but not swedish citizen basic templates when group unit is english`() {
        val groupId = insertGroupInUnitWithLanguage(Language.en, name = "English Daycare")
        listOf(UiLanguage.FI, UiLanguage.SV, UiLanguage.EN).forEach { lang ->
            publishCitizenBasicTemplate(lang)
        }

        val active =
            controller.getActiveTemplatesByGroupId(
                dbInstance(),
                employee.user,
                now,
                groupId,
                setOf(ChildDocumentType.CITIZEN_BASIC),
            )
        assertEquals(setOf(UiLanguage.FI, UiLanguage.EN), active.map { it.language }.toSet())
    }

    @Test
    fun `active templates by group id endpoint returns finnish and english but not swedish citizen basic templates when group unit is finnish`() {
        val groupId = insertGroupInUnitWithLanguage(Language.fi, name = "Finnish Daycare")
        listOf(UiLanguage.FI, UiLanguage.SV, UiLanguage.EN).forEach { lang ->
            publishCitizenBasicTemplate(lang)
        }

        val active =
            controller.getActiveTemplatesByGroupId(
                dbInstance(),
                employee.user,
                now,
                groupId,
                emptySet(),
            )
        assertEquals(setOf(UiLanguage.FI, UiLanguage.EN), active.map { it.language }.toSet())
    }

    @Test
    fun `active templates by group id endpoint does not show citizen basic templates when pilot feature is disabled`() {
        val groupId =
            insertGroupInUnitWithLanguage(
                Language.fi,
                name = "Finnish Daycare",
                enabledPilotFeatures = emptySet(),
            )
        publishCitizenBasicTemplate(UiLanguage.FI)

        val active =
            controller.getActiveTemplatesByGroupId(
                dbInstance(),
                employee.user,
                now,
                groupId,
                emptySet(),
            )
        assertEquals(emptyList(), active)
    }

    @Test
    fun `active templates by group id endpoint does not show english pedagogical templates to finnish unit`() {
        val groupId =
            insertGroupInUnitWithLanguage(
                Language.fi,
                name = "Finnish Daycare",
                enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC),
            )
        publishPedagogicalAssessmentTemplate(UiLanguage.FI)
        publishPedagogicalAssessmentTemplate(UiLanguage.EN)

        val active =
            controller.getActiveTemplatesByGroupId(
                dbInstance(),
                employee.user,
                now,
                groupId,
                emptySet(),
            )
        assertEquals(setOf(UiLanguage.FI), active.map { it.language }.toSet())
    }

    @Test
    fun `active templates by group id endpoint does not show swedish pedagogical template to english unit`() {
        val groupId =
            insertGroupInUnitWithLanguage(
                Language.en,
                name = "English Daycare",
                enabledPilotFeatures = setOf(PilotFeature.VASU_AND_PEDADOC),
            )
        publishPedagogicalAssessmentTemplate(UiLanguage.SV)

        val active =
            controller.getActiveTemplatesByGroupId(
                dbInstance(),
                employee.user,
                now,
                groupId,
                emptySet(),
            )
        assertEquals(emptyList(), active)
    }

    private fun insertChildInUnitWithLanguage(language: Language, name: String) =
        db.transaction { tx ->
            val areaId =
                tx.insert(DevCareArea(name = "Area for $name", shortName = "area_${language.name}"))
            val daycareId =
                tx.insert(
                    DevDaycare(
                        areaId = areaId,
                        name = name,
                        language = language,
                        enabledPilotFeatures =
                            setOf(
                                PilotFeature.VASU_AND_PEDADOC,
                                PilotFeature.OTHER_DECISION,
                                PilotFeature.CITIZEN_BASIC_DOCUMENT,
                            ),
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

    private fun insertGroupInUnitWithLanguage(
        language: Language,
        name: String,
        enabledPilotFeatures: Set<PilotFeature> = setOf(PilotFeature.CITIZEN_BASIC_DOCUMENT),
    ) = db.transaction { tx ->
        val areaId =
            tx.insert(
                DevCareArea(name = "Area for $name", shortName = "area_group_${language.name}")
            )
        val daycareId =
            tx.insert(
                DevDaycare(
                    areaId = areaId,
                    name = name,
                    language = language,
                    enabledPilotFeatures = enabledPilotFeatures,
                )
            )
        tx.insert(DevDaycareGroup(daycareId = daycareId))
    }

    private fun publishPedagogicalAssessmentTemplate(language: UiLanguage) {
        val template =
            controllerWithEnglishFlag(enabled = true)
                .createTemplate(
                    dbInstance(),
                    employee.user,
                    now,
                    testCreationRequest.copy(
                        name = "PA-${language.name}",
                        type = ChildDocumentType.PEDAGOGICAL_ASSESSMENT,
                        language = language,
                        validity = DateRange(now.today(), null),
                    ),
                )
        controller.publishTemplate(dbInstance(), employee.user, now, template.id)
    }

    private fun publishCitizenBasicTemplate(language: UiLanguage) {
        val template =
            controller.createTemplate(
                dbInstance(),
                employee.user,
                now,
                testCreationRequest.copy(
                    name = "CB-${language.name}",
                    type = ChildDocumentType.CITIZEN_BASIC,
                    language = language,
                    confidentiality = null,
                    validity = DateRange(now.today(), null),
                ),
            )
        controller.publishTemplate(dbInstance(), employee.user, now, template.id)
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
        val childId = db.transaction { tx ->
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

    @Test
    fun `English is rejected for non-CITIZEN_BASIC types when flag is off - create`() {
        val request =
            testCreationRequest.copy(
                type = ChildDocumentType.PEDAGOGICAL_REPORT,
                language = UiLanguage.EN,
            )
        val ex =
            assertThrows<BadRequest> {
                controllerWithEnglishFlag(enabled = false)
                    .createTemplate(dbInstance(), employee.user, now, request)
            }
        assertEquals("English is not supported for this document type", ex.message)
    }

    @Test
    fun `English is rejected for non-CITIZEN_BASIC types when flag is off - import`() {
        val exported =
            testCreationRequest
                .copy(type = ChildDocumentType.PEDAGOGICAL_REPORT, language = UiLanguage.EN)
                .toExported(testContent)
        val ex =
            assertThrows<BadRequest> {
                controllerWithEnglishFlag(enabled = false)
                    .importTemplate(dbInstance(), employee.user, now, exported)
            }
        assertEquals("English is not supported for this document type", ex.message)
    }

    @Test
    fun `English is rejected for non-CITIZEN_BASIC types when flag is off - duplicate`() {
        val disabledController = controllerWithEnglishFlag(enabled = false)
        val created =
            disabledController.createTemplate(dbInstance(), employee.user, now, testCreationRequest)
        val ex =
            assertThrows<BadRequest> {
                disabledController.duplicateTemplate(
                    dbInstance(),
                    employee.user,
                    now,
                    created.id,
                    testCreationRequest.copy(
                        name = "dup",
                        type = ChildDocumentType.PEDAGOGICAL_REPORT,
                        language = UiLanguage.EN,
                    ),
                )
            }
        assertEquals("English is not supported for this document type", ex.message)
    }

    @Test
    fun `English is rejected for non-CITIZEN_BASIC types when flag is off - update`() {
        val disabledController = controllerWithEnglishFlag(enabled = false)
        val created =
            disabledController.createTemplate(dbInstance(), employee.user, now, testCreationRequest)
        val ex =
            assertThrows<BadRequest> {
                disabledController.updateDraftTemplateBasics(
                    dbInstance(),
                    employee.user,
                    now,
                    created.id,
                    testCreationRequest.copy(
                        type = ChildDocumentType.PEDAGOGICAL_REPORT,
                        language = UiLanguage.EN,
                    ),
                )
            }
        assertEquals("English is not supported for this document type", ex.message)
    }

    @Test
    fun `English is accepted for non-CITIZEN_BASIC types when flag is on - create`() {
        val englishController = controllerWithEnglishFlag(enabled = true)
        val request =
            testCreationRequest.copy(
                type = ChildDocumentType.PEDAGOGICAL_REPORT,
                language = UiLanguage.EN,
            )
        val created = englishController.createTemplate(dbInstance(), employee.user, now, request)
        assertEquals(UiLanguage.EN, created.language)
        assertEquals(ChildDocumentType.PEDAGOGICAL_REPORT, created.type)
    }

    @Test
    fun `English is accepted for non-CITIZEN_BASIC types when flag is on - update`() {
        val englishController = controllerWithEnglishFlag(enabled = true)
        val created =
            englishController.createTemplate(dbInstance(), employee.user, now, testCreationRequest)
        englishController.updateDraftTemplateBasics(
            dbInstance(),
            employee.user,
            now,
            created.id,
            testCreationRequest.copy(
                type = ChildDocumentType.PEDAGOGICAL_REPORT,
                language = UiLanguage.EN,
            ),
        )
        val updated = englishController.getTemplate(dbInstance(), employee.user, now, created.id)
        assertEquals(UiLanguage.EN, updated.language)
        assertEquals(ChildDocumentType.PEDAGOGICAL_REPORT, updated.type)
    }

    @Test
    fun `English is accepted for non-CITIZEN_BASIC types when flag is on - import`() {
        val englishController = controllerWithEnglishFlag(enabled = true)
        val exported =
            testCreationRequest
                .copy(type = ChildDocumentType.PEDAGOGICAL_REPORT, language = UiLanguage.EN)
                .toExported(testContent)
        val imported = englishController.importTemplate(dbInstance(), employee.user, now, exported)
        assertEquals(UiLanguage.EN, imported.language)
        assertEquals(ChildDocumentType.PEDAGOGICAL_REPORT, imported.type)
    }

    @Test
    fun `English is accepted for non-CITIZEN_BASIC types when flag is on - duplicate`() {
        val englishController = controllerWithEnglishFlag(enabled = true)
        val created =
            englishController.createTemplate(dbInstance(), employee.user, now, testCreationRequest)
        val duplicated =
            englishController.duplicateTemplate(
                dbInstance(),
                employee.user,
                now,
                created.id,
                testCreationRequest.copy(
                    name = "dup",
                    type = ChildDocumentType.PEDAGOGICAL_REPORT,
                    language = UiLanguage.EN,
                ),
            )
        assertEquals(UiLanguage.EN, duplicated.language)
        assertEquals(ChildDocumentType.PEDAGOGICAL_REPORT, duplicated.type)
    }

    @Test
    fun `English is accepted for CITIZEN_BASIC regardless of flag`() {
        val request =
            testCreationRequest.copy(
                type = ChildDocumentType.CITIZEN_BASIC,
                language = UiLanguage.EN,
                confidentiality = null,
            )
        val createdFlagOff =
            controllerWithEnglishFlag(enabled = false)
                .createTemplate(dbInstance(), employee.user, now, request)
        assertEquals(UiLanguage.EN, createdFlagOff.language)
        assertEquals(ChildDocumentType.CITIZEN_BASIC, createdFlagOff.type)

        val createdFlagOn =
            controllerWithEnglishFlag(enabled = true)
                .createTemplate(dbInstance(), employee.user, now, request.copy(name = "cb2"))
        assertEquals(UiLanguage.EN, createdFlagOn.language)
        assertEquals(ChildDocumentType.CITIZEN_BASIC, createdFlagOn.type)
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
