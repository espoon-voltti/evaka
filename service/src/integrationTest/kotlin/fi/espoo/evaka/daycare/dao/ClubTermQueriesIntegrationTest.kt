// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.dao

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.getActiveClubTermAt
import fi.espoo.evaka.daycare.getClubTerms
import fi.espoo.evaka.daycare.insertClubTerm
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ClubTermQueriesIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insertClubTerm(
                FiniteDateRange(LocalDate.of(2020, 8, 13), LocalDate.of(2021, 6, 4)),
                FiniteDateRange(LocalDate.of(2020, 1, 8), LocalDate.of(2020, 1, 20)),
                DateSet.empty(),
            )
            tx.insertClubTerm(
                FiniteDateRange(LocalDate.of(2021, 8, 11), LocalDate.of(2022, 6, 3)),
                FiniteDateRange(LocalDate.of(2021, 1, 8), LocalDate.of(2021, 1, 20)),
                DateSet.empty(),
            )
        }
    }

    @Test
    fun `get club terms`() {
        val data = db.read { it.getClubTerms() }
        assertEquals(2, data.size)
        assertEquals(
            FiniteDateRange(LocalDate.of(2020, 8, 13), LocalDate.of(2021, 6, 4)),
            data[0].term,
        )
    }

    @Test
    fun `get active club term at date`() {
        val dateAtStartOfTerm = LocalDate.of(2020, 8, 13)
        val dateWithinTerm = LocalDate.of(2020, 12, 15)
        val dateAtEndOfTerm = LocalDate.of(2021, 6, 4)
        db.read { dbRead ->
            listOf(dateAtStartOfTerm, dateWithinTerm, dateAtEndOfTerm).forEach { date ->
                val clubTerm = dbRead.getActiveClubTermAt(date)
                assertEquals(
                    FiniteDateRange(LocalDate.of(2020, 8, 13), LocalDate.of(2021, 6, 4)),
                    clubTerm?.term,
                )
            }
        }

        val dateOutsideTerm = LocalDate.of(2020, 8, 5)
        db.read { assertNull(it.getActiveClubTermAt(dateOutsideTerm)) }
    }
}
