// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.UserRole.FINANCE_ADMIN
import fi.espoo.evaka.shared.auth.UserRole.GROUP_STAFF
import fi.espoo.evaka.shared.auth.UserRole.SERVICE_WORKER
import fi.espoo.evaka.shared.auth.UserRole.SPECIAL_EDUCATION_TEACHER
import fi.espoo.evaka.shared.auth.UserRole.STAFF
import fi.espoo.evaka.shared.auth.UserRole.UNIT_SUPERVISOR
import java.util.EnumSet

sealed interface Action {
    enum class Global(private val roles: EnumSet<UserRole>) : Action {
        CREATE_VASU_TEMPLATE(),
        READ_VASU_TEMPLATE(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),

        CREATE_PAPER_APPLICATION(SERVICE_WORKER),
        READ_APPLICATION(SERVICE_WORKER, SPECIAL_EDUCATION_TEACHER);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Application(private val roles: EnumSet<UserRole>) : Action {
        READ(SERVICE_WORKER, UNIT_SUPERVISOR),
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
    enum class BackupCare(private val roles: EnumSet<UserRole>) : Action {
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
    enum class Child(private val roles: EnumSet<UserRole>) : Action {
        READ_APPLICATION_SUMMARY(SERVICE_WORKER, UNIT_SUPERVISOR),

        CREATE_ASSISTANCE_ACTION(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        READ_ASSISTANCE_ACTION(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),

        CREATE_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        READ_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),

        CREATE_BACKUP_CARE(SERVICE_WORKER, UNIT_SUPERVISOR),
        READ_BACKUP_CARE(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF, SPECIAL_EDUCATION_TEACHER),

        CREATE_BACKUP_PICKUP(UNIT_SUPERVISOR, STAFF),
        READ_BACKUP_PICKUP(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),

        READ_DAILY_SERVICE_TIMES(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),
        UPDATE_DAILY_SERVICE_TIMES(UNIT_SUPERVISOR),
        DELETE_DAILY_SERVICE_TIMES(UNIT_SUPERVISOR),

        READ_PLACEMENT(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),

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
    enum class Group(private val roles: EnumSet<UserRole>) : Action {
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class GroupPlacement(private val roles: EnumSet<UserRole>) : Action {
        UPDATE(UNIT_SUPERVISOR),
        DELETE(UNIT_SUPERVISOR);

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
    enum class Placement(private val roles: EnumSet<UserRole>) : Action {
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
    enum class Unit(private val roles: EnumSet<UserRole>) : Action {
        READ_BACKUP_CARE(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF),

        CREATE_PLACEMENT(SERVICE_WORKER, UNIT_SUPERVISOR),
        READ_PLACEMENT(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),

        READ_PLACEMENT_PLAN(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR),

        ACCEPT_PLACEMENT_PROPOSAL(SERVICE_WORKER, UNIT_SUPERVISOR);

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
