// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.reports

import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.specialdiet.MealTexture
import fi.espoo.evaka.specialdiet.SpecialDiet
import java.time.LocalDate

fun Database.Read.childPlacementsForDay(
    daycareId: DaycareId,
    date: LocalDate,
): Map<ChildId, PlacementType> =
    createQuery {
            sql(
                """
SELECT child_id, placement_type 
FROM realized_placement_one(${bind(date)}) 
WHERE unit_id = ${bind(daycareId)}
                    """
            )
        }
        .toMap { columnPair("child_id", "placement_type") }

fun Database.Read.specialDietsForChildren(childIds: Set<ChildId>): Map<ChildId, SpecialDiet> =
    createQuery {
            sql(
                """
SELECT child.id as child_id, special_diet.*
FROM child JOIN special_diet ON child.diet_id = special_diet.id
WHERE child.id = ANY (${bind(childIds)})
"""
            )
        }
        .toMap { column<ChildId>("child_id") to row<SpecialDiet>() }

fun Database.Read.mealTexturesForChildren(childIds: Set<ChildId>): Map<ChildId, MealTexture> =
    createQuery {
            sql(
                """
SELECT child.id as child_id, meal_texture.*
FROM child JOIN meal_texture ON child.meal_texture_id = meal_texture.id
WHERE child.id = ANY (${bind(childIds)})
"""
            )
        }
        .toMap { column<ChildId>("child_id") to row<MealTexture>() }
