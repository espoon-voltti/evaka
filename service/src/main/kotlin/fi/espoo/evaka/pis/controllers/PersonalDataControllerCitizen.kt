//  SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
//  SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.utils.EMAIL_PATTERN
import fi.espoo.evaka.shared.utils.PHONE_PATTERN
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/personal-data")
class PersonalDataControllerCitizen(private val accessControl: AccessControl) {
    @PutMapping
    fun updatePersonalData(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: PersonalDataUpdate
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Person.UPDATE_PERSONAL_DATA,
                    user.id
                )

                val person = tx.getPersonById(user.id) ?: error("User not found")

                val validationErrors =
                    listOfNotNull(
                        "invalid preferredName".takeUnless {
                            person.firstName.split(" ").contains(body.preferredName)
                        },
                        "invalid phone".takeUnless { PHONE_PATTERN.matches(body.phone) },
                        "invalid backup phone".takeUnless {
                            body.backupPhone.isBlank() || PHONE_PATTERN.matches(body.backupPhone)
                        },
                        "invalid email".takeUnless {
                            body.email.isBlank() || EMAIL_PATTERN.matches(body.email)
                        }
                    )

                if (validationErrors.isNotEmpty())
                    throw BadRequest(validationErrors.joinToString(", "))

                tx.updatePersonalDetails(user.id, body)
            }
        }
        Audit.PersonalDataUpdate.log(targetId = user.id)
    }
}

data class PersonalDataUpdate(
    val preferredName: String,
    val phone: String,
    val backupPhone: String,
    val email: String
)

fun Database.Transaction.updatePersonalDetails(personId: PersonId, body: PersonalDataUpdate) {
    createUpdate<DatabaseTable> {
            sql(
                """
                UPDATE person SET
                    preferred_name = ${bind(body.preferredName)},
                    phone = ${bind(body.phone)},
                    backup_phone = ${bind(body.backupPhone)},
                    email = ${bind(body.email)}
                WHERE id = ${bind(personId)}
                """
            )
        }
        .updateExactlyOne()
}
