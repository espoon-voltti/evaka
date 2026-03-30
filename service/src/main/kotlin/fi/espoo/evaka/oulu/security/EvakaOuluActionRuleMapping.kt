// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.security

import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.ActionRuleMapping
import fi.espoo.evaka.shared.security.actionrule.HasGlobalRole
import fi.espoo.evaka.shared.security.actionrule.HasUnitRole
import fi.espoo.evaka.shared.security.actionrule.ScopedActionRule
import fi.espoo.evaka.shared.security.actionrule.UnscopedActionRule

class EvakaOuluActionRuleMapping : ActionRuleMapping {
    override fun rulesOf(action: Action.UnscopedAction): Sequence<UnscopedActionRule> =
        when (action) {
            Action.Global.READ_TAMPERE_REGIONAL_SURVEY_REPORT,
            Action.Global.SUBMIT_PATU_REPORT,
            Action.Global.SEND_PATU_REPORT -> sequenceOf()

            Action.Global.SETTINGS_PAGE,
            Action.Global.UPDATE_SETTINGS ->
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.SERVICE_WORKER))

            else -> action.defaultRules.asSequence()
        }

    override fun <T> rulesOf(action: Action.ScopedAction<in T>): Sequence<ScopedActionRule<in T>> =
        when (action) {
            Action.BackupCare.UPDATE,
            Action.BackupCare.DELETE -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasUnitRole(UserRole.STAFF).inPlacementUnitOfChildOfBackupCare()
                            as ScopedActionRule<in T>
                    )
            }

            Action.Child.CREATE_BACKUP_CARE,
            Action.Child.READ_ASSISTANCE_ACTION -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasUnitRole(UserRole.STAFF).inPlacementUnitOfChild()
                            as ScopedActionRule<in T>
                    )
            }

            Action.ChildDocument.UPDATE_DECISION_VALIDITY -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>)
            }

            Action.AssistanceAction.READ,
            Action.AssistanceFactor.READ,
            Action.DaycareAssistance.READ,
            Action.PreschoolAssistance.READ,
            Action.OtherAssistanceMeasure.READ -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.SERVICE_WORKER) as ScopedActionRule<in T>)
            }

            Action.Person.ADD_SSN,
            Action.Person.UPDATE_FROM_VTJ -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.FINANCE_ADMIN) as ScopedActionRule<in T>)
            }

            Action.Person.READ_FAMILY_OVERVIEW -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.SERVICE_WORKER) as ScopedActionRule<in T>)
            }

            Action.Person.READ_FOSTER_CHILDREN,
            Action.Person.READ_FOSTER_PARENTS -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.FINANCE_ADMIN) as ScopedActionRule<in T>)
            }

            Action.OtherAssistanceMeasure.READ -> {
                @Suppress("UNCHECKED_CAST")
                // Enable UNIT_SUPERVISORS to see past other assistance measures
                sequenceOf(HasGlobalRole(UserRole.ADMIN) as ScopedActionRule<in T>) +
                    sequenceOf(
                        HasUnitRole(UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.UNIT_SUPERVISOR)
                            .inPlacementUnitOfChildOfOtherAssistanceMeasure(false)
                            as ScopedActionRule<in T>
                    ) +
                    sequenceOf(
                        HasUnitRole(UserRole.STAFF)
                            .inPlacementUnitOfChildOfOtherAssistanceMeasure(true)
                            as ScopedActionRule<in T>
                    )
            }

            Action.Unit.READ_TRANSFER_APPLICATIONS -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasUnitRole(UserRole.UNIT_SUPERVISOR).inUnit() as ScopedActionRule<in T>
                    )
            }

            else -> {
                action.defaultRules.asSequence()
            }
        }
}
