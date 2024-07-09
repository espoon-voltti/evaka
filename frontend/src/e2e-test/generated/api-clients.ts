// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { Absence } from 'lib-common/generated/api-types/absence'
import { ApplicationDetails } from 'lib-common/generated/api-types/application'
import { Autocomplete } from './api-types'
import { Caretaker } from './api-types'
import { ChildDailyNoteBody } from 'lib-common/generated/api-types/note'
import { ChildStickyNoteBody } from 'lib-common/generated/api-types/note'
import { Citizen } from './api-types'
import { CreateVasuTemplateBody } from './api-types'
import { DailyReservationRequest } from 'lib-common/generated/api-types/reservations'
import { DaycareAclInsert } from './api-types'
import { DaycarePlacementPlan } from 'lib-common/generated/api-types/application'
import { Decision } from 'lib-common/generated/api-types/decision'
import { DecisionRequest } from './api-types'
import { DevAbsence } from './api-types'
import { DevApiError } from '../dev-api'
import { DevApplicationWithForm } from './api-types'
import { DevAssistanceAction } from './api-types'
import { DevAssistanceActionOption } from './api-types'
import { DevAssistanceFactor } from './api-types'
import { DevAssistanceNeedDecision } from './api-types'
import { DevAssistanceNeedPreschoolDecision } from './api-types'
import { DevAssistanceNeedVoucherCoefficient } from './api-types'
import { DevBackupCare } from './api-types'
import { DevBackupPickup } from './api-types'
import { DevCalendarEvent } from './api-types'
import { DevCalendarEventAttendee } from './api-types'
import { DevCalendarEventTime } from './api-types'
import { DevCareArea } from './api-types'
import { DevChild } from './api-types'
import { DevChildAttendance } from './api-types'
import { DevChildDocument } from './api-types'
import { DevClubTerm } from './api-types'
import { DevCreateIncomeStatements } from './api-types'
import { DevDailyServiceTimeNotification } from './api-types'
import { DevDailyServiceTimes } from './api-types'
import { DevDaycare } from './api-types'
import { DevDaycareAssistance } from './api-types'
import { DevDaycareGroup } from './api-types'
import { DevDaycareGroupAcl } from './api-types'
import { DevDaycareGroupPlacement } from './api-types'
import { DevDocumentTemplate } from './api-types'
import { DevEmployee } from './api-types'
import { DevEmployeePin } from './api-types'
import { DevFamilyContact } from './api-types'
import { DevFosterParent } from './api-types'
import { DevFridgeChild } from './api-types'
import { DevFridgePartner } from './api-types'
import { DevGuardian } from './api-types'
import { DevHoliday } from './api-types'
import { DevIncome } from './api-types'
import { DevMobileDevice } from './api-types'
import { DevOtherAssistanceMeasure } from './api-types'
import { DevParentship } from './api-types'
import { DevPayment } from './api-types'
import { DevPedagogicalDocument } from './api-types'
import { DevPerson } from './api-types'
import { DevPersonEmail } from './api-types'
import { DevPersonType } from './api-types'
import { DevPersonalMobileDevice } from './api-types'
import { DevPlacement } from './api-types'
import { DevPreschoolAssistance } from './api-types'
import { DevPreschoolTerm } from './api-types'
import { DevServiceNeed } from './api-types'
import { DevStaffAttendance } from './api-types'
import { DevStaffAttendancePlan } from './api-types'
import { DevTerminatePlacementRequest } from './api-types'
import { DevUpsertStaffOccupancyCoefficient } from './api-types'
import { DevVardaReset } from './api-types'
import { DevVardaServiceNeed } from './api-types'
import { Email } from './api-types'
import { Employee } from 'lib-common/generated/api-types/pis'
import { FeeDecision } from 'lib-common/generated/api-types/invoicing'
import { FeeThresholds } from 'lib-common/generated/api-types/invoicing'
import { FixedPeriodQuestionnaireBody } from 'lib-common/generated/api-types/holidayperiod'
import { GroupNoteBody } from 'lib-common/generated/api-types/note'
import { HolidayPeriodBody } from 'lib-common/generated/api-types/holidayperiod'
import { IncomeNotification } from 'lib-common/generated/api-types/invoicing'
import { Invoice } from 'lib-common/generated/api-types/invoicing'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { MockVtjDataset } from './api-types'
import { Pairing } from 'lib-common/generated/api-types/pairing'
import { PlacementPlan } from './api-types'
import { PostPairingChallengeReq } from 'lib-common/generated/api-types/pairing'
import { PostPairingReq } from 'lib-common/generated/api-types/pairing'
import { PostPairingResponseReq } from 'lib-common/generated/api-types/pairing'
import { PostVasuDocBody } from './api-types'
import { ReservationInsert } from './api-types'
import { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import { SfiMessage } from './api-types'
import { SpecialDiet } from 'lib-common/generated/api-types/specialdiet'
import { StaffMemberAttendance } from 'lib-common/generated/api-types/attendance'
import { UUID } from 'lib-common/types'
import { VoucherValueDecision } from './api-types'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonAbsence } from 'lib-common/generated/api-types/absence'
import { deserializeJsonApplicationDetails } from 'lib-common/generated/api-types/application'
import { deserializeJsonDecision } from 'lib-common/generated/api-types/decision'
import { deserializeJsonEmployee } from 'lib-common/generated/api-types/pis'
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
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
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
    daycareId: UUID,
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
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
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
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
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
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
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
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
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
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
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
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
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
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
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
    applicationId: UUID,
    body: DaycarePlacementPlan
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/applications/${request.applicationId}/actions/create-placement-plan`.toString(),
      method: 'POST',
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
  }
): Promise<UUID[]> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID[]>>({
      url: uri`/applications`.toString(),
      method: 'POST',
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
  }
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
      url: uri`/child-documents`.toString(),
      method: 'POST',
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
    id: UUID
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
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/decisions`.toString(),
      method: 'POST',
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
    applicationId: UUID
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/applications/${request.applicationId}/actions/create-default-placement-plan`.toString(),
      method: 'POST'
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
  }
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
      url: uri`/document-templates`.toString(),
      method: 'POST',
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
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
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
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
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
* Generated from fi.espoo.evaka.shared.dev.DevApi.createHoliday
*/
export async function createHoliday(
  request: {
    body: DevHoliday
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/holiday`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevHoliday>
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
    id: UUID,
    body: HolidayPeriodBody
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/holiday-period/${request.id}`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<HolidayPeriodBody>
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
    id: UUID,
    body: FixedPeriodQuestionnaireBody
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/holiday-period/questionnaire/${request.id}`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<FixedPeriodQuestionnaireBody>
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
* Generated from fi.espoo.evaka.shared.dev.DevApi.createIncomeStatements
*/
export async function createIncomeStatements(
  request: {
    body: DevCreateIncomeStatements
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/income-statements`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevCreateIncomeStatements>
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
    body: Invoice[]
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/invoices`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<Invoice[]>
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
): Promise<UUID> {
  try {
    const params = createUrlSearchParams(
      ['type', request.type.toString()]
    )
    const { data: json } = await devClient.request<JsonOf<UUID>>({
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
    applicationId: UUID,
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
* Generated from fi.espoo.evaka.shared.dev.DevApi.createVardaReset
*/
export async function createVardaReset(
  request: {
    body: DevVardaReset
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/varda/reset-child`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevVardaReset>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createVardaServiceNeed
*/
export async function createVardaServiceNeed(
  request: {
    body: DevVardaServiceNeed
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/varda/varda-service-need`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevVardaServiceNeed>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createVasuDocument
*/
export async function createVasuDocument(
  request: {
    body: PostVasuDocBody
  }
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
      url: uri`/vasu/doc`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<PostVasuDocBody>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.createVasuTemplate
*/
export async function createVasuTemplate(
  request: {
    body: CreateVasuTemplateBody
  }
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
      url: uri`/vasu/template`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<CreateVasuTemplateBody>
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
    daycareId: UUID
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
* Generated from fi.espoo.evaka.shared.dev.DevApi.deleteVasuTemplates
*/
export async function deleteVasuTemplates(): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/vasu/templates`.toString(),
      method: 'DELETE'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.digitransitAutocomplete
*/
export async function digitransitAutocomplete(): Promise<Autocomplete> {
  try {
    const { data: json } = await devClient.request<JsonOf<Autocomplete>>({
      url: uri`/digitransit/autocomplete`.toString(),
      method: 'GET'
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
    person: UUID
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
* Generated from fi.espoo.evaka.shared.dev.DevApi.getAbsences
*/
export async function getAbsences(
  request: {
    childId: UUID,
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
    applicationId: UUID
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
    applicationId: UUID
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
* Generated from fi.espoo.evaka.shared.dev.DevApi.getCitizen
*/
export async function getCitizen(
  request: {
    ssn: string
  }
): Promise<Citizen> {
  try {
    const { data: json } = await devClient.request<JsonOf<Citizen>>({
      url: uri`/citizen/ssn/${request.ssn}`.toString(),
      method: 'GET'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.getCitizens
*/
export async function getCitizens(): Promise<Citizen[]> {
  try {
    const { data: json } = await devClient.request<JsonOf<Citizen[]>>({
      url: uri`/citizen`.toString(),
      method: 'GET'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.getEmployees
*/
export async function getEmployees(): Promise<Employee[]> {
  try {
    const { data: json } = await devClient.request<JsonOf<Employee[]>>({
      url: uri`/employee`.toString(),
      method: 'GET'
    })
    return json.map(e => deserializeJsonEmployee(e))
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
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
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
    childId: UUID,
    body: ChildDailyNoteBody
  }
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
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
    childId: UUID,
    body: ChildStickyNoteBody
  }
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
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
* Generated from fi.espoo.evaka.shared.dev.DevApi.postDigitransitQuery
*/
export async function postDigitransitQuery(
  request: {
    body: string
  }
): Promise<string> {
  try {
    const { data: json } = await devClient.request<JsonOf<string>>({
      url: uri`/digitransit/query`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<string>
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
    groupId: UUID,
    body: GroupNoteBody
  }
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
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
  }
): Promise<Pairing> {
  try {
    const { data: json } = await devClient.request<JsonOf<Pairing>>({
      url: uri`/mobile/pairings`.toString(),
      method: 'POST',
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
  }
): Promise<Pairing> {
  try {
    const { data: json } = await devClient.request<JsonOf<Pairing>>({
      url: uri`/mobile/pairings/challenge`.toString(),
      method: 'POST',
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
    id: UUID,
    body: PostPairingResponseReq
  }
): Promise<Pairing> {
  try {
    const { data: json } = await devClient.request<JsonOf<Pairing>>({
      url: uri`/mobile/pairings/${request.id}/response`.toString(),
      method: 'POST',
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
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/reservations`.toString(),
      method: 'POST',
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
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/reservations/raw`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<ReservationInsert[]>
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.publishVasuDocument
*/
export async function publishVasuDocument(
  request: {
    documentId: UUID
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/vasu/doc/publish/${request.documentId}`.toString(),
      method: 'POST'
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
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/diets`.toString(),
      method: 'PUT',
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
    id: UUID
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/decisions/${request.id}/actions/reject-by-citizen`.toString(),
      method: 'POST'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.resetServiceState
*/
export async function resetServiceState(): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/reset-service-state`.toString(),
      method: 'POST'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.revokeSharingPermission
*/
export async function revokeSharingPermission(
  request: {
    docId: UUID
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/vasu/revokeSharingPermission/${request.docId}`.toString(),
      method: 'POST'
    })
    return json
  } catch (e) {
    throw new DevApiError(e)
  }
}


/**
* Generated from fi.espoo.evaka.shared.dev.DevApi.runJobs
*/
export async function runJobs(): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/run-jobs`.toString(),
      method: 'POST'
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
    applicationId: UUID,
    action: string
  }
): Promise<void> {
  try {
    const { data: json } = await devClient.request<JsonOf<void>>({
      url: uri`/applications/${request.applicationId}/actions/${request.action}`.toString(),
      method: 'POST'
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
* Generated from fi.espoo.evaka.shared.dev.DevApi.upsertPerson
*/
export async function upsertPerson(
  request: {
    body: DevPerson
  }
): Promise<UUID> {
  try {
    const { data: json } = await devClient.request<JsonOf<UUID>>({
      url: uri`/person`.toString(),
      method: 'POST',
      data: request.body satisfies JsonCompatible<DevPerson>
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
