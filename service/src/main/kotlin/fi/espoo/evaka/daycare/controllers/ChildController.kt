// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.createChild
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.daycare.updateChild
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.pis.service.hideNonPermittedPersonData
import fi.espoo.evaka.pis.updatePreferredName
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.mapper.Nested
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChildController(private val accessControl: AccessControl, private val featureConfig: FeatureConfig) {
    @GetMapping("/children/{childId}")
    fun getChild(db: Database, user: AuthenticatedUser, @PathVariable childId: ChildId): ChildResponse {
        Audit.PersonDetailsRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ, childId)
        return db.connect { dbc ->
            dbc.read { tx ->
                val child = tx.getPersonById(childId)
                    ?.hideNonPermittedPersonData(
                        includeInvoiceAddress = accessControl.hasPermissionFor(
                            user,
                            Action.Person.READ_INVOICE_ADDRESS,
                            childId
                        ),
                        includeOphOid = accessControl.hasPermissionFor(user, Action.Person.READ_OPH_OID, childId)
                    )
                    ?: throw NotFound("Child $childId not found")
                ChildResponse(
                    person = PersonJSON.from(child),
                    permittedActions = accessControl.getPermittedActions(tx, user, childId),
                    permittedPersonActions = accessControl.getPermittedActions(tx, user, childId),
                    assistanceNeedVoucherCoefficientsEnabled = !featureConfig.valueDecisionCapacityFactorEnabled
                )
            }
        }
    }

    @GetMapping("/children/{childId}/additional-information")
    fun getAdditionalInfo(db: Database, user: AuthenticatedUser, @PathVariable childId: ChildId): AdditionalInformation {
        Audit.ChildAdditionalInformationRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_ADDITIONAL_INFO, childId)
        return db.connect { dbc -> dbc.read { it.getAdditionalInformation(childId) } }
    }

    @PutMapping("/children/{childId}/additional-information")
    fun updateAdditionalInfo(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId,
        @RequestBody data: AdditionalInformation
    ) {
        Audit.ChildAdditionalInformationUpdate.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.UPDATE_ADDITIONAL_INFO, childId)
        db.connect { dbc -> dbc.transaction { it.upsertAdditionalInformation(childId, data) } }
    }

    data class ChildResponse(
        val person: PersonJSON,
        val permittedActions: Set<Action.Child>,
        val permittedPersonActions: Set<Action.Person>,
        val assistanceNeedVoucherCoefficientsEnabled: Boolean
    )
}

fun Database.Read.getAdditionalInformation(childId: ChildId): AdditionalInformation {
    val child = getChild(childId)
    return if (child != null) {
        AdditionalInformation(
            allergies = child.additionalInformation.allergies,
            diet = child.additionalInformation.diet,
            additionalInfo = child.additionalInformation.additionalInfo,
            preferredName = child.additionalInformation.preferredName,
            medication = child.additionalInformation.medication
        )
    } else AdditionalInformation()
}

fun Database.Transaction.upsertAdditionalInformation(childId: ChildId, data: AdditionalInformation) {
    updatePreferredName(childId, data.preferredName)
    val child = getChild(childId)
    if (child != null) {
        updateChild(child.copy(additionalInformation = data))
    } else {
        createChild(
            Child(
                id = childId,
                additionalInformation = data
            )
        )
    }
}

data class Child(
    val id: ChildId,
    @Nested val additionalInformation: AdditionalInformation
)

data class AdditionalInformation(
    val allergies: String = "",
    val diet: String = "",
    val additionalInfo: String = "",
    val preferredName: String = "",
    val medication: String = ""
)
