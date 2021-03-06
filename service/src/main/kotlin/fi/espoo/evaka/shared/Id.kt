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
    sealed class Absence : DatabaseTable
    sealed class Application : DatabaseTable
    sealed class ApplicationNote : DatabaseTable
    sealed class Area : DatabaseTable
    sealed class AssistanceAction : DatabaseTable
    sealed class AssistanceNeed : DatabaseTable
    sealed class Attachment : DatabaseTable
    sealed class Attendance : DatabaseTable
    sealed class BackupCare : DatabaseTable
    sealed class Daycare : DatabaseTable
    sealed class DaycareDailyNote : DatabaseTable
    sealed class Decision : DatabaseTable
    sealed class Employee : DatabaseTable
    sealed class FeeDecision : DatabaseTable
    sealed class Group : DatabaseTable
    sealed class GroupPlacement : DatabaseTable
    sealed class Income : DatabaseTable
    sealed class MobileDevice : DatabaseTable
    sealed class Pairing : DatabaseTable
    sealed class Parentship : DatabaseTable
    sealed class Partnership : DatabaseTable
    sealed class Person : DatabaseTable
    sealed class Placement : DatabaseTable
    sealed class PlacementPlan : DatabaseTable
    sealed class ServiceNeed : DatabaseTable
    sealed class ServiceNeedOption : DatabaseTable
    sealed class VardaDecision : DatabaseTable
    sealed class VardaPlacement : DatabaseTable
    sealed class VoucherValueDecision : DatabaseTable
}

typealias AbsenceId = Id<DatabaseTable.Absence>
typealias ApplicationId = Id<DatabaseTable.Application>
typealias ApplicationNoteId = Id<DatabaseTable.ApplicationNote>
typealias AreaId = Id<DatabaseTable.Area>
typealias AssistanceActionId = Id<DatabaseTable.AssistanceAction>
typealias AssistanceNeedId = Id<DatabaseTable.AssistanceNeed>
typealias AttachmentId = Id<DatabaseTable.Attachment>
typealias AttendanceId = Id<DatabaseTable.Attendance>
typealias BackupCareId = Id<DatabaseTable.BackupCare>
typealias ChildId = Id<DatabaseTable.Person>
typealias DaycareId = Id<DatabaseTable.Daycare>
typealias DaycareDailyNoteId = Id<DatabaseTable.DaycareDailyNote>
typealias DecisionId = Id<DatabaseTable.Decision>
typealias EmployeeId = Id<DatabaseTable.Employee>
typealias FeeDecisionId = Id<DatabaseTable.FeeDecision>
typealias GroupId = Id<DatabaseTable.Group>
typealias GroupPlacementId = Id<DatabaseTable.GroupPlacement>
typealias IncomeId = Id<DatabaseTable.Income>
typealias MobileDeviceId = Id<DatabaseTable.MobileDevice>
typealias PairingId = Id<DatabaseTable.Pairing>
typealias ParentshipId = Id<DatabaseTable.Parentship>
typealias PartnershipId = Id<DatabaseTable.Partnership>
typealias PersonId = Id<DatabaseTable.Person>
typealias PlacementId = Id<DatabaseTable.Placement>
typealias PlacementPlanId = Id<DatabaseTable.PlacementPlan>
typealias ServiceNeedId = Id<DatabaseTable.ServiceNeed>
typealias ServiceNeedOptionId = Id<DatabaseTable.ServiceNeedOption>
typealias VardaDecisionId = Id<DatabaseTable.VardaDecision>
typealias VardaPlacementId = Id<DatabaseTable.VardaPlacement>
typealias VoucherValueDecisionId = Id<DatabaseTable.VoucherValueDecision>

@JsonSerialize(converter = Id.ToJson::class)
@JsonDeserialize(converter = Id.FromJson::class, keyUsing = Id.KeyFromJson::class)
data class Id<out T : DatabaseTable>(val raw: UUID) : Comparable<Id<*>> {
    override fun toString(): String = raw.toString()
    override fun hashCode(): Int = raw.hashCode()
    override fun compareTo(other: Id<*>): Int = this.raw.compareTo(other.raw)

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
