// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { Absence } from 'lib-common/generated/api-types/absence'
import type { AbsenceId } from './api-types'
import type { ApplicationDetails } from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import type { Autocomplete } from './api-types'
import type { CalendarEventAttendeeId } from './api-types'
import type { CalendarEventId } from 'lib-common/generated/api-types/shared'
import type { CalendarEventTimeId } from 'lib-common/generated/api-types/shared'
import type { Caretaker } from './api-types'
import type { ChildDailyNoteBody } from 'lib-common/generated/api-types/note'
import type { ChildDailyNoteId } from 'lib-common/generated/api-types/shared'
import type { ChildDocumentId } from 'lib-common/generated/api-types/shared'
import type { ChildStickyNoteBody } from 'lib-common/generated/api-types/note'
import type { ChildStickyNoteId } from 'lib-common/generated/api-types/shared'
import type { DailyReservationRequest } from 'lib-common/generated/api-types/reservations'
import type { DailyServiceTimeId } from 'lib-common/generated/api-types/shared'
import type { DaycareAclInsert } from './api-types'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import type { DaycarePlacementPlan } from 'lib-common/generated/api-types/application'
import type { Decision } from 'lib-common/generated/api-types/decision'
import type { DecisionId } from 'lib-common/generated/api-types/shared'
import type { DecisionRequest } from './api-types'
import type { DevAbsence } from './api-types'
import type { DevApplicationWithForm } from './api-types'
import type { DevAssistanceAction } from './api-types'
import type { DevAssistanceActionOption } from './api-types'
import type { DevAssistanceFactor } from './api-types'
import type { DevAssistanceNeedDecision } from './api-types'
import type { DevAssistanceNeedPreschoolDecision } from './api-types'
import type { DevAssistanceNeedVoucherCoefficient } from './api-types'
import type { DevBackupCare } from './api-types'
import type { DevBackupPickup } from './api-types'
import type { DevCalendarEvent } from './api-types'
import type { DevCalendarEventAttendee } from './api-types'
import type { DevCalendarEventTime } from './api-types'
import type { DevCareArea } from './api-types'
import type { DevChild } from './api-types'
import type { DevChildAttendance } from './api-types'
import type { DevChildDocument } from './api-types'
import type { DevClubTerm } from './api-types'
import type { DevDailyServiceTimeNotification } from './api-types'
import type { DevDailyServiceTimes } from './api-types'
import type { DevDaycare } from './api-types'
import type { DevDaycareAssistance } from './api-types'
import type { DevDaycareGroup } from './api-types'
import type { DevDaycareGroupAcl } from './api-types'
import type { DevDaycareGroupPlacement } from './api-types'
import type { DevDocumentTemplate } from './api-types'
import type { DevEmployee } from './api-types'
import type { DevEmployeePin } from './api-types'
import type { DevFamilyContact } from './api-types'
import type { DevFinanceNote } from './api-types'
import type { DevFosterParent } from './api-types'
import type { DevFridgeChild } from './api-types'
import type { DevFridgePartner } from './api-types'
import type { DevGuardian } from './api-types'
import type { DevIncome } from './api-types'
import type { DevIncomeStatement } from './api-types'
import type { DevInvoice } from './api-types'
import type { DevMobileDevice } from './api-types'
import type { DevOtherAssistanceMeasure } from './api-types'
import type { DevParentship } from './api-types'
import type { DevPayment } from './api-types'
import type { DevPedagogicalDocument } from './api-types'
import type { DevPerson } from './api-types'
import type { DevPersonEmail } from './api-types'
import type { DevPersonType } from './api-types'
import type { DevPersonalMobileDevice } from './api-types'
import type { DevPlacement } from './api-types'
import type { DevPreschoolAssistance } from './api-types'
import type { DevPreschoolTerm } from './api-types'
import type { DevServiceNeed } from './api-types'
import type { DevStaffAttendance } from './api-types'
import type { DevStaffAttendancePlan } from './api-types'
import type { DevTerminatePlacementRequest } from './api-types'
import type { DevUpsertStaffOccupancyCoefficient } from './api-types'
import type { DocumentTemplateId } from 'lib-common/generated/api-types/shared'
import type { Email } from './api-types'
import type { EmployeeId } from 'lib-common/generated/api-types/shared'
import type { FeeDecision } from 'lib-common/generated/api-types/invoicing'
import type { FeeThresholds } from 'lib-common/generated/api-types/invoicing'
import type { FeeThresholdsId } from 'lib-common/generated/api-types/shared'
import type { GroupId } from 'lib-common/generated/api-types/shared'
import type { GroupNoteBody } from 'lib-common/generated/api-types/note'
import type { GroupNoteId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import type { HolidayPeriodCreate } from 'lib-common/generated/api-types/holidayperiod'
import type { HolidayPeriodId } from 'lib-common/generated/api-types/shared'
import type { HolidayQuestionnaireId } from 'lib-common/generated/api-types/shared'
import type { IncomeNotification } from 'lib-common/generated/api-types/invoicing'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { MockVtjDataset } from './api-types'
import type { NekkuSpecialDiet } from './api-types'
import type { NekkuSpecialDietChoices } from 'lib-common/generated/api-types/nekku'
import type { Pairing } from 'lib-common/generated/api-types/pairing'
import type { PairingId } from 'lib-common/generated/api-types/shared'
import type { PaymentId } from 'lib-common/generated/api-types/shared'
import type { PedagogicalDocumentId } from 'lib-common/generated/api-types/shared'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { PlacementId } from 'lib-common/generated/api-types/shared'
import type { PlacementPlan } from './api-types'
import type { PostPairingChallengeReq } from 'lib-common/generated/api-types/pairing'
import type { PostPairingReq } from 'lib-common/generated/api-types/pairing'
import type { PostPairingResponseReq } from 'lib-common/generated/api-types/pairing'
import type { QuestionnaireBody } from 'lib-common/generated/api-types/holidayperiod'
import type { ReservationInsert } from './api-types'
import type { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import type { SfiMessage } from './api-types'
import type { SimpleApplicationAction } from 'lib-common/generated/api-types/application'
import type { SpecialDiet } from 'lib-common/generated/api-types/specialdiet'
import type { StaffAttendancePlanId } from './api-types'
import type { StaffAttendanceRealtimeId } from 'lib-common/generated/api-types/shared'
import type { StaffMemberAttendance } from 'lib-common/generated/api-types/attendance'
import type { UpdateIncomeStatementHandledBody } from './api-types'
import type { UpdateWeakLoginCredentialsRequest } from 'lib-common/generated/api-types/pis'
import type { VoucherValueDecision } from './api-types'
import { AxiosProgressEvent } from 'axios'
import { DevApiError } from '../dev-api'
import { createFormData } from 'lib-common/api'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonAbsence } from 'lib-common/generated/api-types/absence'
import { deserializeJsonApplicationDetails } from 'lib-common/generated/api-types/application'
import { deserializeJsonDecision } from 'lib-common/generated/api-types/decision'
import { deserializeJsonPairing } from 'lib-common/generated/api-types/pairing'
import { deserializeJsonStaffMemberAttendance } from 'lib-common/generated/api-types/attendance'
import { devClient } from '../dev-api'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.addAbsence
*/
export async function addAbsence(
  request: {
    body: DevAbsence
  }
): Promise<AbsenceId> {
  try {
    const { data: json } = await devClient.request<JsonOf<AbsenceId>>({
      url: uri`/absence`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevAbsence>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.addAclRoleForDaycare
*/
export async function addAclRoleForDaycare(
  request: {
    daycareId: DaycareId,
    body: DaycareAclInsert
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/daycares/${request.daycareId}/acl`.toString(),
      method: 'PUT',
      data: request.body satisfies JsonCompatible<DaycareAclInsert>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.addCalendarEvent
*/
export async function addCalendarEvent(
  request: {
    body: DevCalendarEvent
  }
): Promise<CalendarEventId> {
  try {
    const { data: json } = await devClient.request<JsonOf<CalendarEventId>>({
      url: uri`/calendar-event`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevCalendarEvent>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.addCalendarEventAttendee
*/
export async function addCalendarEventAttendee(
  request: {
    body: DevCalendarEventAttendee
  }
): Promise<CalendarEventAttendeeId> {
  try {
    const { data: json } = await devClient.request<JsonOf<CalendarEventAttendeeId>>({
      url: uri`/calendar-event-attendee`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevCalendarEventAttendee>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.addCalendarEventTime
*/
export async function addCalendarEventTime(
  request: {
    body: DevCalendarEventTime
  }
): Promise<CalendarEventTimeId> {
  try {
    const { data: json } = await devClient.request<JsonOf<CalendarEventTimeId>>({
      url: uri`/calendar-event-time`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevCalendarEventTime>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.addDailyServiceTime
*/
export async function addDailyServiceTime(
  request: {
    body: DevDailyServiceTimes
  }
): Promise<DailyServiceTimeId> {
  try {
    const { data: json } = await devClient.request<JsonOf<DailyServiceTimeId>>({
      url: uri`/daily-service-time`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevDailyServiceTimes>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.addDailyServiceTimeNotification
*/
export async function addDailyServiceTimeNotification(
  request: {
    body: DevDailyServiceTimeNotification
  }
): Promise<number> {
  try {
    const { data: json } = await devClient.request<JsonOf<number>>({
      url: uri`/daily-service-time-notification`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevDailyServiceTimeNotification>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.addPayment
*/
export async function addPayment(
  request: {
    body: DevPayment
  }
): Promise<PaymentId> {
  try {
    const { data: json } = await devClient.request<JsonOf<PaymentId>>({
      url: uri`/payments`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevPayment>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.addStaffAttendance
*/
export async function addStaffAttendance(
  request: {
    body: DevStaffAttendance
  }
): Promise<StaffAttendanceRealtimeId> {
  try {
    const { data: json } = await devClient.request<JsonOf<StaffAttendanceRealtimeId>>({
      url: uri`/realtime-staff-attendance`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevStaffAttendance>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.addStaffAttendancePlan
*/
export async function addStaffAttendancePlan(
  request: {
    body: DevStaffAttendancePlan
  }
): Promise<StaffAttendancePlanId> {
  try {
    const { data: json } = await devClient.request<JsonOf<StaffAttendancePlanId>>({
      url: uri`/staff-attendance-plan`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevStaffAttendancePlan>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.cleanUpMessages
*/
export async function cleanUpMessages(): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/messages/clean-up`.toString(),
      method: 'POST'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createApplicationPlacementPlan
*/
export async function createApplicationPlacementPlan(
  request: {
    applicationId: ApplicationId,
    body: DaycarePlacementPlan
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/applications/${request.applicationId}/actions/create-placement-plan`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<DaycarePlacementPlan>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createApplications
*/
export async function createApplications(
  request: {
    body: DevApplicationWithForm[]
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<ApplicationId[]> {
  try {
    const { data: json } = await devClient.request<JsonOf<ApplicationId[]>>({
      url: uri`/applications`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<DevApplicationWithForm[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createAssistanceAction
*/
export async function createAssistanceAction(
  request: {
    body: DevAssistanceAction[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/assistance-action`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevAssistanceAction[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createAssistanceActionOption
*/
export async function createAssistanceActionOption(
  request: {
    body: DevAssistanceActionOption[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/assistance-action-option`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevAssistanceActionOption[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createAssistanceFactors
*/
export async function createAssistanceFactors(
  request: {
    body: DevAssistanceFactor[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/assistance-factors`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevAssistanceFactor[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createAssistanceNeedDecisions
*/
export async function createAssistanceNeedDecisions(
  request: {
    body: DevAssistanceNeedDecision[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/assistance-need-decisions`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevAssistanceNeedDecision[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createAssistanceNeedPreschoolDecisions
*/
export async function createAssistanceNeedPreschoolDecisions(
  request: {
    body: DevAssistanceNeedPreschoolDecision[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/assistance-need-preschool-decisions`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevAssistanceNeedPreschoolDecision[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createAssistanceNeedVoucherCoefficients
*/
export async function createAssistanceNeedVoucherCoefficients(
  request: {
    body: DevAssistanceNeedVoucherCoefficient[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/assistance-need-voucher-coefficients`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevAssistanceNeedVoucherCoefficient[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createBackupCares
*/
export async function createBackupCares(
  request: {
    body: DevBackupCare[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/backup-cares`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevBackupCare[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createBackupPickup
*/
export async function createBackupPickup(
  request: {
    body: DevBackupPickup[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/backup-pickup`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevBackupPickup[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createCareAreas
*/
export async function createCareAreas(
  request: {
    body: DevCareArea[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/care-areas`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevCareArea[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createChildDocument
*/
export async function createChildDocument(
  request: {
    body: DevChildDocument
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<ChildDocumentId> {
  try {
    const { data: json } = await devClient.request<JsonOf<ChildDocumentId>>({
      url: uri`/child-documents`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<DevChildDocument>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createChildren
*/
export async function createChildren(
  request: {
    body: DevChild[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/children`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevChild[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createClubTerm
*/
export async function createClubTerm(
  request: {
    body: DevClubTerm
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/club-term`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevClubTerm>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createDaycareAssistances
*/
export async function createDaycareAssistances(
  request: {
    body: DevDaycareAssistance[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/daycare-assistances`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevDaycareAssistance[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createDaycareCaretakers
*/
export async function createDaycareCaretakers(
  request: {
    body: Caretaker[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/daycare-caretakers`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<Caretaker[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createDaycareGroupAclRows
*/
export async function createDaycareGroupAclRows(
  request: {
    body: DevDaycareGroupAcl[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/daycare-group-acl`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevDaycareGroupAcl[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createDaycareGroupPlacement
*/
export async function createDaycareGroupPlacement(
  request: {
    body: DevDaycareGroupPlacement[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/daycare-group-placements`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevDaycareGroupPlacement[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createDaycareGroups
*/
export async function createDaycareGroups(
  request: {
    body: DevDaycareGroup[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/daycare-groups`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevDaycareGroup[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createDaycarePlacements
*/
export async function createDaycarePlacements(
  request: {
    body: DevPlacement[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/daycare-placements`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevPlacement[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createDaycares
*/
export async function createDaycares(
  request: {
    body: DevDaycare[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/daycares`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevDaycare[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createDecisionPdf
*/
export async function createDecisionPdf(
  request: {
    id: DecisionId
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/decisions/${request.id}/actions/create-pdf`.toString(),
      method: 'POST'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createDecisions
*/
export async function createDecisions(
  request: {
    body: DecisionRequest[]
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/decisions`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<DecisionRequest[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createDefaultPlacementPlan
*/
export async function createDefaultPlacementPlan(
  request: {
    applicationId: ApplicationId
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/applications/${request.applicationId}/actions/create-default-placement-plan`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() }
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createDefaultServiceNeedOptions
*/
export async function createDefaultServiceNeedOptions(): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/service-need-options`.toString(),
      method: 'POST'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createDocumentTemplate
*/
export async function createDocumentTemplate(
  request: {
    body: DevDocumentTemplate
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<DocumentTemplateId> {
  try {
    const { data: json } = await devClient.request<JsonOf<DocumentTemplateId>>({
      url: uri`/document-templates`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<DevDocumentTemplate>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createEmployee
*/
export async function createEmployee(
  request: {
    body: DevEmployee
  }
): Promise<EmployeeId> {
  try {
    const { data: json } = await devClient.request<JsonOf<EmployeeId>>({
      url: uri`/employee`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevEmployee>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createEmployeePins
*/
export async function createEmployeePins(
  request: {
    body: DevEmployeePin[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/employee-pin`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevEmployeePin[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createFamilyContact
*/
export async function createFamilyContact(
  request: {
    body: DevFamilyContact[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/family-contact`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevFamilyContact[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createFeeDecisions
*/
export async function createFeeDecisions(
  request: {
    body: FeeDecision[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/fee-decisions`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<FeeDecision[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createFeeThresholds
*/
export async function createFeeThresholds(
  request: {
    body: FeeThresholds
  }
): Promise<FeeThresholdsId> {
  try {
    const { data: json } = await devClient.request<JsonOf<FeeThresholdsId>>({
      url: uri`/fee-thresholds`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<FeeThresholds>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createFinanceNotes
*/
export async function createFinanceNotes(
  request: {
    body: DevFinanceNote[]
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/finance-notes`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<DevFinanceNote[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createFosterParent
*/
export async function createFosterParent(
  request: {
    body: DevFosterParent[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/foster-parent`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevFosterParent[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createFridgeChild
*/
export async function createFridgeChild(
  request: {
    body: DevFridgeChild[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/fridge-child`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevFridgeChild[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createFridgePartner
*/
export async function createFridgePartner(
  request: {
    body: DevFridgePartner[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/fridge-partner`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevFridgePartner[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createHolidayPeriod
*/
export async function createHolidayPeriod(
  request: {
    id: HolidayPeriodId,
    body: HolidayPeriodCreate
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/holiday-period/${request.id}`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<HolidayPeriodCreate>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createHolidayQuestionnaire
*/
export async function createHolidayQuestionnaire(
  request: {
    id: HolidayQuestionnaireId,
    body: QuestionnaireBody
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/holiday-period/questionnaire/${request.id}`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<QuestionnaireBody>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createIncome
*/
export async function createIncome(
  request: {
    body: DevIncome
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/income`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevIncome>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createIncomeNotification
*/
export async function createIncomeNotification(
  request: {
    body: IncomeNotification
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/income-notifications`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<IncomeNotification>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createIncomeStatement
*/
export async function createIncomeStatement(
  request: {
    body: DevIncomeStatement
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/income-statement`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevIncomeStatement>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createInvoices
*/
export async function createInvoices(
  request: {
    body: DevInvoice[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/invoices`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevInvoice[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createMessageAccounts
*/
export async function createMessageAccounts(): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/message-account/upsert-all`.toString(),
      method: 'POST'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createNekkuSpecialDiets
*/
export async function createNekkuSpecialDiets(
  request: {
    body: NekkuSpecialDiet[]
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/nekku-special-diets`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<NekkuSpecialDiet[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createOtherAssistanceMeasures
*/
export async function createOtherAssistanceMeasures(
  request: {
    body: DevOtherAssistanceMeasure[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/other-assistance-measures`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevOtherAssistanceMeasure[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createParentships
*/
export async function createParentships(
  request: {
    body: DevParentship[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/parentship`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevParentship[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createPedagogicalDocumentAttachment
*/
export async function createPedagogicalDocumentAttachment(
  request: {
    pedagogicalDocumentId: PedagogicalDocumentId,
    employeeId: EmployeeId,
    file: File
  },
  options?: {
    onUploadProgress?: (event: AxiosProgressEvent) => void,
    mockedTime?: HelsinkiDateTime
  }
): Promise<string> {
  try {
    const data = createFormData(
      ['file', request.file]
    )
    const params = createUrlSearchParams(
      ['employeeId', request.employeeId]
    )
    const { data: json } = await devClient.request<JsonOf<string>>({
      url: uri`/pedagogical-document-attachment/${request.pedagogicalDocumentId}`.toString(),
      method: 'POST',
      headers: {
        'Content-Type': 'multipart/form-data',
        'EvakaMockedTime': options?.mockedTime?.formatIso()
      },
      onUploadProgress: options?.onUploadProgress,
      params,
      data
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createPedagogicalDocuments
*/
export async function createPedagogicalDocuments(
  request: {
    body: DevPedagogicalDocument[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/pedagogical-document`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevPedagogicalDocument[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createPerson
*/
export async function createPerson(
  request: {
    type: DevPersonType,
    body: DevPerson
  }
): Promise<PersonId> {
  try {
    const params = createUrlSearchParams(
      ['type', request.type.toString()]
    )
    const { data: json } = await devClient.request<JsonOf<PersonId>>({
      url: uri`/person/create`.toString(),
      method: 'POST',
      params,
      data: request.body satisfies JsonCompatible<DevPerson>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createPlacementPlan
*/
export async function createPlacementPlan(
  request: {
    applicationId: ApplicationId,
    body: PlacementPlan
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/placement-plan/${request.applicationId}`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<PlacementPlan>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createPreschoolAssistances
*/
export async function createPreschoolAssistances(
  request: {
    body: DevPreschoolAssistance[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/preschool-assistances`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevPreschoolAssistance[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createPreschoolTerm
*/
export async function createPreschoolTerm(
  request: {
    body: DevPreschoolTerm
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/preschool-term`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevPreschoolTerm>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createServiceNeedOption
*/
export async function createServiceNeedOption(
  request: {
    body: ServiceNeedOption[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/service-need-option`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<ServiceNeedOption[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createServiceNeeds
*/
export async function createServiceNeeds(
  request: {
    body: DevServiceNeed[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/service-need`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevServiceNeed[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createVoucherValueDecisions
*/
export async function createVoucherValueDecisions(
  request: {
    body: VoucherValueDecision[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/value-decisions`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<VoucherValueDecision[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createVoucherValues
*/
export async function createVoucherValues(): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/voucher-values`.toString(),
      method: 'POST'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.deleteDaycareCostCenter
*/
export async function deleteDaycareCostCenter(
  request: {
    daycareId: DaycareId
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/daycare/${request.daycareId}/cost-center`.toString(),
      method: 'DELETE'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.deletePlacement
*/
export async function deletePlacement(
  request: {
    placementId: PlacementId
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/placement/${request.placementId}`.toString(),
      method: 'DELETE'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.forceFullVtjRefresh
*/
export async function forceFullVtjRefresh(
  request: {
    person: PersonId
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/persons/${request.person}/force-full-vtj-refresh`.toString(),
      method: 'POST'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.generateReplacementDraftInvoices
*/
export async function generateReplacementDraftInvoices(
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/generate-replacement-draft-invoices`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() }
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.getAbsences
*/
export async function getAbsences(
  request: {
    childId: PersonId,
    date: LocalDate
  }
): Promise<Absence[]> {
  try {
    const params = createUrlSearchParams(
      ['childId', request.childId],
      ['date', request.date.formatIso()]
    )
    const { data: json } = await devClient.request<JsonOf<Absence[]>>({
      url: uri`/absences`.toString(),
      method: 'GET',
      params
    })
    return json.map(e => deserializeJsonAbsence(e))
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.getApplication
*/
export async function getApplication(
  request: {
    applicationId: ApplicationId
  }
): Promise<ApplicationDetails> {
  try {
    const { data: json } = await devClient.request<JsonOf<ApplicationDetails>>({
      url: uri`/applications/${request.applicationId}`.toString(),
      method: 'GET'
    })
    return deserializeJsonApplicationDetails(json)
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.getApplicationDecisions
*/
export async function getApplicationDecisions(
  request: {
    applicationId: ApplicationId
  }
): Promise<Decision[]> {
  try {
    const { data: json } = await devClient.request<JsonOf<Decision[]>>({
      url: uri`/applications/${request.applicationId}/decisions`.toString(),
      method: 'GET'
    })
    return json.map(e => deserializeJsonDecision(e))
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.getMessages
*/
export async function getMessages(): Promise<SfiMessage[]> {
  try {
    const { data: json } = await devClient.request<JsonOf<SfiMessage[]>>({
      url: uri`/messages`.toString(),
      method: 'GET'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.getNekkuSpecialDietChoices
*/
export async function getNekkuSpecialDietChoices(
  request: {
    childId: PersonId
  }
): Promise<NekkuSpecialDietChoices[]> {
  try {
    const { data: json } = await devClient.request<JsonOf<NekkuSpecialDietChoices[]>>({
      url: uri`/nekku-special-diet-choices/${request.childId}`.toString(),
      method: 'GET'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.getSentEmails
*/
export async function getSentEmails(): Promise<Email[]> {
  try {
    const { data: json } = await devClient.request<JsonOf<Email[]>>({
      url: uri`/emails`.toString(),
      method: 'GET'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.getStaffAttendances
*/
export async function getStaffAttendances(): Promise<StaffMemberAttendance[]> {
  try {
    const { data: json } = await devClient.request<JsonOf<StaffMemberAttendance[]>>({
      url: uri`/realtime-staff-attendance`.toString(),
      method: 'GET'
    })
    return json.map(e => deserializeJsonStaffMemberAttendance(e))
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.insertChild
*/
export async function insertChild(
  request: {
    body: DevPerson
  }
): Promise<PersonId> {
  try {
    const { data: json } = await devClient.request<JsonOf<PersonId>>({
      url: uri`/child`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevPerson>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.insertGuardians
*/
export async function insertGuardians(
  request: {
    body: DevGuardian[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/guardian`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevGuardian[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.postAttendances
*/
export async function postAttendances(
  request: {
    body: DevChildAttendance[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/attendances`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevChildAttendance[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.postChildDailyNote
*/
export async function postChildDailyNote(
  request: {
    childId: PersonId,
    body: ChildDailyNoteBody
  }
): Promise<ChildDailyNoteId> {
  try {
    const { data: json } = await devClient.request<JsonOf<ChildDailyNoteId>>({
      url: uri`/children/${request.childId}/child-daily-notes`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<ChildDailyNoteBody>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.postChildStickyNote
*/
export async function postChildStickyNote(
  request: {
    childId: PersonId,
    body: ChildStickyNoteBody
  }
): Promise<ChildStickyNoteId> {
  try {
    const { data: json } = await devClient.request<JsonOf<ChildStickyNoteId>>({
      url: uri`/children/${request.childId}/child-sticky-notes`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<ChildStickyNoteBody>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.postGroupNote
*/
export async function postGroupNote(
  request: {
    groupId: GroupId,
    body: GroupNoteBody
  }
): Promise<GroupNoteId> {
  try {
    const { data: json } = await devClient.request<JsonOf<GroupNoteId>>({
      url: uri`/daycare-groups/${request.groupId}/group-notes`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<GroupNoteBody>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.postMobileDevice
*/
export async function postMobileDevice(
  request: {
    body: DevMobileDevice
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/mobile/devices`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevMobileDevice>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.postPairing
*/
export async function postPairing(
  request: {
    body: PostPairingReq
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<Pairing> {
  try {
    const { data: json } = await devClient.request<JsonOf<Pairing>>({
      url: uri`/mobile/pairings`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<PostPairingReq>
    })
    return deserializeJsonPairing(json)
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.postPairingChallenge
*/
export async function postPairingChallenge(
  request: {
    body: PostPairingChallengeReq
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<Pairing> {
  try {
    const { data: json } = await devClient.request<JsonOf<Pairing>>({
      url: uri`/mobile/pairings/challenge`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<PostPairingChallengeReq>
    })
    return deserializeJsonPairing(json)
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.postPairingResponse
*/
export async function postPairingResponse(
  request: {
    id: PairingId,
    body: PostPairingResponseReq
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<Pairing> {
  try {
    const { data: json } = await devClient.request<JsonOf<Pairing>>({
      url: uri`/mobile/pairings/${request.id}/response`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<PostPairingResponseReq>
    })
    return deserializeJsonPairing(json)
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.postPersonalMobileDevice
*/
export async function postPersonalMobileDevice(
  request: {
    body: DevPersonalMobileDevice
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/mobile/personal-devices`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevPersonalMobileDevice>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.postReservations
*/
export async function postReservations(
  request: {
    body: DailyReservationRequest[]
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/reservations`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<DailyReservationRequest[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.postReservationsRaw
*/
export async function postReservationsRaw(
  request: {
    body: ReservationInsert[]
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/reservations/raw`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<ReservationInsert[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.putDiets
*/
export async function putDiets(
  request: {
    body: SpecialDiet[]
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/diets`.toString(),
      method: 'PUT',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<SpecialDiet[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.putDigitransitAutocomplete
*/
export async function putDigitransitAutocomplete(
  request: {
    body: Autocomplete
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/digitransit/autocomplete`.toString(),
      method: 'PUT',
      data: request.body satisfies JsonCompatible<Autocomplete>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.rejectDecisionByCitizen
*/
export async function rejectDecisionByCitizen(
  request: {
    id: DecisionId
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/decisions/${request.id}/actions/reject-by-citizen`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() }
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.resetServiceState
*/
export async function resetServiceState(
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/reset-service-state`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() }
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.runJobs
*/
export async function runJobs(
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/run-jobs`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() }
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.setPersonEmail
*/
export async function setPersonEmail(
  request: {
    body: DevPersonEmail
  }
): Promise<number> {
  try {
    const { data: json } = await devClient.request<JsonOf<number>>({
      url: uri`/person-email`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevPersonEmail>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.setTestMode
*/
export async function setTestMode(
  request: {
    enabled: boolean
  }
): Promise<void> {
  try {
    const params = createUrlSearchParams(
      ['enabled', request.enabled.toString()]
    )
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/test-mode`.toString(),
      method: 'POST',
      params
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.simpleAction
*/
export async function simpleAction(
  request: {
    applicationId: ApplicationId,
    action: SimpleApplicationAction
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/applications/${request.applicationId}/actions/${request.action}`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() }
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.terminatePlacement
*/
export async function terminatePlacement(
  request: {
    body: DevTerminatePlacementRequest
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/placement/terminate`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevTerminatePlacementRequest>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.updateIncomeStatementHandled
*/
export async function updateIncomeStatementHandled(
  request: {
    body: UpdateIncomeStatementHandledBody
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/income-statement/update-handled`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<UpdateIncomeStatementHandledBody>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.upsertPasswordBlacklist
*/
export async function upsertPasswordBlacklist(
  request: {
    body: string[]
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/password-blacklist`.toString(),
      method: 'PUT',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<string[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.upsertStaffOccupancyCoefficient
*/
export async function upsertStaffOccupancyCoefficient(
  request: {
    body: DevUpsertStaffOccupancyCoefficient
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/occupancy-coefficient`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevUpsertStaffOccupancyCoefficient>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.upsertVtjDataset
*/
export async function upsertVtjDataset(
  request: {
    body: MockVtjDataset
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/vtj-persons`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<MockVtjDataset>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.upsertWeakCredentials
*/
export async function upsertWeakCredentials(
  request: {
    id: PersonId,
    body: UpdateWeakLoginCredentialsRequest
  },
  options?: { mockedTime?: HelsinkiDateTime }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/citizen/${request.id}/weak-credentials`.toString(),
      method: 'POST',
      headers: { EvakaMockedTime: options?.mockedTime?.formatIso() },
      data: request.body satisfies JsonCompatible<UpdateWeakLoginCredentialsRequest>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}
