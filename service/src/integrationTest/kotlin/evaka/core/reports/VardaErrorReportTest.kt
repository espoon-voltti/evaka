// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.FullApplicationTest
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.varda.freezeVardaSync
import evaka.core.varda.setVardaUpdateError
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VardaErrorReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var vardaErrorReport: VardaErrorReport

    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0))
    private val clock = MockEvakaClock(now)

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val erroredChild = DevPerson()
    private val frozenChild = DevPerson()

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(admin)
            listOf(erroredChild, frozenChild).forEach { child ->
                tx.insert(child, DevPersonType.CHILD)
                tx.execute {
                    sql(
                        "INSERT INTO varda_state (child_id, state) VALUES (${bind(child.id)}, NULL)"
                    )
                }
                tx.setVardaUpdateError(child.id, now, "boom")
            }
            tx.freezeVardaSync(listOf(frozenChild.id), now)
        }
    }

    @Test
    fun `a child whose Varda data has been removed is left out of the error report`() {
        val rows = vardaErrorReport.getVardaChildErrorsReport(dbInstance(), admin.user, clock)

        assertEquals(listOf(erroredChild.id), rows.map { it.childId })
    }
}
