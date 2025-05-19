// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { AcceptDecisionRequest } from 'lib-common/generated/api-types/application'
import type { ApplicationDecisions } from 'lib-common/generated/api-types/application'
import type { ApplicationDetails } from 'lib-common/generated/api-types/application'
import type { ApplicationFormUpdate } from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import type { ApplicationType } from 'lib-common/generated/api-types/application'
import type { ApplicationsOfChild } from 'lib-common/generated/api-types/application'
import type { CitizenApplicationUpdate } from 'lib-common/generated/api-types/application'
import type { CitizenChildren } from 'lib-common/generated/api-types/application'
import type { CreateApplicationBody } from 'lib-common/generated/api-types/application'
import type { DecisionId } from 'lib-common/generated/api-types/shared'
import type { DecisionWithValidStartDatePeriod } from 'lib-common/generated/api-types/application'
import type { FeeDecisionId } from 'lib-common/generated/api-types/shared'
import type { FinanceDecisionCitizenInfo } from 'lib-common/generated/api-types/application'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { RejectDecisionRequest } from 'lib-common/generated/api-types/application'
import type { Uri } from 'lib-common/uri'
import type { VoucherValueDecisionId } from 'lib-common/generated/api-types/shared'
import { client } from '../../api-client'
import { deserializeJsonApplicationDecisions } from 'lib-common/generated/api-types/application'
import { deserializeJsonApplicationDetails } from 'lib-common/generated/api-types/application'
import { deserializeJsonApplicationsOfChild } from 'lib-common/generated/api-types/application'
import { deserializeJsonCitizenChildren } from 'lib-common/generated/api-types/application'
import { deserializeJsonDecisionWithValidStartDatePeriod } from 'lib-common/generated/api-types/application'
import { deserializeJsonFinanceDecisionCitizenInfo } from 'lib-common/generated/api-types/application'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.acceptDecision
*/
export async function acceptDecision(
  request: {
    applicationId: ApplicationId,
    body: AcceptDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/applications/${request.applicationId}/actions/accept-decision`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AcceptDecisionRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.createApplication
*/
export async function createApplication(
  request: {
    body: CreateApplicationBody
  }
): Promise<ApplicationId> {
  const { data: json } = await client.request<JsonOf<ApplicationId>>({
    url: uri`/citizen/applications`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CreateApplicationBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.deleteOrCancelUnprocessedApplication
*/
export async function deleteOrCancelUnprocessedApplication(
  request: {
    applicationId: ApplicationId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/applications/${request.applicationId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.downloadDecisionPdf
*/
export function downloadDecisionPdf(
  request: {
    id: DecisionId
  }
): { url: Uri } {
  return {
    url: uri`/citizen/decisions/${request.id}/download`.withBaseUrl(client.defaults.baseURL ?? '')
  }
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.downloadFeeDecisionPdf
*/
export function downloadFeeDecisionPdf(
  request: {
    id: FeeDecisionId
  }
): { url: Uri } {
  return {
    url: uri`/citizen/fee-decisions/${request.id}/download`.withBaseUrl(client.defaults.baseURL ?? '')
  }
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.downloadVoucherValueDecisionPdf
*/
export function downloadVoucherValueDecisionPdf(
  request: {
    id: VoucherValueDecisionId
  }
): { url: Uri } {
  return {
    url: uri`/citizen/voucher-value-decisions/${request.id}/download`.withBaseUrl(client.defaults.baseURL ?? '')
  }
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getApplication
*/
export async function getApplication(
  request: {
    applicationId: ApplicationId
  }
): Promise<ApplicationDetails> {
  const { data: json } = await client.request<JsonOf<ApplicationDetails>>({
    url: uri`/citizen/applications/${request.applicationId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonApplicationDetails(json)
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getApplicationChildren
*/
export async function getApplicationChildren(): Promise<CitizenChildren[]> {
  const { data: json } = await client.request<JsonOf<CitizenChildren[]>>({
    url: uri`/citizen/applications/children`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonCitizenChildren(e))
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getApplicationDecisions
*/
export async function getApplicationDecisions(
  request: {
    applicationId: ApplicationId
  }
): Promise<DecisionWithValidStartDatePeriod[]> {
  const { data: json } = await client.request<JsonOf<DecisionWithValidStartDatePeriod[]>>({
    url: uri`/citizen/applications/${request.applicationId}/decisions`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonDecisionWithValidStartDatePeriod(e))
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getChildDuplicateApplications
*/
export async function getChildDuplicateApplications(
  request: {
    childId: PersonId
  }
): Promise<Partial<Record<ApplicationType, boolean>>> {
  const { data: json } = await client.request<JsonOf<Partial<Record<ApplicationType, boolean>>>>({
    url: uri`/citizen/applications/duplicates/${request.childId}`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getChildPlacementStatusByApplicationType
*/
export async function getChildPlacementStatusByApplicationType(
  request: {
    childId: PersonId
  }
): Promise<Partial<Record<ApplicationType, boolean>>> {
  const { data: json } = await client.request<JsonOf<Partial<Record<ApplicationType, boolean>>>>({
    url: uri`/citizen/applications/active-placements/${request.childId}`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getDecisions
*/
export async function getDecisions(): Promise<ApplicationDecisions> {
  const { data: json } = await client.request<JsonOf<ApplicationDecisions>>({
    url: uri`/citizen/decisions`.toString(),
    method: 'GET'
  })
  return deserializeJsonApplicationDecisions(json)
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getGuardianApplicationNotifications
*/
export async function getGuardianApplicationNotifications(): Promise<number> {
  const { data: json } = await client.request<JsonOf<number>>({
    url: uri`/citizen/applications/by-guardian/notifications`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getGuardianApplications
*/
export async function getGuardianApplications(): Promise<ApplicationsOfChild[]> {
  const { data: json } = await client.request<JsonOf<ApplicationsOfChild[]>>({
    url: uri`/citizen/applications/by-guardian`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonApplicationsOfChild(e))
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getLiableCitizenFinanceDecisions
*/
export async function getLiableCitizenFinanceDecisions(): Promise<FinanceDecisionCitizenInfo[]> {
  const { data: json } = await client.request<JsonOf<FinanceDecisionCitizenInfo[]>>({
    url: uri`/citizen/finance-decisions/by-liable-citizen`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonFinanceDecisionCitizenInfo(e))
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.rejectDecision
*/
export async function rejectDecision(
  request: {
    applicationId: ApplicationId,
    body: RejectDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/applications/${request.applicationId}/actions/reject-decision`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<RejectDecisionRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.saveApplicationAsDraft
*/
export async function saveApplicationAsDraft(
  request: {
    applicationId: ApplicationId,
    body: ApplicationFormUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/applications/${request.applicationId}/draft`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<ApplicationFormUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.sendApplication
*/
export async function sendApplication(
  request: {
    applicationId: ApplicationId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/applications/${request.applicationId}/actions/send-application`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.updateApplication
*/
export async function updateApplication(
  request: {
    applicationId: ApplicationId,
    body: CitizenApplicationUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/applications/${request.applicationId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<CitizenApplicationUpdate>
  })
  return json
}
