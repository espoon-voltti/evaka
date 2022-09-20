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
        "SELECT child.*, person.preferred_name FROM child JOIN person ON child.id = person.id WHERE child.id = :id"

    return createQuery(sql).bind("id", id).mapTo<Child>().firstOrNull()
}

fun Database.Transaction.createChild(child: Child): Child {
    // language=SQL
    val sql =
        "INSERT INTO child (id, allergies, diet, additionalinfo, medication) VALUES (:id, :allergies, :diet, :additionalInfo, :medication) RETURNING *"

    return createQuery(sql)
        .bind("id", child.id)
        .bind("allergies", child.additionalInformation.allergies)
        .bind("diet", child.additionalInformation.diet)
        .bind("additionalInfo", child.additionalInformation.additionalInfo)
        .bind("medication", child.additionalInformation.medication)
        .mapTo<Child>()
        .first()
}

fun Database.Transaction.upsertChild(child: Child) {
    // language=SQL
    val sql =
        """
        INSERT INTO child (id, allergies, diet) VALUES (:id, :allergies, :diet) 
        ON CONFLICT (id) DO UPDATE SET allergies = :allergies, diet = :diet
        """.trimIndent(
        )

    createUpdate(sql)
        .bind("id", child.id)
        .bind("allergies", child.additionalInformation.allergies)
        .bind("diet", child.additionalInformation.diet)
        .execute()
}

fun Database.Transaction.updateChild(child: Child) {
    // language=SQL
    val sql =
        "UPDATE child SET allergies = :allergies, diet = :diet, additionalinfo = :additionalInfo, medication = :medication WHERE id = :id"

    createUpdate(sql)
        .bind("id", child.id)
        .bind("allergies", child.additionalInformation.allergies)
        .bind("diet", child.additionalInformation.diet)
        .bind("additionalInfo", child.additionalInformation.additionalInfo)
        .bind("medication", child.additionalInformation.medication)
        .execute()
}
