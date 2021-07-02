// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import java.util.UUID

sealed interface DatabaseTable {
    sealed class Daycare : DatabaseTable
    sealed class Employee : DatabaseTable
    sealed class Group : DatabaseTable
    sealed class Person : DatabaseTable
}

typealias ChildId = Id<DatabaseTable.Person>
typealias DaycareId = Id<DatabaseTable.Daycare>
typealias EmployeeId = Id<DatabaseTable.Employee>
typealias GroupId = Id<DatabaseTable.Group>
typealias PersonId = Id<DatabaseTable.Person>

@JsonSerialize(converter = Id.ToJson::class)
@JsonDeserialize(converter = Id.FromJson::class)
data class Id<T : DatabaseTable>(val raw: UUID) {
    override fun toString(): String = raw.toString()

    class FromJson : StdConverter<UUID, Id<*>>() {
        override fun convert(value: UUID): Id<DatabaseTable> = Id(value)
    }

    class ToJson : StdConverter<Id<*>, UUID>() {
        override fun convert(value: Id<*>): UUID = value.raw
    }
}
