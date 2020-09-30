// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.assistanceaction.AssistanceActionType
import fi.espoo.evaka.assistanceaction.AssistanceMeasure
import fi.espoo.evaka.assistanceneed.AssistanceBasis
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.preschoolTerm2019
import fi.espoo.evaka.preschoolTerm2020
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevAssistanceAction
import fi.espoo.evaka.shared.dev.DevAssistanceNeed
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestAssistanceAction
import fi.espoo.evaka.shared.dev.insertTestAssistanceNeed
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import org.jdbi.v3.core.Handle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class KoskiIntegrationTest : FullApplicationTest() {
    private lateinit var koskiServer: MockKoskiServer
    private lateinit var koskiTester: KoskiTester

    @BeforeAll
    fun initDependencies() {
        koskiServer = MockKoskiServer.start()
        koskiTester = KoskiTester(
            jdbi,
            KoskiClient(
                jdbi = jdbi,
                env = env,
                baseUrl = "http://localhost:${koskiServer.port}",
                asyncJobRunner = null
            )
        )
    }

    @BeforeEach
    fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
            h.setUnitOids()
        }
        koskiServer.clearData()
    }

    @Test
    fun `won't send same data twice (input cache check)`() {
        insertPlacement()

        fun assertSingleStudyRight() = jdbi.handle { it.getStoredResults() }.let {
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

        fun assertSingleStudyRight() = jdbi.handle { it.getStoredResults() }.let {
            val stored = it.single()
            val sent = koskiServer.getStudyRights().entries.single()
            assertEquals(stored.studyRightOid, sent.key)
            assertEquals(0, sent.value.version)
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertSingleStudyRight()

        jdbi.handle { it.clearKoskiInputCache() }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(2))
        assertSingleStudyRight()
    }

    @Test
    fun `will send again if placement updated`() {
        val placementId = insertPlacement()

        fun assertSingleStudyRight(version: Int) = jdbi.handle { it.getStoredResults() }.let {
            val stored = it.single()
            val sent = koskiServer.getStudyRights().entries.single()
            assertEquals(stored.studyRightOid, sent.key)
            assertEquals(version, sent.value.version)
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertSingleStudyRight(version = 0)

        jdbi.handle {
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
            period = ClosedPeriod(
                LocalDate.of(2018, 8, 1),
                LocalDate.of(2019, 5, 1)
            )
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
            period = ClosedPeriod(
                LocalDate.of(2018, 8, 1),
                LocalDate.of(2019, 4, 30)
            )
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
            period = ClosedPeriod(
                LocalDate.of(2018, 12, 1),
                LocalDate.of(2019, 5, 1)
            )
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
            period = ClosedPeriod(
                august2019,
                preschoolTerm2019.end
            )
        )

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        val studyRight = koskiServer.getStudyRights().values.single().opiskeluoikeus
        assertEquals(OpiskeluoikeudenTyyppiKoodi.PREPARATORY, studyRight.tyyppi.koodiarvo)
        assertEquals(
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start),
                Opiskeluoikeusjakso.valmistunut(preschoolTerm2019.end)
            ),
            studyRight.tila.opiskeluoikeusjaksot
        )
    }

    @Test
    fun `simple preschool placement changes to preparatory`() {
        insertPlacement(
            type = PlacementType.PRESCHOOL,
            period = ClosedPeriod(
                preschoolTerm2019.start,
                preschoolTerm2019.start.plusMonths(4)
            )
        )
        insertPlacement(
            type = PlacementType.PREPARATORY,
            period = ClosedPeriod(
                preschoolTerm2019.start.plusMonths(4).plusDays(1),
                preschoolTerm2019.end
            )
        )

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        val studyRights = koskiServer.getStudyRights().values.let { studyRights ->
            Pair(
                studyRights.single { it.opiskeluoikeus.tyyppi.koodiarvo == OpiskeluoikeudenTyyppiKoodi.PRESCHOOL },
                studyRights.single { it.opiskeluoikeus.tyyppi.koodiarvo == OpiskeluoikeudenTyyppiKoodi.PREPARATORY }
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
        insertPlacement(period = ClosedPeriod(start, firstPlacementEnd), type = PlacementType.PREPARATORY)

        val secondPlacementStart = firstPlacementEnd.plusDays(1)
        insertPlacement(
            period = ClosedPeriod(secondPlacementStart, end),
            daycareId = testDaycare2.id,
            type = PlacementType.PREPARATORY
        )

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        val stored = jdbi.handle { it.getStoredResults() }.let { studyRights ->
            listOf(
                studyRights.single { it.unitId == testDaycare.id },
                studyRights.single { it.unitId == testDaycare2.id }
            )
        }
        val studyRights = koskiServer.getStudyRights().let { studyRights ->
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

        val searchByExistingPerson = jdbi.handle {
            it.getPendingStudyRights(today, KoskiSearchParams(personIds = listOf(testChild_1.id)))
        }
        assertEquals(1, searchByExistingPerson.size)

        val searchByRandomPerson = jdbi.handle {
            it.getPendingStudyRights(today, KoskiSearchParams(personIds = listOf(UUID.randomUUID())))
        }
        assertEquals(0, searchByRandomPerson.size)

        val searchByExistingDaycare = jdbi.handle {
            it.getPendingStudyRights(today, KoskiSearchParams(daycareIds = listOf(testDaycare.id)))
        }
        assertEquals(1, searchByExistingDaycare.size)

        val searchByRandomDaycare = jdbi.handle {
            it.getPendingStudyRights(today, KoskiSearchParams(daycareIds = listOf(UUID.randomUUID())))
        }
        assertEquals(0, searchByRandomDaycare.size)
    }

    @Test
    fun `assistance needs are converted to Koski extra information`() {
        data class TestCase(
            val period: ClosedPeriod,
            val basis: AssistanceBasis
        )
        insertPlacement(testChild_1)
        val testCases = listOf(
            TestCase(testPeriod(0L to 1L), AssistanceBasis.DEVELOPMENTAL_DISABILITY_1),
            TestCase(testPeriod(2L to 3L), AssistanceBasis.DEVELOPMENTAL_DISABILITY_2)
        )
        jdbi.handle { h ->
            testCases.forEach {
                h.insertTestAssistanceNeed(
                    DevAssistanceNeed(
                        updatedBy = testDecisionMaker_1.id,
                        childId = testChild_1.id,
                        startDate = it.period.start,
                        endDate = it.period.end,
                        bases = setOf(it.basis)
                    )
                )
            }
        }

        koskiTester.triggerUploads(today = preschoolTerm2019.end.plusDays(1))
        assertEquals(
            Lisätiedot(
                vammainen = listOf(Aikajakso(alku = testCases[0].period.start, loppu = testCases[0].period.end)),
                vaikeastiVammainen = listOf(
                    Aikajakso(alku = testCases[1].period.start, loppu = testCases[1].period.end)
                ),
                pidennettyOppivelvollisuus = null,
                kuljetusetu = null,
                erityisenTuenPäätökset = null
            ),
            koskiServer.getStudyRights().values.single().opiskeluoikeus.lisätiedot
        )
    }

    @Test
    fun `assistance actions are converted to Koski extra information`() {
        data class TestCase(
            val period: ClosedPeriod,
            val measure: AssistanceMeasure,
            val action: AssistanceActionType? = null
        )
        insertPlacement(testChild_1)
        val testCases = listOf(
            TestCase(testPeriod(0L to 1L), AssistanceMeasure.EXTENDED_COMPULSORY_EDUCATION),
            TestCase(testPeriod(2L to 3L), AssistanceMeasure.TRANSPORT_BENEFIT),
            TestCase(
                testPeriod(4L to 5L),
                AssistanceMeasure.SPECIAL_ASSISTANCE_DECISION,
                AssistanceActionType.SPECIAL_GROUP
            )
        )
        jdbi.handle { h ->
            testCases.forEach {
                h.insertTestAssistanceAction(
                    DevAssistanceAction(
                        updatedBy = testDecisionMaker_1.id,
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
                vammainen = null,
                vaikeastiVammainen = null,
                pidennettyOppivelvollisuus = Aikajakso(
                    alku = testCases[0].period.start,
                    loppu = testCases[0].period.end
                ),
                kuljetusetu = Aikajakso(alku = testCases[1].period.start, loppu = testCases[1].period.end),
                erityisenTuenPäätökset = listOf(
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
            listOf(
                Opiskeluoikeusjakso.läsnä(preschoolTerm2019.start)
            ),
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
    fun `if all placements are outside the term, a study right row is stored but nothing is sent`() {
        insertPlacement(
            testChild_1,
            period = ClosedPeriod(
                start = preschoolTerm2019.start.minusDays(10),
                end = preschoolTerm2019.start.minusDays(2)
            )
        )
        val today = preschoolTerm2019.end.plusDays(1)
        koskiTester.triggerUploads(today)

        val stored = jdbi.handle { it.getStoredResults() }.single()
        assertNull(stored.studyRightOid)
        assertNull(stored.personOid)
        assertEquals("{}", stored.payload)

        assertTrue(koskiServer.getStudyRights().isEmpty())

        // The study right should now be ignored by the diffing mechanism, so it won't be tried again unless input data changes
        assertTrue(jdbi.handle { it.getPendingStudyRights(today) }.isEmpty())
    }

    @Test
    fun `a child can be in preschool for the duration of two terms`() {
        insertPlacement(
            testChild_1,
            period = ClosedPeriod(preschoolTerm2019.start, preschoolTerm2019.end)
        )
        insertPlacement(
            testChild_1,
            period = ClosedPeriod(preschoolTerm2020.start, preschoolTerm2020.end)
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

    private fun insertPlacement(
        child: PersonData.Detailed = testChild_1,
        daycareId: UUID = testDaycare.id,
        period: ClosedPeriod = preschoolTerm2019,
        type: PlacementType = PlacementType.PRESCHOOL
    ): UUID = jdbi.handle {
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
}

private fun Handle.clearKoskiInputCache() = createUpdate("UPDATE koski_study_right SET input_data = NULL").execute()

private fun testPeriod(offsets: Pair<Long, Long?>) = ClosedPeriod(
    preschoolTerm2019.start.plusDays(offsets.first),
    offsets.second?.let { preschoolTerm2019.start.plusDays(it) } ?: preschoolTerm2019.end
)
