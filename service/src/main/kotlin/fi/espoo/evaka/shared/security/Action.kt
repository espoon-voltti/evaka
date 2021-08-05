// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.UserRole.FINANCE_ADMIN
import fi.espoo.evaka.shared.auth.UserRole.SERVICE_WORKER
import fi.espoo.evaka.shared.auth.UserRole.SPECIAL_EDUCATION_TEACHER
import fi.espoo.evaka.shared.auth.UserRole.STAFF
import fi.espoo.evaka.shared.auth.UserRole.UNIT_SUPERVISOR
import java.util.EnumSet

sealed interface Action {
    enum class Application(private val roles: EnumSet<UserRole>) : Action {
        ;

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
        CREATE_ASSISTANCE_ACTION(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        READ_ASSISTANCE_ACTION(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        CREATE_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        READ_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        CREATE_BACKUP_CARE(SERVICE_WORKER, UNIT_SUPERVISOR),
        READ_BACKUP_CARE(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF, SPECIAL_EDUCATION_TEACHER),
        CREATE_BACKUP_PICKUP(UNIT_SUPERVISOR, STAFF),
        READ_BACKUP_PICKUP(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER);

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
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class ServiceNeed(private val roles: EnumSet<UserRole>) : Action {
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Unit(private val roles: EnumSet<UserRole>) : Action {
        READ_BACKUP_CARE(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class VasuDocument(private val roles: EnumSet<UserRole>) : Action {
        ;

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
