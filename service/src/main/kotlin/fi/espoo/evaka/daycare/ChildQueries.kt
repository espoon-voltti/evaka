// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.daycare.controllers.Child
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database

fun Database.Read.getChild(id: ChildId): Child? {
    val child =
        createQuery {
                sql(
                    """
SELECT child.*, person.preferred_name, special_diet.id as special_diet_id, special_diet.abbreviation as special_diet_abbreviation, meal_texture.name AS meal_texture_name, (
  SELECT jsonb_agg(jsonb_build_object('dietId', diet_id, 'fieldId', field_id, 'value', value))
  FROM nekku_special_diet_choices
  WHERE child_id = ${bind(id)}
) AS nekku_special_diet_choices
FROM child JOIN person ON child.id = person.id LEFT JOIN special_diet on child.diet_id = special_diet.id LEFT JOIN meal_texture on child.meal_texture_id = meal_texture.id
WHERE child.id = ${bind(id)}
"""
                )
            }
            .exactlyOneOrNull<Child>()
    return child
}

fun Database.Transaction.createChild(child: Child) {
    execute {
        sql(
            """
INSERT INTO child (id, allergies, diet, additionalinfo, medication, language_at_home, language_at_home_details, diet_id, meal_texture_id, nekku_diet, nekku_eats_breakfast) VALUES (
    ${bind(child.id)},
    ${bind(child.additionalInformation.allergies)},
    ${bind(child.additionalInformation.diet)},
    ${bind(child.additionalInformation.additionalInfo)},
    ${bind(child.additionalInformation.medication)},
    ${bind(child.additionalInformation.languageAtHome)},
    ${bind(child.additionalInformation.languageAtHomeDetails)},
    ${bind(child.additionalInformation.specialDiet?.id)},
    ${bind(child.additionalInformation.mealTexture?.id)},
    ${bind(child.additionalInformation.nekkuDiet)},
    ${bind(child.additionalInformation.nekkuEatsBreakfast)}
)
"""
        )
    }
    resetNekkuSpecialDietChoices(child)
}

fun Database.Transaction.upsertChild(child: Child) {
    execute {
        sql(
            """
INSERT INTO child (id, allergies, diet) VALUES (${bind(child.id)}, ${bind(child.additionalInformation.allergies)}, ${bind(child.additionalInformation.diet)}) 
ON CONFLICT (id) DO UPDATE SET allergies = ${bind(child.additionalInformation.allergies)}, diet = ${bind(child.additionalInformation.diet)}
"""
        )
    }
}

fun Database.Transaction.updateChild(child: Child) {
    execute {
        sql(
            """
UPDATE child SET
    allergies = ${bind(child.additionalInformation.allergies)}, 
    diet = ${bind(child.additionalInformation.diet)}, 
    additionalinfo = ${bind(child.additionalInformation.additionalInfo)}, 
    medication = ${bind(child.additionalInformation.medication)}, 
    language_at_home = ${bind(child.additionalInformation.languageAtHome)}, 
    language_at_home_details = ${bind(child.additionalInformation.languageAtHomeDetails)}, 
    diet_id = ${bind(child.additionalInformation.specialDiet?.id)}, 
    meal_texture_id = ${bind(child.additionalInformation.mealTexture?.id)},
    nekku_diet = ${bind(child.additionalInformation.nekkuDiet)},
    nekku_eats_breakfast = ${bind(child.additionalInformation.nekkuEatsBreakfast)}
WHERE id = ${bind(child.id)}
"""
        )
    }
    resetNekkuSpecialDietChoices(child)
}

fun Database.Transaction.resetNekkuSpecialDietChoices(child: Child) {
    execute {
        sql(
            """
                DELETE FROM nekku_special_diet_choices WHERE child_id = ${bind(child.id)}
            """
        )
    }
    executeBatch(child.additionalInformation.nekkuSpecialDietChoices) {
        sql(
            """
                INSERT INTO nekku_special_diet_choices (child_id, diet_id, field_id, value)
                VALUES (
                    ${bind(child.id)},
                    ${bind{it.dietId}},
                    ${bind{it.fieldId}},
                    ${bind{it.value}}
                )
            """
        )
    }
}
