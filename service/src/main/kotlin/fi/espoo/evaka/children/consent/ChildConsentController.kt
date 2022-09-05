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
        @PathVariable childId: ChildId
    ): List<ChildConsent> {
        Audit.ChildConsentsRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_CHILD_CONSENTS, childId)
        return db.connect { dbc ->
            dbc.transaction { tx ->
                val consents = tx.getChildConsentsByChild(childId)
                featureConfig.enabledChildConsentTypes.map { type ->
                    consents.find { it.type == type }
                        ?: ChildConsent(type, null, null, null, null)
                }
            }
        }
    }

    @GetMapping("/citizen/children/consents")
    fun getCitizenChildConsents(
        db: Database,
        user: AuthenticatedUser.Citizen,
        evakaClock: EvakaClock
    ): Map<ChildId, List<CitizenChildConsent>> {
        Audit.ChildConsentsReadCitizen.log(targetId = user.id)
        accessControl.requirePermissionFor(user, Action.Citizen.Person.READ_CHILD_CONSENTS, user.id)
        return db.connect { dbc ->
            dbc.transaction { tx ->
                tx.getCitizenChildConsentsForGuardian(user.id, evakaClock.today()).mapValues { consent ->
                    featureConfig.enabledChildConsentTypes.map { type ->
                        CitizenChildConsent(type, consent.value.find { it.type == type }?.given)
                    }
                }
            }
        }
    }

    @PostMapping("/children/{childId}/consent")
    fun modifyChildConsents(
        db: Database,
        user: AuthenticatedUser.Employee,
        evakaClock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: List<UpdateChildConsentRequest>
    ) {
        Audit.ChildConsentsReadCitizen.log()
        accessControl.requirePermissionFor(user, Action.Child.UPSERT_CHILD_CONSENT, childId)
        return db.connect { dbc ->
            dbc.transaction { tx ->
                body.filter { featureConfig.enabledChildConsentTypes.contains(it.type) }.forEach { consent ->
                    if (consent.given == null) {
                        tx.deleteChildConsentEmployee(childId, consent.type)
                    } else {
                        tx.upsertChildConsentEmployee(childId, consent.type, consent.given, user.id, evakaClock.now())
                    }
                }
            }
        }
    }

    @PostMapping("/citizen/children/{childId}/consent")
    fun citizenUpsertChildConsents(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @PathVariable childId: ChildId,
        @RequestBody body: List<CitizenChildConsent>
    ) {
        Audit.ChildConsentsInsertCitizen.log()
        accessControl.requirePermissionFor(user, Action.Citizen.Child.INSERT_CHILD_CONSENTS, childId)
        return db.connect { dbc ->
            dbc.transaction { tx ->
                body.filter { featureConfig.enabledChildConsentTypes.contains(it.type) }.forEach { consent ->
                    if (consent.given == null || !tx.insertChildConsentCitizen(childId, consent.type, consent.given, user.id)) {
                        throw Forbidden("Citizens may not modify consent that has already been given.")
                    }
                }
            }
        }
    }

    @GetMapping("/citizen/children/consents/notifications")
    fun getCitizenChildConsentNotifications(
        db: Database,
        user: AuthenticatedUser.Citizen,
        evakaClock: EvakaClock
    ): Map<ChildId, Int> {
        Audit.ChildConsentsReadNotificationsCitizen.log(targetId = user.id)
        accessControl.requirePermissionFor(user, Action.Citizen.Person.READ_CHILD_CONSENT_NOTIFICATIONS, user.id)
        return db.connect { dbc ->
            dbc.transaction { tx ->
                tx.getCitizenConsentedChildConsentTypes(user.id, evakaClock.today()).map { (child, knownConsentTypes) ->
                    child to featureConfig.enabledChildConsentTypes.filterNot { knownConsentTypes.contains(it) }.size
                }.toMap()
            }
        }
    }

    data class UpdateChildConsentRequest(
        val type: ChildConsentType,
        val given: Boolean?
    )
}
