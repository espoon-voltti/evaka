// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.daycare.controllers.Child
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database

fun Database.Read.getChild(id: ChildId): Child? {
    // language=SQL
    val sql =
        """
SELECT child.*, person.preferred_name, special_diet.id as special_diet_id, special_diet.name as special_diet_name, special_diet.abbreviation as special_diet_abbreviation
FROM child JOIN person ON child.id = person.id LEFT JOIN special_diet on child.diet_id = special_diet.id
WHERE child.id = :id
"""

    @Suppress("DEPRECATION") return createQuery(sql).bind("id", id).exactlyOneOrNull<Child>()
}

fun Database.Transaction.createChild(child: Child): Child {
    // language=SQL
    val sql =
        "INSERT INTO child (id, allergies, diet, additionalinfo, medication, language_at_home, language_at_home_details, diet_id) VALUES (:id, :allergies, :diet, :additionalInfo, :medication, :languageAtHome, :languageAtHomeDetails, :dietId) RETURNING *"

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("id", child.id)
        .bind("allergies", child.additionalInformation.allergies)
        .bind("diet", child.additionalInformation.diet)
        .bind("additionalInfo", child.additionalInformation.additionalInfo)
        .bind("medication", child.additionalInformation.medication)
        .bind("languageAtHome", child.additionalInformation.languageAtHome)
        .bind("languageAtHomeDetails", child.additionalInformation.languageAtHomeDetails)
        .bind("dietId", child.additionalInformation.specialDiet?.id)
        .exactlyOne<Child>()
}

fun Database.Transaction.upsertChild(child: Child) {
    // language=SQL
    val sql =
        """
        INSERT INTO child (id, allergies, diet) VALUES (:id, :allergies, :diet) 
        ON CONFLICT (id) DO UPDATE SET allergies = :allergies, diet = :diet
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    createUpdate(sql)
        .bind("id", child.id)
        .bind("allergies", child.additionalInformation.allergies)
        .bind("diet", child.additionalInformation.diet)
        .execute()
}

fun Database.Transaction.updateChild(child: Child) {
    // language=SQL
    val sql =
        "UPDATE child SET allergies = :allergies, diet = :diet, additionalinfo = :additionalInfo, medication = :medication, language_at_home = :languageAtHome, language_at_home_details = :languageAtHomeDetails, diet_id = :dietId WHERE id = :id"

    @Suppress("DEPRECATION")
    createUpdate(sql)
        .bind("id", child.id)
        .bind("allergies", child.additionalInformation.allergies)
        .bind("diet", child.additionalInformation.diet)
        .bind("additionalInfo", child.additionalInformation.additionalInfo)
        .bind("medication", child.additionalInformation.medication)
        .bind("languageAtHome", child.additionalInformation.languageAtHome)
        .bind("languageAtHomeDetails", child.additionalInformation.languageAtHomeDetails)
        .bind(
            "dietId",
            if (child.additionalInformation.specialDiet != null)
                child.additionalInformation.specialDiet.id
            else null
        )
        .execute()
}
