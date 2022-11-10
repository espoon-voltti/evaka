// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children.consent

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChildConsentController(
    private val accessControl: AccessControl,
    private val featureConfig: FeatureConfig
) {
    @GetMapping("/children/{childId}/consent")
    fun getChildConsents(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ): List<ChildConsent> {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_CHILD_CONSENTS,
                        childId
                    )
                    val consents = tx.getChildConsentsByChild(childId)
                    featureConfig.enabledChildConsentTypes.map { type ->
                        consents.find { it.type == type }
                            ?: ChildConsent(type, null, null, null, null)
                    }
                }
            }
            .also {
                Audit.ChildConsentsRead.log(targetId = childId, meta = mapOf("count" to it.size))
            }
    }

    @GetMapping("/citizen/children/consents")
    fun getCitizenChildConsents(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock
    ): Map<ChildId, List<CitizenChildConsent>> {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_CHILD_CONSENTS,
                        user.id
                    )
                    tx.getCitizenChildConsentsForGuardian(user.id, clock.today()).mapValues {
                        consent ->
                        featureConfig.enabledChildConsentTypes.map { type ->
                            CitizenChildConsent(type, consent.value.find { it.type == type }?.given)
                        }
                    }
                }
            }
            .also {
                Audit.ChildConsentsReadCitizen.log(
                    targetId = user.id,
                    meta =
                        mapOf(
                            "count" to
                                it.values.asSequence().map { consents -> consents.size }.sum()
                        )
                )
            }
    }

    @PostMapping("/children/{childId}/consent")
    fun modifyChildConsents(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: List<UpdateChildConsentRequest>
    ) {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.UPSERT_CHILD_CONSENT,
                        childId
                    )
                    body
                        .filter { featureConfig.enabledChildConsentTypes.contains(it.type) }
                        .forEach { consent ->
                            if (consent.given == null) {
                                tx.deleteChildConsentEmployee(childId, consent.type)
                            } else {
                                tx.upsertChildConsentEmployee(
                                    clock,
                                    childId,
                                    consent.type,
                                    consent.given,
                                    user.id,
                                    clock.now()
                                )
                            }
                        }
                }
            }
            .also { Audit.ChildConsentsUpdate.log(targetId = childId) }
    }

    @PostMapping("/citizen/children/{childId}/consent")
    fun citizenUpsertChildConsents(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: List<CitizenChildConsent>
    ) {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.INSERT_CHILD_CONSENTS,
                        childId
                    )
                    body
                        .filter { featureConfig.enabledChildConsentTypes.contains(it.type) }
                        .forEach { consent ->
                            if (
                                consent.given == null ||
                                    !tx.insertChildConsentCitizen(
                                        clock,
                                        childId,
                                        consent.type,
                                        consent.given,
                                        user.id
                                    )
                            ) {
                                throw Forbidden(
                                    "Citizens may not modify consent that has already been given."
                                )
                            }
                        }
                }
            }
            .also { Audit.ChildConsentsInsertCitizen.log(targetId = childId) }
    }

    @GetMapping("/citizen/children/consents/notifications")
    fun getCitizenChildConsentNotifications(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock
    ): Map<ChildId, Int> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_CHILD_CONSENT_NOTIFICATIONS,
                        user.id
                    )
                    tx.getCitizenConsentedChildConsentTypes(user.id, clock.today())
                        .map { (child, knownConsentTypes) ->
                            child to
                                featureConfig.enabledChildConsentTypes
                                    .filterNot { knownConsentTypes.contains(it) }
                                    .size
                        }
                        .toMap()
                }
            }
            .also { Audit.ChildConsentsReadNotificationsCitizen.log(targetId = user.id) }
    }

    data class UpdateChildConsentRequest(val type: ChildConsentType, val given: Boolean?)
}
