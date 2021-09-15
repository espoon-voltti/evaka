// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.UserRole.FINANCE_ADMIN
import fi.espoo.evaka.shared.auth.UserRole.GROUP_STAFF
import fi.espoo.evaka.shared.auth.UserRole.MOBILE
import fi.espoo.evaka.shared.auth.UserRole.SERVICE_WORKER
import fi.espoo.evaka.shared.auth.UserRole.SPECIAL_EDUCATION_TEACHER
import fi.espoo.evaka.shared.auth.UserRole.STAFF
import fi.espoo.evaka.shared.auth.UserRole.UNIT_SUPERVISOR
import java.util.EnumSet

@ExcludeCodeGen
sealed interface Action {
    sealed interface ScopedAction<I> : Action

    /*
    * Global actions are top/root level actions that cannot be tied to any specific scope.
    * If a global action is permitted to a scoped role then user has to have that role somewhere,
    * e.g. be a unit supervisor in some unit.
    * */
    enum class Global(private val roles: EnumSet<UserRole>) : Action {
        CREATE_VASU_TEMPLATE(),
        READ_VASU_TEMPLATE(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),

        FETCH_INCOME_STATEMENTS_AWAITING_HANDLER(FINANCE_ADMIN),

        CREATE_PAPER_APPLICATION(SERVICE_WORKER),
        SEARCH_APPLICATION_WITH_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        SEARCH_APPLICATION_WITHOUT_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR),
        READ_PERSON_APPLICATION(SERVICE_WORKER) // Applications summary on person page
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }

    enum class Application(private val roles: EnumSet<UserRole>) : Action {
        READ_WITH_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        READ_WITHOUT_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR),
        UPDATE(SERVICE_WORKER),

        SEND(SERVICE_WORKER),
        CANCEL(SERVICE_WORKER),

        MOVE_TO_WAITING_PLACEMENT(SERVICE_WORKER),
        RETURN_TO_SENT(SERVICE_WORKER),
        VERIFY(SERVICE_WORKER),

        READ_PLACEMENT_PLAN_DRAFT(SERVICE_WORKER),
        CREATE_PLACEMENT_PLAN(SERVICE_WORKER),
        CANCEL_PLACEMENT_PLAN(SERVICE_WORKER),

        READ_DECISION_DRAFT(SERVICE_WORKER),
        UPDATE_DECISION_DRAFT(SERVICE_WORKER),
        SEND_DECISIONS_WITHOUT_PROPOSAL(SERVICE_WORKER),
        SEND_PLACEMENT_PROPOSAL(SERVICE_WORKER),
        WITHDRAW_PLACEMENT_PROPOSAL(SERVICE_WORKER),
        RESPOND_TO_PLACEMENT_PROPOSAL(SERVICE_WORKER, UNIT_SUPERVISOR),

        CONFIRM_DECISIONS_MAILED(SERVICE_WORKER),
        ACCEPT_DECISION(SERVICE_WORKER, UNIT_SUPERVISOR),
        REJECT_DECISION(SERVICE_WORKER, UNIT_SUPERVISOR);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class AssistanceAction(private val roles: EnumSet<UserRole>) : Action {
        UPDATE(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        DELETE(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class AssistanceNeed(private val roles: EnumSet<UserRole>) : Action {
        UPDATE(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        DELETE(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class BackupCare(private val roles: EnumSet<UserRole>) : ScopedAction<BackupCareId> {
        UPDATE(SERVICE_WORKER, UNIT_SUPERVISOR),
        DELETE(SERVICE_WORKER, UNIT_SUPERVISOR);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class BackupPickup(private val roles: EnumSet<UserRole>) : Action {
        UPDATE(UNIT_SUPERVISOR, STAFF),
        DELETE(UNIT_SUPERVISOR, STAFF);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Child(private val roles: EnumSet<UserRole>) : ScopedAction<ChildId> {
        READ(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF, SPECIAL_EDUCATION_TEACHER),

        READ_ADDITIONAL_INFO(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF, SPECIAL_EDUCATION_TEACHER),
        UPDATE_ADDITIONAL_INFO(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF, SPECIAL_EDUCATION_TEACHER),

        READ_APPLICATION(SERVICE_WORKER),

        CREATE_ASSISTANCE_ACTION(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        READ_ASSISTANCE_ACTION(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),

        CREATE_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        READ_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),

        CREATE_ATTENDANCE_RESERVATION(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),

        CREATE_BACKUP_CARE(SERVICE_WORKER, UNIT_SUPERVISOR),
        READ_BACKUP_CARE(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF, SPECIAL_EDUCATION_TEACHER),

        CREATE_BACKUP_PICKUP(UNIT_SUPERVISOR, STAFF),
        READ_BACKUP_PICKUP(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),

        READ_DAILY_SERVICE_TIMES(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),
        UPDATE_DAILY_SERVICE_TIMES(UNIT_SUPERVISOR),
        DELETE_DAILY_SERVICE_TIMES(UNIT_SUPERVISOR),

        READ_PLACEMENT(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),

        READ_FAMILY_CONTACTS(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        UPDATE_FAMILY_CONTACT(UNIT_SUPERVISOR),

        READ_GUARDIANS(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),

        READ_FEE_ALTERATIONS(FINANCE_ADMIN),

        READ_CHILD_RECIPIENTS(FINANCE_ADMIN, SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),
        UPDATE_CHILD_RECIPIENT(SERVICE_WORKER, UNIT_SUPERVISOR),

        CREATE_VASU_DOCUMENT(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF),
        READ_VASU_DOCUMENT(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class DailyNote(private val roles: EnumSet<UserRole>) : Action {
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Decision(private val roles: EnumSet<UserRole>) : Action {
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Group(private val roles: EnumSet<UserRole>) : ScopedAction<GroupId> {
        UPDATE(UNIT_SUPERVISOR),
        DELETE(SERVICE_WORKER, UNIT_SUPERVISOR),

        READ_ABSENCES(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        READ_DAYCARE_DAILY_NOTES(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE),

        READ_CARETAKERS(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        CREATE_CARETAKERS(UNIT_SUPERVISOR),
        UPDATE_CARETAKERS(UNIT_SUPERVISOR),
        DELETE_CARETAKERS(UNIT_SUPERVISOR),

        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class GroupPlacement(private val roles: EnumSet<UserRole>) : ScopedAction<GroupPlacementId> {
        UPDATE(UNIT_SUPERVISOR),
        DELETE(UNIT_SUPERVISOR);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class IncomeStatement(private val roles: EnumSet<UserRole>) : Action {
        UPDATE_HANDLED(FINANCE_ADMIN),
        UPLOAD_EMPLOYEE_ATTACHMENT(FINANCE_ADMIN),
        DELETE_EMPLOYEE_ATTACHMENT(FINANCE_ADMIN);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class MobileDevice(private val roles: EnumSet<UserRole>) : Action {
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Pairing(private val roles: EnumSet<UserRole>) : Action {
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Person(private val roles: EnumSet<UserRole>) : ScopedAction<PersonId> {
        READ(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        READ_FAMILY_OVERVIEW(FINANCE_ADMIN, UNIT_SUPERVISOR),
        READ_INCOME(FINANCE_ADMIN),
        READ_INCOME_STATEMENTS(FINANCE_ADMIN);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Placement(private val roles: EnumSet<UserRole>) : ScopedAction<PlacementId> {
        UPDATE(SERVICE_WORKER, UNIT_SUPERVISOR),
        DELETE(SERVICE_WORKER, UNIT_SUPERVISOR),

        CREATE_GROUP_PLACEMENT(UNIT_SUPERVISOR),

        CREATE_SERVICE_NEED(UNIT_SUPERVISOR);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class ServiceNeed(private val roles: EnumSet<UserRole>) : Action {
        UPDATE(UNIT_SUPERVISOR),
        DELETE(UNIT_SUPERVISOR);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Unit(private val roles: EnumSet<UserRole>) : ScopedAction<DaycareId> {
        READ_BASIC(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        READ_DETAILED(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR),
        UPDATE(),

        READ_ATTENDANCE_RESERVATIONS(UNIT_SUPERVISOR, STAFF),

        READ_BACKUP_CARE(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF),

        CREATE_PLACEMENT(SERVICE_WORKER, UNIT_SUPERVISOR),
        READ_PLACEMENT(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),

        READ_PLACEMENT_PLAN(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR),

        ACCEPT_PLACEMENT_PROPOSAL(SERVICE_WORKER, UNIT_SUPERVISOR),

        CREATE_GROUP(UNIT_SUPERVISOR),

        READ_ACL(UNIT_SUPERVISOR),
        INSERT_ACL_UNIT_SUPERVISOR(),
        DELETE_ACL_UNIT_SUPERVISOR(),
        INSERT_ACL_SPECIAL_EDUCATION_TEACHER(),
        DELETE_ACL_SPECIAL_EDUCATION_TEACHER(),
        INSERT_ACL_STAFF(UNIT_SUPERVISOR),
        DELETE_ACL_STAFF(UNIT_SUPERVISOR),
        UPDATE_STAFF_GROUP_ACL(UNIT_SUPERVISOR);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class VasuDocument(private val roles: EnumSet<UserRole>) : Action {
        READ(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF),
        UPDATE(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF),
        EVENT_PUBLISHED(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF),
        EVENT_MOVED_TO_READY(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF),
        EVENT_RETURNED_TO_READY(UNIT_SUPERVISOR),
        EVENT_MOVED_TO_REVIEWED(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF),
        EVENT_RETURNED_TO_REVIEWED(),
        EVENT_MOVED_TO_CLOSED(),
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }

    enum class VasuTemplate(private val roles: EnumSet<UserRole>) : Action {
        READ(),
        COPY(),
        UPDATE(),
        DELETE();

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }

    /**
     * Roles allowed to perform this action by default.
     *
     * Can be empty if permission checks for this action are not based on roles.
     */
    fun defaultRoles(): Set<UserRole>
}

private fun Array<out UserRole>.toEnumSet() = EnumSet.noneOf(UserRole::class.java).also {
    it.addAll(this)
}
