// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.process

import fi.espoo.evaka.shared.ArchivedProcessId
import fi.espoo.evaka.shared.db.Database

data class ArchivedProcess(
    val id: ArchivedProcessId,
    val processDefinitionNumber: String,
    val year: Int,
    val number: Int
) {
    val processNumber: String
        get() = "$number/$processDefinitionNumber/$year"
}

fun Database.Transaction.insertProcess(
    processDefinitionNumber: String,
    year: Int
): ArchivedProcess =
    createQuery {
            sql(
                """
    INSERT INTO archived_process (process_definition_number, year, number)
    VALUES (
        ${bind(processDefinitionNumber)}, 
        ${bind(year)},
        coalesce((
            SELECT max(number)
            FROM archived_process
            WHERE process_definition_number = ${bind(processDefinitionNumber)} AND year = ${bind(year)}
        ), 0) + 1
    )
    RETURNING id, process_definition_number, year, number
"""
            )
        }
        .exactlyOne()

fun Database.Read.getProcess(id: ArchivedProcessId): ArchivedProcess? =
    createQuery {
            sql(
                """
    SELECT id, process_definition_number, year, number
    FROM archived_process
    WHERE id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull()
