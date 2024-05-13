// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.specialdiet

import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.mapper.PropagateNull

data class SpecialDiet(@PropagateNull val id: Int?, val abbreviation: String)

/**
 * Sets child's special diets to null if there are some children whose special diet is not contained
 * in the new list. Returns the count of affected rows
 */
fun Database.Transaction.resetSpecialDietsNotContainedWithin(
    specialDietList: List<SpecialDiet>
): Int {
    val newSpecialDietIds = specialDietList.map { it.id }
    val affectedRows = execute {
        sql("UPDATE child SET diet_id = null WHERE diet_id != ALL (${bind(newSpecialDietIds)})")
    }
    return affectedRows
}
/** Replaces special_diet list with the given list. Returns count of removed diets */
fun Database.Transaction.setSpecialDiets(specialDietList: List<SpecialDiet>): Int {
    val newSpecialDietIds = specialDietList.map { it.id }
    val deletedDietCount = execute {
        sql("DELETE FROM special_diet WHERE id != ALL (${bind(newSpecialDietIds)})")
    }
    executeBatch(specialDietList) {
        sql(
            """
INSERT INTO special_diet (id, abbreviation)
VALUES (
    ${bind{it.id}},
    ${bind{it.abbreviation}}
)
ON CONFLICT (id) DO UPDATE SET
  abbreviation = excluded.abbreviation
"""
        )
    }
    return deletedDietCount
}

fun Database.Transaction.getSpecialDiets(): List<SpecialDiet> {
    return createQuery { sql("SELECT id, abbreviation FROM special_diet") }.toList<SpecialDiet>()
}
