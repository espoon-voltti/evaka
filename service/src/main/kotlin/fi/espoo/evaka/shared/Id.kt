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
    sealed class AssistanceActionOption : DatabaseTable
    sealed class AssistanceBasisOption : DatabaseTable
    sealed class AssistanceNeed : DatabaseTable
    sealed class Attachment : DatabaseTable
    sealed class Attendance : DatabaseTable
    sealed class BackupCare : DatabaseTable
    sealed class BackupPickup : DatabaseTable
    sealed class Daycare : DatabaseTable
    sealed class ChildDailyNote : DatabaseTable
    sealed class ChildImage : DatabaseTable
    sealed class ChildStickyNote : DatabaseTable
    sealed class GroupNote : DatabaseTable
    sealed class Decision : DatabaseTable
    sealed class Employee : DatabaseTable
    sealed class FeeDecision : DatabaseTable
    sealed class FeeThresholds : DatabaseTable
    sealed class Group : DatabaseTable
    sealed class GroupPlacement : DatabaseTable
    sealed class Income : DatabaseTable
    sealed class IncomeStatement : DatabaseTable
    sealed class Message : DatabaseTable
    sealed class MessageAccount : DatabaseTable
    sealed class MessageContent : DatabaseTable
    sealed class MessageDraft : DatabaseTable
    sealed class MessageThread : DatabaseTable
    sealed class MobileDevice : DatabaseTable
    sealed class Pairing : DatabaseTable
    sealed class Parentship : DatabaseTable
    sealed class Partnership : DatabaseTable
    sealed class PedagogicalDocument : DatabaseTable
    sealed class Person : DatabaseTable
    sealed class Placement : DatabaseTable
    sealed class PlacementPlan : DatabaseTable
    sealed class ServiceNeed : DatabaseTable
    sealed class ServiceNeedOption : DatabaseTable
    sealed class StaffAttendance : DatabaseTable
    sealed class StaffAttendanceExternal : DatabaseTable
    sealed class VardaDecision : DatabaseTable
    sealed class VardaPlacement : DatabaseTable
    sealed class VasuDocument : DatabaseTable
    sealed class VasuTemplate : DatabaseTable
    sealed class VoucherValueDecision : DatabaseTable
}

typealias AbsenceId = Id<DatabaseTable.Absence>
typealias ApplicationId = Id<DatabaseTable.Application>
typealias ApplicationNoteId = Id<DatabaseTable.ApplicationNote>
typealias AreaId = Id<DatabaseTable.Area>
typealias AssistanceActionId = Id<DatabaseTable.AssistanceAction>
typealias AssistanceActionOptionId = Id<DatabaseTable.AssistanceActionOption>
typealias AssistanceBasisOptionId = Id<DatabaseTable.AssistanceBasisOption>
typealias AssistanceNeedId = Id<DatabaseTable.AssistanceNeed>
typealias AttachmentId = Id<DatabaseTable.Attachment>
typealias AttendanceId = Id<DatabaseTable.Attendance>
typealias BackupCareId = Id<DatabaseTable.BackupCare>
typealias BackupPickupId = Id<DatabaseTable.BackupPickup>
typealias ChildId = Id<DatabaseTable.Person>
typealias DaycareId = Id<DatabaseTable.Daycare>
typealias ChildDailyNoteId = Id<DatabaseTable.ChildDailyNote>
typealias ChildImageId = Id<DatabaseTable.ChildImage>
typealias ChildStickyNoteId = Id<DatabaseTable.ChildStickyNote>
typealias GroupNoteId = Id<DatabaseTable.GroupNote>
typealias DecisionId = Id<DatabaseTable.Decision>
typealias EmployeeId = Id<DatabaseTable.Employee>
typealias FeeDecisionId = Id<DatabaseTable.FeeDecision>
typealias FeeThresholdsId = Id<DatabaseTable.FeeThresholds>
typealias GroupId = Id<DatabaseTable.Group>
typealias GroupPlacementId = Id<DatabaseTable.GroupPlacement>
typealias IncomeId = Id<DatabaseTable.Income>
typealias IncomeStatementId = Id<DatabaseTable.IncomeStatement>
typealias MessageId = Id<DatabaseTable.Message>
typealias MessageAccountId = Id<DatabaseTable.MessageAccount>
typealias MessageContentId = Id<DatabaseTable.MessageContent>
typealias MessageDraftId = Id<DatabaseTable.MessageDraft>
typealias MessageThreadId = Id<DatabaseTable.MessageThread>
typealias MobileDeviceId = Id<DatabaseTable.MobileDevice>
typealias PairingId = Id<DatabaseTable.Pairing>
typealias ParentshipId = Id<DatabaseTable.Parentship>
typealias PartnershipId = Id<DatabaseTable.Partnership>
typealias PedagogicalDocumentId = Id<DatabaseTable.PedagogicalDocument>
typealias PersonId = Id<DatabaseTable.Person>
typealias PlacementId = Id<DatabaseTable.Placement>
typealias PlacementPlanId = Id<DatabaseTable.PlacementPlan>
typealias ServiceNeedId = Id<DatabaseTable.ServiceNeed>
typealias ServiceNeedOptionId = Id<DatabaseTable.ServiceNeedOption>
typealias StaffAttendanceId = Id<DatabaseTable.StaffAttendance>
typealias StaffAttendanceExternalId = Id<DatabaseTable.StaffAttendanceExternal>
typealias VardaDecisionId = Id<DatabaseTable.VardaDecision>
typealias VardaPlacementId = Id<DatabaseTable.VardaPlacement>
typealias VasuDocumentId = Id<DatabaseTable.VasuDocument>
typealias VasuDocumentFollowupEntryId = Pair<VasuDocumentId, UUID>
typealias VasuTemplateId = Id<DatabaseTable.VasuTemplate>
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
