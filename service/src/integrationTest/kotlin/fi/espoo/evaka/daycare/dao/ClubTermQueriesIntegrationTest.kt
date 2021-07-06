// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.dao

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.getActiveClubTermAt
import fi.espoo.evaka.daycare.getClubTerms
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClubTermQueriesIntegrationTest : PureJdbiTest() {

    @BeforeEach
    internal fun setUp() {
        db.transaction {
            it.execute(
                """INSERT INTO club_term (term, application_period) VALUES
                ('[2020-08-13,2021-06-04]', '[2020-01-08,2020-01-20]'),
                ('[2021-08-11,2022-06-03]', '[2021-01-08,2021-01-20]')
                """.trimIndent()
            )
        }
    }

    @AfterEach
    internal fun tearDown() {
        db.transaction { it.execute("TRUNCATE TABLE club_term") }
    }

    @Test
    fun `get club terms`() {
        val data = db.read { it.getClubTerms() }
        assertEquals(2, data.size)
        assertEquals(
            FiniteDateRange(LocalDate.of(2020, 8, 13), LocalDate.of(2021, 6, 4)),
            data[0].term
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
                    clubTerm?.term
                )
            }
        }

        val dateOutsideTerm = LocalDate.of(2020, 8, 5)
        db.read { assertNull(it.getActiveClubTermAt(dateOutsideTerm)) }
    }
}
