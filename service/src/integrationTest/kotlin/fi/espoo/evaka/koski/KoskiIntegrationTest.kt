// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.KoskiEnv
import fi.espoo.evaka.OphEnv
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.assistance.OtherAssistanceMeasureType
import fi.espoo.evaka.assistance.PreschoolAssistanceLevel
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevOtherAssistanceMeasure
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevPreschoolAssistance
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChildDuplicateOf
import fi.espoo.evaka.testChildDuplicated
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_7
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class KoskiIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var koskiEndpoint: MockKoskiEndpoint
    private lateinit var koskiTester: KoskiTester

    @BeforeAll
    fun initDependencies() {
        koskiTester =
            KoskiTester(
                db,
                KoskiClient(
                    KoskiEnv.fromEnvironment(env)
                        .copy(url = "http://localhost:$httpPort/public/mock-koski"),
                    OphEnv.fromEnvironment(env),
                    asyncJobRunner = null
                )
            )
    }

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            listOf(testChildDuplicated, testChildDuplicateOf, testChild_1, testChild_7).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
            tx.setUnitOids()
        }
        koskiEndpoint.clearData()
    }

    @Test
    fun `won't send same data twice (input cache check)`() {
        insertPlacement()

        fun assertSingleStudyRight() =
            db.read { it.getStoredResults() }
                .let {
                    val stored = it.single()
                    val sent = koskiEndpoint.getStudyRights().entries.single()
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
                    val sent = koskiEndpoint.getStudyRights().entries.single()
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
    fun `4xx errors don't fail the upload, and the study right remains in pending state until upload succeeds`() {
        val daycare = testDaycare
        val today = preschoolTerm2019.end.plusDays(1)

        fun countPendingStudyRights() = db.read { it.getPendingStudyRights(today) }.size

        db.transaction { it.setUnitOid(daycare.id, MockKoskiEndpoint.UNIT_OID_THAT_TRIGGERS_400) }
        insertPlacement(daycareId = daycare.id)

        koskiTester.triggerUploads(today)
        assertEquals(1, countPendingStudyRights())

        db.transaction { it.setUnitOids() }
        koskiTester.triggerUploads(today)
        assertEquals(0, countPendingStudyRights())
    }

    @Test
    fun `voiding considers status 404 as success`() {
        val placementId = insertPlacement()

        fun countActiveStudyRights() =
            db.read {
                @Suppress("DEPRECATION")
                it.createQuery("SELECT count(*) FROM koski_study_right WHERE void_date IS NULL")
                    .exactlyOne<Long>()
            }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertEquals(1, koskiEndpoint.getStudyRights().values.size)
        assertEquals(1, countActiveStudyRights())

        koskiEndpoint.clearData()

        db.transaction {
            it.execute { sql("DELETE FROM placement WHERE id = ${bind(placementId)}") }
        }
        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(2))
        assertEquals(0, koskiEndpoint.getStudyRights().values.size)
        assertEquals(0, countActiveStudyRights())
    }

    @Test
    fun `will send again if placement updated`() {
        val placementId = insertPlacement()

        fun assertSingleStudyRight(version: Int) =
            db.read { it.getStoredResults() }
                .let {
                    val stored = it.single()
                    val sent = koskiEndpoint.getStudyRights().entries.single()
                    assertEquals(stored.studyRightOid, sent.key)
                    assertEquals(version, sent.value.version)
                }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertSingleStudyRight(version = 0)

        db.transaction {
            @Suppress("DEPRECATION")
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
            koskiEndpoint.getStudyRights().values.single().opiskeluoikeus.tila.opiskeluoikeusjaksot
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
            koskiEndpoint.getStudyRights().values.single().opiskeluoikeus.tila.opiskeluoikeusjaksot
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
            koskiEndpoint.getStudyRights().values.single().opiskeluoikeus.tila.opiskeluoikeusjaksot
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
        val studyRight = koskiEndpoint.getStudyRights().values.single().opiskeluoikeus
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
            koskiEndpoint.getStudyRights().values.let { studyRights ->
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
            koskiEndpoint.getStudyRights().let { studyRights ->
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
    fun `preschool assistance info is converted to Koski extra information`() {
        data class TestCase(
            val period: FiniteDateRange,
            val level: PreschoolAssistanceLevel,
        )
        insertPlacement(testChild_1)
        val intensifiedSupport =
            TestCase(testPeriod(0L to 1L), PreschoolAssistanceLevel.INTENSIFIED_SUPPORT)
        val specialSupport =
            TestCase(testPeriod(2L to 3L), PreschoolAssistanceLevel.SPECIAL_SUPPORT)
        val level1 =
            TestCase(
                testPeriod(4L to 5L),
                PreschoolAssistanceLevel.SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1,
            )
        val level2 =
            TestCase(
                testPeriod(6L to 7L),
                PreschoolAssistanceLevel.SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2
            )
        db.transaction { tx ->
            listOf(intensifiedSupport, specialSupport, level1, level2).forEach {
                tx.insert(
                    DevPreschoolAssistance(
                        modifiedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                        childId = testChild_1.id,
                        validDuring = it.period,
                        level = it.level,
                    )
                )
            }
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertEquals(
            Lisätiedot(
                vammainen =
                    listOf(
                        Aikajakso.from(level1.period),
                    ),
                vaikeastiVammainen =
                    listOf(
                        Aikajakso.from(level2.period),
                    ),
                pidennettyOppivelvollisuus = Aikajakso.from(level1.period.merge(level2.period)),
                kuljetusetu = null,
                erityisenTuenPäätökset =
                    listOf(
                        ErityisenTuenPäätös.from(
                            level1.period.merge(level2.period).merge(specialSupport.period)
                        )
                    )
            ),
            koskiEndpoint.getStudyRights().values.single().opiskeluoikeus.lisätiedot
        )
    }

    @Test
    fun `adjacent transport benefit ranges are sent as one joined range`() {
        insertPlacement(testChild_1)
        val otherAssistanceMeasures =
            listOf(
                testPeriod(1L to 1L),
                testPeriod(2L to 2L),
                testPeriod(3L to 4L),
                testPeriod(6L to 7L)
            )
        db.transaction { tx ->
            otherAssistanceMeasures.forEach {
                tx.insert(
                    DevOtherAssistanceMeasure(
                        modifiedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                        childId = testChild_1.id,
                        validDuring = it,
                        type = OtherAssistanceMeasureType.TRANSPORT_BENEFIT
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
            koskiEndpoint.getStudyRights().values.single().opiskeluoikeus.lisätiedot
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
            koskiEndpoint.getStudyRights().values.single().opiskeluoikeus.tila.opiskeluoikeusjaksot
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
            koskiEndpoint.getStudyRights().values.single().opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `placements starting in the future are not included in Koski study right processing`() {
        insertPlacement(testChild_1, period = testPeriod((0L to 2L)))
        insertPlacement(testChild_1, period = testPeriod((4L to null)))

        fun assertStudyRight(dateRanges: List<Opiskeluoikeusjakso>, qualified: Boolean) {
            val opiskeluoikeus = koskiEndpoint.getStudyRights().values.single().opiskeluoikeus
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

        val opiskeluoikeus = koskiEndpoint.getStudyRights().values.single().opiskeluoikeus
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

        val oldOid = koskiEndpoint.getStudyRights().keys.single()
        db.transaction {
            @Suppress("DEPRECATION") it.createUpdate("DELETE FROM placement").execute()
        }
        koskiTester.triggerUploads(today.plusDays(1))
        assertTrue(koskiEndpoint.getStudyRights().isEmpty())

        db.transaction { it.clearKoskiInputCache() }
        koskiTester.triggerUploads(today.plusDays(2))
        assertTrue(koskiEndpoint.getStudyRights().isEmpty())

        insertPlacement()
        koskiTester.triggerUploads(today.plusDays(3))

        val newOid = koskiEndpoint.getStudyRights().keys.single()
        assertNotEquals(oldOid, newOid)
    }

    @Test
    fun `a daycare with purchased provider type is marked as such in study rights`() {
        val daycareId =
            db.transaction {
                it.insert(DevDaycare(areaId = testArea.id, providerType = ProviderType.PURCHASED))
            }
        insertPlacement(daycareId = daycareId)

        val today = preschoolTerm2019.end.plusDays(1)
        koskiTester.triggerUploads(today)

        val opiskeluoikeus = koskiEndpoint.getStudyRights().values.single().opiskeluoikeus
        assertEquals(
            Järjestämismuoto(JärjestämismuotoKoodi.PURCHASED),
            opiskeluoikeus.järjestämismuoto
        )
    }

    @Test
    fun `a daycare with private provider type is marked as purchased in study rights`() {
        val daycareId =
            db.transaction {
                it.insert(DevDaycare(areaId = testArea.id, providerType = ProviderType.PRIVATE))
            }
        insertPlacement(daycareId = daycareId)

        val today = preschoolTerm2019.end.plusDays(1)
        koskiTester.triggerUploads(today)

        val opiskeluoikeus = koskiEndpoint.getStudyRights().values.single().opiskeluoikeus
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

        val opiskeluoikeus = koskiEndpoint.getStudyRights().values.single().opiskeluoikeus
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
        insertPlacement(period = preschoolTerm2019, type = PlacementType.PREPARATORY)

        val today = preschoolTerm2019.end.plusDays(1)
        koskiTester.triggerUploads(today)

        insertAbsences(
            testChild_1.id,
            AbsenceType.UNKNOWN_ABSENCE,
            FiniteDateRange(preschoolTerm2019.start.plusDays(1), preschoolTerm2019.end.minusDays(1))
        )

        val opiskeluoikeus = koskiEndpoint.getStudyRights().values.single().opiskeluoikeus
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

        val opiskeluoikeus = koskiEndpoint.getStudyRights().values.single().opiskeluoikeus
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

        val opiskeluoikeus = koskiEndpoint.getStudyRights().values.single().opiskeluoikeus
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
    fun `preparatory is NOT considered resigned if sick leave periods longer than week make up more than 30 days in total`() {
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
        insertAbsences(testChild_1.id, AbsenceType.SICKLEAVE, *absences.toTypedArray())

        val today = preschoolTerm2020.end.plusDays(1)
        koskiTester.triggerUploads(today)

        val opiskeluoikeus = koskiEndpoint.getStudyRights().values.single().opiskeluoikeus
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2020.start),
                Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(absences[0].start),
                Opiskeluoikeusjakso.läsnä(absences[0].end.plusDays(1)),
                Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(absences[1].start),
                Opiskeluoikeusjakso.läsnä(absences[1].end.plusDays(1)),
                Opiskeluoikeusjakso.valmistunut(preschoolTerm2020.end)
            ),
            opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
        val suoritus = opiskeluoikeus.suoritukset.single()
        assertNotNull(suoritus.vahvistus)
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

        val opiskeluoikeus = koskiEndpoint.getStudyRights().values.single().opiskeluoikeus
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
            @Suppress("DEPRECATION")
            it.createUpdate("UPDATE person SET oph_person_oid = :oid WHERE id = :id")
                .bind("id", testChild_7.id)
                .bind("oid", personOid)
                .execute()
        }
        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))

        val studyRights = koskiEndpoint.getStudyRights()
        assertEquals(1, studyRights.size)
        val studyRightOid = studyRights.values.first().opiskeluoikeus.oid
        assertEquals(setOf(studyRightOid), koskiEndpoint.getPersonStudyRights(personOid))
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
            koskiEndpoint.getStudyRights().values.sortedBy {
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
                        .copy(url = "http://localhost:$httpPort/public/mock-koski"),
                    OphEnv.fromEnvironment(env).copy(municipalityCode = "001"),
                    asyncJobRunner = null
                )
            )

        testerWithMunicipalityCode.triggerUploads(today = preschoolTerm2019.end.plusDays(1))

        assertEquals(
            "001",
            koskiEndpoint
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

    @Test
    fun `will send duplicate child's preschool daycare placement, but not the original child's`() {
        val placementPeriodOfOriginal = preschoolTerm2019
        insertPlacement(
            child = testChildDuplicated,
            period = placementPeriodOfOriginal,
            type = PlacementType.PRESCHOOL_DAYCARE
        )
        koskiTester.triggerUploads(today = placementPeriodOfOriginal.end.plusDays(1))

        val duplicatedChildsStudyRights = koskiEndpoint.getStudyRights()

        assertEquals(0, duplicatedChildsStudyRights.size)

        val placementPeriodOfDuplicate =
            FiniteDateRange(preschoolTerm2019.start.plusDays(1), preschoolTerm2019.end.minusDays(1))

        insertPlacement(
            child = testChildDuplicateOf,
            period = placementPeriodOfDuplicate,
            type = PlacementType.PRESCHOOL_DAYCARE
        )
        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))

        val allStudyRights = koskiEndpoint.getStudyRights()
        assertEquals(1, allStudyRights.size)

        val onlyStudyRight = allStudyRights.values.first()
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(placementPeriodOfDuplicate.start),
                Opiskeluoikeusjakso.valmistunut(placementPeriodOfDuplicate.end)
            ),
            onlyStudyRight.opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `preschool club placement is delivered`() {
        insertPlacement(
            child = testChild_1,
            daycareId = testDaycare.id,
            period = preschoolTerm2019,
            type = PlacementType.PRESCHOOL_CLUB
        )
        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))

        val studyRights = koskiEndpoint.getStudyRights()
        assertEquals(1, studyRights.size)
        val studyRight = studyRights.values.first()

        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start),
                Opiskeluoikeusjakso.valmistunut(preschoolTerm2019.end)
            ),
            studyRight.opiskeluoikeus.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `moving from last preparatory placement to preschool qualifies the preparatory study right`() {
        val daycare3 =
            db.transaction {
                it.insert(
                    DevDaycare(
                        areaId = testArea.id,
                        uploadToKoski = true,
                        ophUnitOid = "1.2.246.562.10.3333333333"
                    )
                )
            }
        val daycare4 =
            db.transaction {
                it.insert(
                    DevDaycare(
                        areaId = testArea.id,
                        uploadToKoski = true,
                        ophUnitOid = "1.2.246.562.10.4444444444"
                    )
                )
            }
        val placements =
            listOf(
                testPeriod(0L to 9L) to Pair(testDaycare.id, PlacementType.PREPARATORY),
                testPeriod(10L to 19L) to Pair(testDaycare2.id, PlacementType.PRESCHOOL),
                testPeriod(20L to 29L) to Pair(daycare3, PlacementType.PREPARATORY),
                testPeriod(30L to null) to Pair(daycare4, PlacementType.PRESCHOOL),
            )
        placements.forEach { (dateRange, placement) ->
            val (unit, type) = placement
            insertPlacement(child = testChild_1, daycareId = unit, period = dateRange, type)
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))

        val terminalStates =
            koskiEndpoint
                .getStudyRights()
                .map { it.value.opiskeluoikeus.tila.opiskeluoikeusjaksot.last() }
                .sortedBy { it.alku }

        assertEquals(
            listOf(
                Opiskeluoikeusjakso.eronnut(placements[0].first.end),
                Opiskeluoikeusjakso.eronnut(placements[1].first.end),
                Opiskeluoikeusjakso.valmistunut(placements[2].first.end),
                Opiskeluoikeusjakso.valmistunut(placements[3].first.end)
            ),
            terminalStates
        )
    }

    private fun insertPlacement(
        child: DevPerson = testChild_1,
        daycareId: DaycareId = testDaycare.id,
        period: FiniteDateRange = preschoolTerm2019,
        type: PlacementType = PlacementType.PRESCHOOL
    ): PlacementId =
        db.transaction {
            it.insert(
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
                    tx.insert(
                        DevAbsence(
                            childId = childId,
                            date = date,
                            absenceType = absenceType,
                            absenceCategory = AbsenceCategory.NONBILLABLE
                        )
                    )
                }
            }
        }
}

private fun Database.Transaction.clearKoskiInputCache() =
    @Suppress("DEPRECATION")
    createUpdate(
            "UPDATE koski_study_right SET preschool_input_data = NULL, preparatory_input_data = NULL"
        )
        .execute()

private val preschoolTerm2019 = FiniteDateRange(LocalDate.of(2019, 8, 8), LocalDate.of(2020, 5, 29))
private val preschoolTerm2020 = FiniteDateRange(LocalDate.of(2020, 8, 13), LocalDate.of(2021, 6, 4))

private fun testPeriod(offsets: Pair<Long, Long?>) =
    FiniteDateRange(
        preschoolTerm2019.start.plusDays(offsets.first),
        offsets.second?.let { preschoolTerm2019.start.plusDays(it) } ?: preschoolTerm2019.end
    )
