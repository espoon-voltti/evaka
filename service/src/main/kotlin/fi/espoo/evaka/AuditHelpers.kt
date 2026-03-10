// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database

data class ApplicationPersonIds(val childId: ChildId, val guardianId: PersonId)

fun Database.Read.getApplicationPersonIds(id: ApplicationId): ApplicationPersonIds =
    createQuery { sql("SELECT child_id, guardian_id FROM application WHERE id = ${bind(id)}") }
        .exactlyOne()

fun Database.Read.getIncomePersonId(id: IncomeId): PersonId =
    createQuery { sql("SELECT person_id FROM income WHERE id = ${bind(id)}") }.exactlyOne()

fun Database.Read.getIncomeStatementPersonId(id: IncomeStatementId): PersonId =
    createQuery { sql("SELECT person_id FROM income_statement WHERE id = ${bind(id)}") }
        .exactlyOne()
