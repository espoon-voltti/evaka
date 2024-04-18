// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.specialdiet

import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.mapper.PropagateNull

data class SpecialDiet(@PropagateNull val id: Int?, val name: String, val abbreviation: String)

fun Database.Transaction.setSpecialDiets(specialDietList: List<SpecialDiet>) {
    val newSpecialDietIds = specialDietList.map { it.id }
    execute {
        sql("UPDATE child SET diet_id = null WHERE diet_id != ALL (${bind(newSpecialDietIds)})")
    }
    execute { sql("DELETE FROM special_diet") }
    executeBatch(specialDietList) {
        sql(
            """
INSERT INTO special_diet (id, name, abbreviation)
VALUES (
    ${bind{it.id}},
    ${bind{it.name}},
    ${bind{it.abbreviation}}
)
"""
        )
    }
}

fun Database.Transaction.getSpecialDiets(): List<SpecialDiet> {
    return createQuery { sql("SELECT id, name, abbreviation FROM special_diet") }
        .toList<SpecialDiet>()
}
