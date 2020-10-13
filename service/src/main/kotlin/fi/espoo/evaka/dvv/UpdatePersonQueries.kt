// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.dvv

import org.jdbi.v3.core.Handle
import java.time.LocalDate
import java.util.UUID

fun setPersonDateOfDeath(h: Handle, id: UUID, date: LocalDate) {
    h.createUpdate(
        """
        UPDATE person SET date_of_death = :date WHERE id = :id
        """.trimIndent()
    ).bind("date", date).bind("id", id).execute()
}
