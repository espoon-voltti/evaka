// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistance

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.dev.DevAssistanceFactor
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AssistanceQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {

    @Test
    fun `endAssistanceFactorsWhichBelongToPastPlacements sets end date from placement`() {
        val placementStart = LocalDate.of(2024, 1, 1)
        val placementEnd = LocalDate.of(2024, 1, 31)
        val today = placementEnd.plusDays(1)
        val assistanceFactorStart = LocalDate.of(2024, 1, 2)
        val assistanceFactorEnd = LocalDate.of(2024, 2, 29)
        val childId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                val unitId = tx.insert(DevDaycare(areaId = areaId))
                val childId =
                    tx.insert(DevPerson(), DevPersonType.CHILD).let { tx.insert(DevChild(id = it)) }
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = placementStart,
                        endDate = placementEnd
                    )
                )
                tx.insert(
                    DevAssistanceFactor(
                        childId = childId,
                        validDuring = FiniteDateRange(assistanceFactorStart, assistanceFactorEnd)
                    )
                )
                childId
            }

        db.transaction { it.endAssistanceFactorsWhichBelongToPastPlacements(today) }

        assertThat(db.read { it.getAssistanceFactors(child = childId) })
            .extracting<FiniteDateRange> { it.validDuring }
            .containsExactlyInAnyOrder(FiniteDateRange(assistanceFactorStart, placementEnd))
    }

    @Test
    fun `endAssistanceFactorsWhichBelongToPastPlacements works with multiple placements`() {
        val placement1Start = LocalDate.of(2024, 1, 1)
        val placement1End = LocalDate.of(2024, 1, 31)
        val placement2Start = LocalDate.of(2024, 2, 1)
        val placement2End = LocalDate.of(2024, 2, 29)
        val today = placement2End.plusDays(1)
        val assistanceFactorStart = LocalDate.of(2024, 1, 2)
        val assistanceFactorEnd = LocalDate.of(2024, 2, 29)
        val childId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                val unitId = tx.insert(DevDaycare(areaId = areaId))
                val childId =
                    tx.insert(DevPerson(), DevPersonType.CHILD).let { tx.insert(DevChild(id = it)) }
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = placement1Start,
                        endDate = placement1End
                    )
                )
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = placement2Start,
                        endDate = placement2End
                    )
                )
                tx.insert(
                    DevAssistanceFactor(
                        childId = childId,
                        validDuring = FiniteDateRange(assistanceFactorStart, assistanceFactorEnd)
                    )
                )
                childId
            }

        db.transaction { it.endAssistanceFactorsWhichBelongToPastPlacements(today) }

        assertThat(db.read { it.getAssistanceFactors(child = childId) })
            .extracting<FiniteDateRange> { it.validDuring }
            .containsExactlyInAnyOrder(FiniteDateRange(assistanceFactorStart, placement2End))
    }

    @Test
    fun `endAssistanceFactorsWhichBelongToPastPlacements works with multiple assistance factors`() {
        val placementStart = LocalDate.of(2024, 1, 1)
        val placementEnd = LocalDate.of(2024, 2, 15)
        val today = placementEnd.plusDays(1)
        val assistanceFactor1Start = LocalDate.of(2024, 1, 1)
        val assistanceFactor1End = LocalDate.of(2024, 1, 31)
        val assistanceFactor2Start = LocalDate.of(2024, 2, 1)
        val assistanceFactor2End = LocalDate.of(2024, 2, 29)
        val childId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                val unitId = tx.insert(DevDaycare(areaId = areaId))
                val childId =
                    tx.insert(DevPerson(), DevPersonType.CHILD).let { tx.insert(DevChild(id = it)) }
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = placementStart,
                        endDate = placementEnd
                    )
                )
                tx.insert(
                    DevAssistanceFactor(
                        childId = childId,
                        validDuring = FiniteDateRange(assistanceFactor1Start, assistanceFactor1End)
                    )
                )
                tx.insert(
                    DevAssistanceFactor(
                        childId = childId,
                        validDuring = FiniteDateRange(assistanceFactor2Start, assistanceFactor2End)
                    )
                )
                childId
            }

        db.transaction { it.endAssistanceFactorsWhichBelongToPastPlacements(today) }

        assertThat(db.read { it.getAssistanceFactors(child = childId) })
            .extracting<FiniteDateRange> { it.validDuring }
            .containsExactlyInAnyOrder(
                FiniteDateRange(assistanceFactor1Start, assistanceFactor1End),
                FiniteDateRange(assistanceFactor2Start, placementEnd)
            )
    }

    @Test
    fun `endAssistanceFactorsWhichBelongToPastPlacements works with placement before assistance factor`() {
        val placementStart = LocalDate.of(2024, 1, 1)
        val placementEnd = LocalDate.of(2024, 1, 31)
        val today = placementEnd.plusDays(1)
        val assistanceFactorStart = LocalDate.of(2024, 2, 1)
        val assistanceFactorEnd = LocalDate.of(2024, 2, 29)
        val childId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                val unitId = tx.insert(DevDaycare(areaId = areaId))
                val childId =
                    tx.insert(DevPerson(), DevPersonType.CHILD).let { tx.insert(DevChild(id = it)) }
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = placementStart,
                        endDate = placementEnd
                    )
                )
                tx.insert(
                    DevAssistanceFactor(
                        childId = childId,
                        validDuring = FiniteDateRange(assistanceFactorStart, assistanceFactorEnd)
                    )
                )
                childId
            }

        db.transaction { it.endAssistanceFactorsWhichBelongToPastPlacements(today) }

        assertThat(db.read { it.getAssistanceFactors(child = childId) })
            .extracting<FiniteDateRange> { it.validDuring }
            .containsExactlyInAnyOrder(FiniteDateRange(assistanceFactorStart, assistanceFactorEnd))
    }

    @Test
    fun `endAssistanceFactorsWhichBelongToPastPlacements works with multiple children`() {
        val child1PlacementStart = LocalDate.of(2024, 1, 1)
        val child1PlacementEnd = LocalDate.of(2024, 1, 31)
        val child2PlacementStart = LocalDate.of(2024, 1, 1)
        val child2PlacementEnd = LocalDate.of(2024, 2, 1)
        val today = child2PlacementEnd.plusDays(1)
        val assistanceFactorStart = LocalDate.of(2024, 1, 2)
        val assistanceFactorEnd = LocalDate.of(2024, 2, 29)
        val unitId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                tx.insert(DevDaycare(areaId = areaId))
            }
        val child1Id =
            db.transaction { tx ->
                val childId =
                    tx.insert(DevPerson(), DevPersonType.CHILD).let { tx.insert(DevChild(id = it)) }
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = child1PlacementStart,
                        endDate = child1PlacementEnd
                    )
                )
                tx.insert(
                    DevAssistanceFactor(
                        childId = childId,
                        validDuring = FiniteDateRange(assistanceFactorStart, assistanceFactorEnd)
                    )
                )
                childId
            }
        val child2Id =
            db.transaction { tx ->
                val childId =
                    tx.insert(DevPerson(), DevPersonType.CHILD).let { tx.insert(DevChild(id = it)) }
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = child2PlacementStart,
                        endDate = child2PlacementEnd
                    )
                )
                tx.insert(
                    DevAssistanceFactor(
                        childId = childId,
                        validDuring = FiniteDateRange(assistanceFactorStart, assistanceFactorEnd)
                    )
                )
                childId
            }

        db.transaction { it.endAssistanceFactorsWhichBelongToPastPlacements(today) }

        assertThat(db.read { it.getAssistanceFactors(child = child1Id) })
            .extracting<FiniteDateRange> { it.validDuring }
            .containsExactlyInAnyOrder(FiniteDateRange(assistanceFactorStart, child1PlacementEnd))
        assertThat(db.read { it.getAssistanceFactors(child = child2Id) })
            .extracting<FiniteDateRange> { it.validDuring }
            .containsExactlyInAnyOrder(FiniteDateRange(assistanceFactorStart, child2PlacementEnd))
    }
}
