// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.KoskiEnv
import fi.espoo.evaka.OphEnv
import fi.espoo.evaka.assistanceaction.AssistanceMeasure
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevAssistanceAction
import fi.espoo.evaka.shared.dev.DevAssistanceNeed
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestAssistanceAction
import fi.espoo.evaka.shared.dev.insertTestAssistanceNeed
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_7
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KoskiIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private lateinit var koskiServer: MockKoskiServer
    private lateinit var koskiTester: KoskiTester

    @BeforeAll
    fun initDependencies() {
        koskiServer = MockKoskiServer.start()
        koskiTester =
            KoskiTester(
                db,
                KoskiClient(
                    KoskiEnv.fromEnvironment(env)
                        .copy(
                            url = "http://localhost:${koskiServer.port}",
                        ),
                    OphEnv.fromEnvironment(env),
                    fuel = http,
                    asyncJobRunner = null
                )
            )
    }

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.setUnitOids()
        }
        koskiServer.clearData()
    }

    @Test
    fun `won't send same data twice (input cache check)`() {
        insertPlacement()

        fun assertSingleStudyRight() =
            db.read { it.getStoredResults() }
                .let {
                    val stored = it.single()
                    val sent = koskiServer.getStudyRights().entries.single()
                    assertEquals(stored.studyRightOid, sent.key)
                    assertEquals(0, sent.value.version)
                }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertSingleStudyRight()

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(2))
        assertSingleStudyRight()
    }

    @Test
    fun `won't send same data twice (payload check)`() {
        insertPlacement()

        fun assertSingleStudyRight() =
            db.read { it.getStoredResults() }
                .let {
                    val stored = it.single()
                    val sent = koskiServer.getStudyRights().entries.single()
                    assertEquals(stored.studyRightOid, sent.key)
                    assertEquals(0, sent.value.version)
                }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertSingleStudyRight()

        db.transaction { it.clearKoskiInputCache() }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(2))
        assertSingleStudyRight()
    }

    @Test
    fun `voiding considers status 404 as success`() {
        val placementId = insertPlacement()

        fun countActiveStudyRights() =
            db.read {
                it.createQuery("SELECT count(*) FROM koski_study_right WHERE void_date IS NULL")
                    .mapTo<Long>()
                    .one()
            }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertEquals(1, koskiServer.getStudyRights().values.size)
        assertEquals(1, countActiveStudyRights())

        koskiServer.clearData()

        db.transaction { it.execute("DELETE FROM placement WHERE id = ?", placementId) }
        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(2))
        assertEquals(0, koskiServer.getStudyRights().values.size)
        assertEquals(0, countActiveStudyRights())
    }

    @Test
    fun `will send again if placement updated`() {
        val placementId = insertPlacement()

        fun assertSingleStudyRight(version: Int) =
            db.read { it.getStoredResults() }
                .let {
                    val stored = it.single()
                    val sent = koskiServer.getStudyRights().entries.single()
                    assertEquals(stored.studyRightOid, sent.key)
                    assertEquals(version, sent.value.version)
                }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertSingleStudyRight(version = 0)

        db.transaction {
            it.createUpdate("UPDATE placement SET end_date = :endDate WHERE id = :id")
                .bind("id", placementId)
                .bind("endDate", preschoolTerm2019.end.minusDays(1))
                .execute()
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(2))
        assertSingleStudyRight(version = 1)
    }

    @Test
    fun `preschool ended within 30 days of term end qualifies`() {
        insertPlacement(
            period = FiniteDateRange(LocalDate.of(2018, 8, 1), LocalDate.of(2019, 5, 1))
        )

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(LocalDate.of(2018, 8, 1)),
                Opiskeluoikeusjakso.valmistunut(LocalDate.of(2019, 5, 1))
            ),
            koskiServer.getStudyRights().values.single().opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `preschool ended earlier than 30 days of term end is considered cancelled`() {
        insertPlacement(
            period = FiniteDateRange(LocalDate.of(2018, 8, 1), LocalDate.of(2019, 4, 30))
        )

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(LocalDate.of(2018, 8, 1)),
                Opiskeluoikeusjakso.eronnut(LocalDate.of(2019, 4, 30))
            ),
            koskiServer.getStudyRights().values.single().opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `preschool started late still qualifies`() {
        insertPlacement(
            period = FiniteDateRange(LocalDate.of(2018, 12, 1), LocalDate.of(2019, 5, 1))
        )

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(LocalDate.of(2018, 12, 1)),
                Opiskeluoikeusjakso.valmistunut(LocalDate.of(2019, 5, 1))
            ),
            koskiServer.getStudyRights().values.single().opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `simple preparatory education placement in the past covering preschool term overly`() {
        val august2019 = LocalDate.of(2019, 8, 1)
        insertPlacement(
            type = PlacementType.PREPARATORY,
            period = FiniteDateRange(august2019, preschoolTerm2019.end)
        )

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        val studyRight = koskiServer.getStudyRights().values.single().opiskeluoikeus
        assertEquals(OpiskeluoikeudenTyyppiKoodi.PREPARATORY, studyRight.tyyppi.koodiarvo)
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(august2019),
                Opiskeluoikeusjakso.valmistunut(preschoolTerm2019.end)
            ),
            studyRight.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `simple preschool placement changes to preparatory`() {
        insertPlacement(
            type = PlacementType.PRESCHOOL,
            period = FiniteDateRange(preschoolTerm2019.start, preschoolTerm2019.start.plusMonths(4))
        )
        insertPlacement(
            type = PlacementType.PREPARATORY,
            period =
                FiniteDateRange(
                    preschoolTerm2019.start.plusMonths(4).plusDays(1),
                    preschoolTerm2019.end
                )
        )

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        val studyRights =
            koskiServer.getStudyRights().values.let { studyRights ->
                Pair(
                    studyRights.single {
                        it.opiskeluoikeus.tyyppi.koodiarvo == OpiskeluoikeudenTyyppiKoodi.PRESCHOOL
                    },
                    studyRights.single {
                        it.opiskeluoikeus.tyyppi.koodiarvo ==
                            OpiskeluoikeudenTyyppiKoodi.PREPARATORY
                    }
                )
            }
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start),
                Opiskeluoikeusjakso.eronnut(preschoolTerm2019.start.plusMonths(4))
            ),
            studyRights.first.opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start.plusMonths(4).plusDays(1)),
                Opiskeluoikeusjakso.valmistunut(preschoolTerm2019.end)
            ),
            studyRights.second.opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `two preparatory placements maps to two study rights`() {
        val start = preschoolTerm2019.start
        val end = preschoolTerm2019.end

        val firstPlacementEnd = start.plusMonths(1)
        insertPlacement(
            period = FiniteDateRange(start, firstPlacementEnd),
            type = PlacementType.PREPARATORY
        )

        val secondPlacementStart = firstPlacementEnd.plusDays(1)
        insertPlacement(
            period = FiniteDateRange(secondPlacementStart, end),
            daycareId = testDaycare2.id,
            type = PlacementType.PREPARATORY
        )

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        val stored =
            db.read { it.getStoredResults() }
                .let { studyRights ->
                    listOf(
                        studyRights.single { it.unitId == testDaycare.id },
                        studyRights.single { it.unitId == testDaycare2.id }
                    )
                }
        val studyRights =
            koskiServer.getStudyRights().let { studyRights ->
                stored.map { studyRights[it.studyRightOid]!! }
            }

        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(start),
                Opiskeluoikeusjakso.eronnut(firstPlacementEnd)
            ),
            studyRights[0].opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(secondPlacementStart),
                Opiskeluoikeusjakso.valmistunut(end)
            ),
            studyRights[1].opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `pending placements can be limited by person or daycare`() {
        val today = preschoolTerm2019.end
        insertPlacement()

        val searchByExistingPerson =
            db.read {
                it.getPendingStudyRights(
                    today,
                    KoskiSearchParams(personIds = listOf(testChild_1.id))
                )
            }
        assertEquals(1, searchByExistingPerson.size)

        val searchByRandomPerson =
            db.read {
                it.getPendingStudyRights(
                    today,
                    KoskiSearchParams(personIds = listOf(ChildId(UUID.randomUUID())))
                )
            }
        assertEquals(0, searchByRandomPerson.size)

        val searchByExistingDaycare =
            db.read {
                it.getPendingStudyRights(
                    today,
                    KoskiSearchParams(daycareIds = listOf(testDaycare.id))
                )
            }
        assertEquals(1, searchByExistingDaycare.size)

        val searchByRandomDaycare =
            db.read {
                it.getPendingStudyRights(
                    today,
                    KoskiSearchParams(daycareIds = listOf(DaycareId(UUID.randomUUID())))
                )
            }
        assertEquals(0, searchByRandomDaycare.size)
    }

    @Test
    fun `assistance needs are converted to Koski extra information`() {
        data class TestCase(val period: FiniteDateRange, val basis: String)
        insertPlacement(testChild_1)
        val testCases =
            listOf(
                TestCase(testPeriod(0L to 1L), "DEVELOPMENTAL_DISABILITY_1"),
                TestCase(testPeriod(2L to 3L), "DEVELOPMENTAL_DISABILITY_2")
            )
        val actionPeriod = testPeriod(0L to 3L)
        db.transaction { tx ->
            testCases.forEach {
                tx.insertTestAssistanceNeed(
                    DevAssistanceNeed(
                        updatedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                        childId = testChild_1.id,
                        startDate = it.period.start,
                        endDate = it.period.end,
                        bases = setOf(it.basis)
                    )
                )
            }
            // Koski validation rules require extended compulsory education when developmental
            // disability
            // date ranges
            // are present
            tx.insertTestAssistanceAction(
                DevAssistanceAction(
                    updatedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                    childId = testChild_1.id,
                    startDate = actionPeriod.start,
                    endDate = actionPeriod.end,
                    measures = setOf(AssistanceMeasure.EXTENDED_COMPULSORY_EDUCATION),
                    actions = emptySet()
                )
            )
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertEquals(
            Lisätiedot(
                vammainen = listOf(Aikajakso.from(testCases[0].period)),
                vaikeastiVammainen = listOf(Aikajakso.from(testCases[1].period)),
                pidennettyOppivelvollisuus = Aikajakso.from(actionPeriod),
                kuljetusetu = null,
                erityisenTuenPäätökset = null
            ),
            koskiServer.getStudyRights().values.single().opiskeluoikeus.lisätiedot
        )
    }

    @Test
    fun `assistance actions are converted to Koski extra information`() {
        data class TestCase(
            val period: FiniteDateRange,
            val measure: AssistanceMeasure,
            val action: String? = null
        )
        insertPlacement(testChild_1)
        val testCases =
            listOf(
                TestCase(testPeriod(0L to 1L), AssistanceMeasure.EXTENDED_COMPULSORY_EDUCATION),
                TestCase(testPeriod(2L to 3L), AssistanceMeasure.TRANSPORT_BENEFIT),
                TestCase(
                    testPeriod(4L to 5L),
                    AssistanceMeasure.SPECIAL_ASSISTANCE_DECISION,
                    "SPECIAL_GROUP"
                )
            )
        db.transaction { tx ->
            tx.insertTestAssistanceNeed(
                DevAssistanceNeed(
                    updatedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                    childId = testChild_1.id,
                    startDate = testCases[0].period.start,
                    endDate = testCases[0].period.end,
                    bases = setOf("DEVELOPMENTAL_DISABILITY_1")
                )
            )
            testCases.forEach {
                tx.insertTestAssistanceAction(
                    DevAssistanceAction(
                        updatedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                        childId = testChild_1.id,
                        startDate = it.period.start,
                        endDate = it.period.end,
                        measures = setOf(it.measure),
                        actions = listOfNotNull(it.action).toSet()
                    )
                )
            }
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertEquals(
            Lisätiedot(
                vammainen = listOf(Aikajakso.from(testCases[0].period)),
                vaikeastiVammainen = null,
                pidennettyOppivelvollisuus = Aikajakso.from(testCases[0].period),
                kuljetusetu = Aikajakso.from(testCases[1].period),
                erityisenTuenPäätökset =
                    listOf(
                        ErityisenTuenPäätös(
                            alku = testCases[2].period.start,
                            loppu = testCases[2].period.end,
                            erityisryhmässä = true,
                            opiskeleeToimintaAlueittain = false
                        )
                    )
            ),
            koskiServer.getStudyRights().values.single().opiskeluoikeus.lisätiedot
        )
    }

    @Test
    fun `sent extended compulsory education date ranges are adjusted to fit Koski requirements`() {
        // https://github.com/Opetushallitus/koski/pull/1860

        insertPlacement(testChild_1)
        val assistanceNeeds =
            listOf(
                Pair(testPeriod(0L to 1L), "DEVELOPMENTAL_DISABILITY_1"),
                Pair(testPeriod(4L to 8L), "DEVELOPMENTAL_DISABILITY_2")
            )
        db.transaction { tx ->
            assistanceNeeds.forEach {
                tx.insertTestAssistanceNeed(
                    DevAssistanceNeed(
                        updatedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                        childId = testChild_1.id,
                        startDate = it.first.start,
                        endDate = it.first.end,
                        bases = setOf(it.second)
                    )
                )
            }
            val assistanceActions = listOf(testPeriod(1L to 3L), testPeriod(4L to 7L))
            assistanceActions.forEach {
                tx.insertTestAssistanceAction(
                    DevAssistanceAction(
                        updatedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                        childId = testChild_1.id,
                        startDate = it.start,
                        endDate = it.end,
                        measures = setOf(AssistanceMeasure.EXTENDED_COMPULSORY_EDUCATION),
                        actions = emptySet()
                    )
                )
            }
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertEquals(
            Lisätiedot(
                vammainen = listOf(Aikajakso.from(assistanceNeeds[0].first)),
                vaikeastiVammainen = listOf(Aikajakso.from(assistanceNeeds[1].first)),
                pidennettyOppivelvollisuus = Aikajakso.from(testPeriod(4L to 7L)),
                kuljetusetu = null,
                erityisenTuenPäätökset = null
            ),
            koskiServer.getStudyRights().values.single().opiskeluoikeus.lisätiedot
        )
    }

    @Test
    fun `adjacent transport benefit ranges are sent as one joined range`() {
        insertPlacement(testChild_1)
        val assistanceActions =
            listOf(
                testPeriod(1L to 1L),
                testPeriod(2L to 2L),
                testPeriod(3L to 4L),
                testPeriod(6L to 7L)
            )
        db.transaction { tx ->
            assistanceActions.forEach {
                tx.insertTestAssistanceAction(
                    DevAssistanceAction(
                        updatedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                        childId = testChild_1.id,
                        startDate = it.start,
                        endDate = it.end,
                        measures = setOf(AssistanceMeasure.TRANSPORT_BENEFIT),
                        actions = emptySet()
                    )
                )
            }
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertEquals(
            Lisätiedot(
                vammainen = null,
                vaikeastiVammainen = null,
                pidennettyOppivelvollisuus = null,
                kuljetusetu = Aikajakso.from(testPeriod(1L to 4L)),
                erityisenTuenPäätökset = null
            ),
            koskiServer.getStudyRights().values.single().opiskeluoikeus.lisätiedot
        )
    }

    @Test
    fun `disability date ranges are only sent if extended compulsory education is sent`() {
        // https://github.com/Opetushallitus/koski/pull/1860

        insertPlacement(testChild_1)
        val assistanceNeeds =
            listOf(
                Pair(testPeriod(0L to 6L), "DEVELOPMENTAL_DISABILITY_1"),
            )
        db.transaction { tx ->
            assistanceNeeds.forEach {
                tx.insertTestAssistanceNeed(
                    DevAssistanceNeed(
                        updatedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                        childId = testChild_1.id,
                        startDate = it.first.start,
                        endDate = it.first.end,
                        bases = setOf(it.second)
                    )
                )
            }
            val actionPeriod = testPeriod(7L to 8L)
            tx.insertTestAssistanceAction(
                DevAssistanceAction(
                    updatedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                    childId = testChild_1.id,
                    startDate = actionPeriod.start,
                    endDate = actionPeriod.end,
                    measures =
                        setOf(
                            AssistanceMeasure.EXTENDED_COMPULSORY_EDUCATION,
                            AssistanceMeasure.TRANSPORT_BENEFIT
                        ),
                    actions = emptySet()
                )
            )
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertEquals(
            Lisätiedot(
                vammainen = null,
                vaikeastiVammainen = null,
                pidennettyOppivelvollisuus = null,
                kuljetusetu = Aikajakso.from(testPeriod(7L to 8L)),
                erityisenTuenPäätökset = null
            ),
            koskiServer.getStudyRights().values.single().opiskeluoikeus.lisätiedot
        )
    }

    @Test
    fun `gaps in placements results in temporary interruptions (eventually qualified)`() {
        insertPlacement(testChild_1, period = testPeriod((0L to 2L)))
        insertPlacement(testChild_1, period = testPeriod((10L to 12L)))
        insertPlacement(testChild_1, period = testPeriod((15L to null)))

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start),
                Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(preschoolTerm2019.start.plusDays(3)),
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start.plusDays(10)),
                Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(preschoolTerm2019.start.plusDays(13)),
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start.plusDays(15)),
                Opiskeluoikeusjakso.valmistunut(preschoolTerm2019.end)
            ),
            koskiServer.getStudyRights().values.single().opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `gaps in placements results in temporary interruptions (eventually resigned)`() {
        insertPlacement(testChild_1, period = testPeriod((0L to 2L)))
        insertPlacement(testChild_1, period = testPeriod((10L to 12L)))
        insertPlacement(testChild_1, period = testPeriod((15L to 16L)))

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start),
                Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(preschoolTerm2019.start.plusDays(3)),
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start.plusDays(10)),
                Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(preschoolTerm2019.start.plusDays(13)),
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start.plusDays(15)),
                Opiskeluoikeusjakso.eronnut(preschoolTerm2019.start.plusDays(16))
            ),
            koskiServer.getStudyRights().values.single().opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `placements starting in the future are not included in Koski study right processing`() {
        insertPlacement(testChild_1, period = testPeriod((0L to 2L)))
        insertPlacement(testChild_1, period = testPeriod((4L to null)))

        fun assertStudyRight(dateRanges: List<Opiskeluoikeusjakso>, qualified: Boolean) {
            val opiskeluoikeus = koskiServer.getStudyRights().values.single().opiskeluoikeus
            val suoritus = opiskeluoikeus.suoritukset.single()
            assertEquals(dateRanges, opiskeluoikeus.tila.opiskeluoikeusjaksot)
            if (qualified) {
                assertNotNull(suoritus.vahvistus)
            } else {
                assertNull(suoritus.vahvistus)
            }
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.start.plusDays(1))
        assertStudyRight(
            listOf(Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start)),
            qualified = false
        )

        koskiTester.triggerUploads(today = preschoolTerm2019.start.plusDays(3))
        assertStudyRight(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start),
                Opiskeluoikeusjakso.eronnut(preschoolTerm2019.start.plusDays(2))
            ),
            qualified = false
        )

        koskiTester.triggerUploads(today = preschoolTerm2019.start.plusDays(5))
        assertStudyRight(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start),
                Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(preschoolTerm2019.start.plusDays(3)),
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start.plusDays(4))
            ),
            qualified = false
        )

        koskiTester.triggerUploads(today = preschoolTerm2019.end.minusDays(1))
        assertStudyRight(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start),
                Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(preschoolTerm2019.start.plusDays(3)),
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start.plusDays(4))
            ),
            qualified = false
        )

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertStudyRight(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start),
                Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(preschoolTerm2019.start.plusDays(3)),
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start.plusDays(4)),
                Opiskeluoikeusjakso.valmistunut(preschoolTerm2019.end)
            ),
            qualified = true
        )
    }

    @Test
    fun `a child can be in preschool for the duration of two terms`() {
        insertPlacement(
            testChild_1,
            period = FiniteDateRange(preschoolTerm2019.start, preschoolTerm2019.end)
        )
        insertPlacement(
            testChild_1,
            period = FiniteDateRange(preschoolTerm2020.start, preschoolTerm2020.end)
        )

        val today = preschoolTerm2020.end.plusDays(1)
        koskiTester.triggerUploads(today)

        val opiskeluoikeus = koskiServer.getStudyRights().values.single().opiskeluoikeus
        val suoritus = opiskeluoikeus.suoritukset.single()
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start),
                Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(preschoolTerm2019.end.plusDays(1)),
                Opiskeluoikeusjakso.läsnä(preschoolTerm2020.start),
                Opiskeluoikeusjakso.valmistunut(preschoolTerm2020.end)
            ),
            opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
        assertNotNull(suoritus.vahvistus)
        assertEquals(preschoolTerm2020.end, suoritus.vahvistus?.päivä)
    }

    @Test
    fun `if a study right is voided and a new placement is later added, a fresh study right is sent`() {
        insertPlacement()

        val today = preschoolTerm2019.end.plusDays(1)
        koskiTester.triggerUploads(today)

        val oldOid = koskiServer.getStudyRights().keys.single()
        db.transaction { it.createUpdate("DELETE FROM placement").execute() }
        koskiTester.triggerUploads(today.plusDays(1))
        assertTrue(koskiServer.getStudyRights().isEmpty())

        db.transaction { it.clearKoskiInputCache() }
        koskiTester.triggerUploads(today.plusDays(2))
        assertTrue(koskiServer.getStudyRights().isEmpty())

        insertPlacement()
        koskiTester.triggerUploads(today.plusDays(3))

        val newOid = koskiServer.getStudyRights().keys.single()
        assertNotEquals(oldOid, newOid)
    }

    @Test
    fun `a daycare with purchased provider type is marked as such in study rights`() {
        val daycareId =
            db.transaction {
                it.insertTestDaycare(
                    DevDaycare(areaId = testArea.id, providerType = ProviderType.PURCHASED)
                )
            }
        insertPlacement(daycareId = daycareId)

        val today = preschoolTerm2019.end.plusDays(1)
        koskiTester.triggerUploads(today)

        val opiskeluoikeus = koskiServer.getStudyRights().values.single().opiskeluoikeus
        assertEquals(
            Järjestämismuoto(JärjestämismuotoKoodi.PURCHASED),
            opiskeluoikeus.järjestämismuoto
        )
    }

    @Test
    fun `a daycare with private provider type is marked as purchased in study rights`() {
        val daycareId =
            db.transaction {
                it.insertTestDaycare(
                    DevDaycare(areaId = testArea.id, providerType = ProviderType.PRIVATE)
                )
            }
        insertPlacement(daycareId = daycareId)

        val today = preschoolTerm2019.end.plusDays(1)
        koskiTester.triggerUploads(today)

        val opiskeluoikeus = koskiServer.getStudyRights().values.single().opiskeluoikeus
        assertEquals(
            Järjestämismuoto(JärjestämismuotoKoodi.PURCHASED),
            opiskeluoikeus.järjestämismuoto
        )
    }

    @Test
    fun `absences have no effect on preschool study rights`() {
        insertPlacement(period = preschoolTerm2020, type = PlacementType.PRESCHOOL)

        val today = preschoolTerm2020.end.plusDays(1)
        koskiTester.triggerUploads(today)

        insertAbsences(
            testChild_1.id,
            AbsenceType.UNKNOWN_ABSENCE,
            FiniteDateRange(preschoolTerm2020.start.plusDays(1), preschoolTerm2020.end.minusDays(1))
        )

        val opiskeluoikeus = koskiServer.getStudyRights().values.single().opiskeluoikeus
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2020.start),
                Opiskeluoikeusjakso.valmistunut(preschoolTerm2020.end)
            ),
            opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `absences have no effect on pre-2020 preparatory study rights`() {
        insertPlacement(period = preschoolTerm2019, type = PlacementType.PRESCHOOL)

        val today = preschoolTerm2019.end.plusDays(1)
        koskiTester.triggerUploads(today)

        insertAbsences(
            testChild_1.id,
            AbsenceType.UNKNOWN_ABSENCE,
            FiniteDateRange(preschoolTerm2019.start.plusDays(1), preschoolTerm2019.end.minusDays(1))
        )

        val opiskeluoikeus = koskiServer.getStudyRights().values.single().opiskeluoikeus
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start),
                Opiskeluoikeusjakso.valmistunut(preschoolTerm2019.end)
            ),
            opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `holidays longer than 7 days are included in preparatory study rights`() {
        val holiday =
            FiniteDateRange(
                preschoolTerm2020.start.plusDays(1),
                preschoolTerm2020.start.plusDays(1L + 8)
            )
        insertPlacement(period = preschoolTerm2020, type = PlacementType.PREPARATORY)
        insertAbsences(testChild_1.id, AbsenceType.PLANNED_ABSENCE, holiday)

        val today = preschoolTerm2020.end.plusDays(1)
        koskiTester.triggerUploads(today)

        val opiskeluoikeus = koskiServer.getStudyRights().values.single().opiskeluoikeus
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2020.start),
                Opiskeluoikeusjakso.loma(holiday.start),
                Opiskeluoikeusjakso.läsnä(holiday.end.plusDays(1)),
                Opiskeluoikeusjakso.valmistunut(preschoolTerm2020.end)
            ),
            opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `preparatory is considered resigned if absence periods longer than week make up more than 30 days in total`() {
        insertPlacement(period = preschoolTerm2020, type = PlacementType.PREPARATORY)
        val absences =
            listOf(
                FiniteDateRange(
                    preschoolTerm2020.start.plusDays(1),
                    preschoolTerm2020.start.plusDays(20)
                ),
                FiniteDateRange(
                    preschoolTerm2020.start.plusDays(50),
                    preschoolTerm2020.start.plusDays(80)
                )
            )
        insertAbsences(testChild_1.id, AbsenceType.UNKNOWN_ABSENCE, *absences.toTypedArray())

        val today = preschoolTerm2020.end.plusDays(1)
        koskiTester.triggerUploads(today)

        val opiskeluoikeus = koskiServer.getStudyRights().values.single().opiskeluoikeus
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2020.start),
                Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(absences[0].start),
                Opiskeluoikeusjakso.läsnä(absences[0].end.plusDays(1)),
                Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(absences[1].start),
                Opiskeluoikeusjakso.läsnä(absences[1].end.plusDays(1)),
                Opiskeluoikeusjakso.eronnut(preschoolTerm2020.end)
            ),
            opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
        val suoritus = opiskeluoikeus.suoritukset.single()
        assertNull(suoritus.vahvistus)
    }

    @Test
    fun `preparatory is not considered resigned even with many random absence days which are non-contiguous`() {
        insertPlacement(period = preschoolTerm2020, type = PlacementType.PREPARATORY)

        val absencesEveryOtherDay =
            (1..90 step 2).map { preschoolTerm2020.start.plusDays(it.toLong()).toFiniteDateRange() }
        assertEquals(45, absencesEveryOtherDay.size)
        insertAbsences(
            testChild_1.id,
            AbsenceType.UNKNOWN_ABSENCE,
            *absencesEveryOtherDay.toTypedArray()
        )

        val today = preschoolTerm2020.end.plusDays(1)
        koskiTester.triggerUploads(today)

        val opiskeluoikeus = koskiServer.getStudyRights().values.single().opiskeluoikeus
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2020.start),
                Opiskeluoikeusjakso.valmistunut(preschoolTerm2020.end)
            ),
            opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `if a child has no ssn but has an oid, study rights are sent normally`() {
        val personOid = "1.2.246.562.24.23736347564"

        insertPlacement(child = testChild_7)
        db.transaction {
            it.createUpdate("UPDATE person SET oph_person_oid = :oid WHERE id = :id")
                .bind("id", testChild_7.id)
                .bind("oid", personOid)
                .execute()
        }
        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))

        val studyRights = koskiServer.getStudyRights()
        assertEquals(1, studyRights.size)
        val studyRightOid = studyRights.values.first().opiskeluoikeus.oid
        assertEquals(setOf(studyRightOid), koskiServer.getPersonStudyRights(personOid))
    }

    @Test
    fun `if a child switches daycares in May, one study right is marked resigned and only one is marked qualified`() {
        val firstPlacement = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 5, 5))
        val secondPlacement = FiniteDateRange(LocalDate.of(2020, 5, 6), LocalDate.of(2020, 6, 1))
        insertPlacement(
            period = firstPlacement,
            type = PlacementType.PRESCHOOL,
            daycareId = testDaycare.id
        )
        insertPlacement(
            period = secondPlacement,
            type = PlacementType.PRESCHOOL,
            daycareId = testDaycare2.id
        )

        koskiTester.triggerUploads(today = LocalDate.of(2020, 7, 1))

        val studyRights =
            koskiServer.getStudyRights().values.sortedBy {
                it.opiskeluoikeus.suoritukset[0].toimipiste.oid
            }
        assertEquals(2, studyRights.size)
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(firstPlacement.start),
                Opiskeluoikeusjakso.eronnut(firstPlacement.end)
            ),
            studyRights[0].opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(secondPlacement.start),
                Opiskeluoikeusjakso.valmistunut(secondPlacement.end)
            ),
            studyRights[1].opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `municipality of preparatory education confirmation is as configured`() {
        insertPlacement()

        val testerWithMunicipalityCode =
            KoskiTester(
                db,
                KoskiClient(
                    KoskiEnv.fromEnvironment(env)
                        .copy(
                            url = "http://localhost:${koskiServer.port}",
                        ),
                    OphEnv.fromEnvironment(env).copy(municipalityCode = "001"),
                    fuel = http,
                    asyncJobRunner = null
                )
            )

        testerWithMunicipalityCode.triggerUploads(today = preschoolTerm2019.end.plusDays(1))

        assertEquals(
            "001",
            koskiServer
                .getStudyRights()
                .values
                .first()
                .opiskeluoikeus
                .suoritukset
                .first()
                .vahvistus
                ?.paikkakunta
                ?.koodiarvo
        )
    }

    private fun insertPlacement(
        child: DevPerson = testChild_1,
        daycareId: DaycareId = testDaycare.id,
        period: FiniteDateRange = preschoolTerm2019,
        type: PlacementType = PlacementType.PRESCHOOL
    ): PlacementId =
        db.transaction {
            it.insertTestPlacement(
                DevPlacement(
                    childId = child.id,
                    unitId = daycareId,
                    startDate = period.start,
                    endDate = period.end,
                    type = type
                )
            )
        }

    private fun insertAbsences(
        childId: ChildId,
        absenceType: AbsenceType,
        vararg periods: FiniteDateRange
    ) =
        db.transaction { tx ->
            for (period in periods) {
                for (date in period.dates()) {
                    tx.insertTestAbsence(
                        childId = childId,
                        category = AbsenceCategory.NONBILLABLE,
                        date = date,
                        absenceType = absenceType
                    )
                }
            }
        }
}

private fun Database.Transaction.clearKoskiInputCache() =
    createUpdate("UPDATE koski_study_right SET input_data = NULL").execute()

private val preschoolTerm2019 = FiniteDateRange(LocalDate.of(2019, 8, 8), LocalDate.of(2020, 5, 29))
private val preschoolTerm2020 = FiniteDateRange(LocalDate.of(2020, 8, 13), LocalDate.of(2021, 6, 4))

private fun testPeriod(offsets: Pair<Long, Long?>) =
    FiniteDateRange(
        preschoolTerm2019.start.plusDays(offsets.first),
        offsets.second?.let { preschoolTerm2019.start.plusDays(it) } ?: preschoolTerm2019.end
    )
