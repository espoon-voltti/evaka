// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.daycare.controllers.Child
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database

fun Database.Read.getChild(id: ChildId): Child? =
    createQuery {
            sql(
                """
SELECT child.*, person.preferred_name, special_diet.id as special_diet_id, special_diet.abbreviation as special_diet_abbreviation, meal_texture.name AS meal_texture_name
FROM child JOIN person ON child.id = person.id LEFT JOIN special_diet on child.diet_id = special_diet.id LEFT JOIN meal_texture on child.meal_texture_id = meal_texture.id
WHERE child.id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull<Child>()

fun Database.Transaction.createChild(child: Child) {
    execute {
        sql(
            """
INSERT INTO child (id, allergies, diet, additionalinfo, medication, language_at_home, language_at_home_details, diet_id, meal_texture_id) VALUES (
    ${bind(child.id)},
    ${bind(child.additionalInformation.allergies)},
    ${bind(child.additionalInformation.diet)},
    ${bind(child.additionalInformation.additionalInfo)},
    ${bind(child.additionalInformation.medication)},
    ${bind(child.additionalInformation.languageAtHome)},
    ${bind(child.additionalInformation.languageAtHomeDetails)},
    ${bind(child.additionalInformation.specialDiet?.id)},
    ${bind(child.additionalInformation.mealTexture?.id)}
)
"""
        )
    }
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
    meal_texture_id = ${bind(child.additionalInformation.mealTexture?.id)}
WHERE id = ${bind(child.id)}
"""
        )
    }
}
