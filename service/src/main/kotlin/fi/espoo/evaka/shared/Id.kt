// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import java.util.UUID

sealed interface DatabaseTable {
    sealed class Application : DatabaseTable
    sealed class ApplicationNote : DatabaseTable
    sealed class Area : DatabaseTable
    sealed class Daycare : DatabaseTable
    sealed class Employee : DatabaseTable
    sealed class Group : DatabaseTable
    sealed class GroupPlacement : DatabaseTable
    sealed class Partnership : DatabaseTable
    sealed class Person : DatabaseTable
    sealed class Placement : DatabaseTable
}

typealias ApplicationId = Id<DatabaseTable.Application>
typealias ApplicationNoteId = Id<DatabaseTable.ApplicationNote>
typealias AreaId = Id<DatabaseTable.Area>
typealias ChildId = Id<DatabaseTable.Person>
typealias DaycareId = Id<DatabaseTable.Daycare>
typealias EmployeeId = Id<DatabaseTable.Employee>
typealias GroupId = Id<DatabaseTable.Group>
typealias GroupPlacementId = Id<DatabaseTable.GroupPlacement>
typealias PartnershipId = Id<DatabaseTable.Partnership>
typealias PersonId = Id<DatabaseTable.Person>
typealias PlacementId = Id<DatabaseTable.Placement>

@JsonSerialize(converter = Id.ToJson::class)
@JsonDeserialize(converter = Id.FromJson::class, keyUsing = Id.KeyFromJson::class)
data class Id<out T : DatabaseTable>(val raw: UUID) {
    override fun toString(): String = raw.toString()

    class FromJson<T> : StdConverter<UUID, Id<*>>() {
        override fun convert(value: UUID): Id<DatabaseTable> = Id(value)
    }

    class ToJson : StdConverter<Id<*>, UUID>() {
        override fun convert(value: Id<*>): UUID = value.raw
    }

    class KeyFromJson : KeyDeserializer() {
        override fun deserializeKey(key: String, ctxt: DeserializationContext): Any =
            Id<DatabaseTable>(UUID.fromString(key))
    }
}
