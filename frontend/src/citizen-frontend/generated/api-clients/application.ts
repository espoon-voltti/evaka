// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AcceptDecisionRequest } from 'lib-common/generated/api-types/application'
import { ApplicationDecisions } from 'lib-common/generated/api-types/application'
import { ApplicationDetails } from 'lib-common/generated/api-types/application'
import { ApplicationFormUpdate } from 'lib-common/generated/api-types/application'
import { ApplicationType } from 'lib-common/generated/api-types/application'
import { ApplicationsOfChild } from 'lib-common/generated/api-types/application'
import { AxiosHeaders } from 'axios'
import { CitizenApplicationUpdate } from 'lib-common/generated/api-types/application'
import { CitizenChildren } from 'lib-common/generated/api-types/application'
import { CreateApplicationBody } from 'lib-common/generated/api-types/application'
import { DecisionWithValidStartDatePeriod } from 'lib-common/generated/api-types/application'
import { FinanceDecisionCitizenInfo } from 'lib-common/generated/api-types/application'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { RejectDecisionRequest } from 'lib-common/generated/api-types/application'
import { UUID } from 'lib-common/types'
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
    applicationId: UUID,
    body: AcceptDecisionRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/applications/${request.applicationId}/actions/accept-decision`.toString(),
    method: 'POST',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/citizen/applications`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<CreateApplicationBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.deleteOrCancelUnprocessedApplication
*/
export async function deleteOrCancelUnprocessedApplication(
  request: {
    applicationId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/applications/${request.applicationId}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getApplication
*/
export async function getApplication(
  request: {
    applicationId: UUID
  },
  headers?: AxiosHeaders
): Promise<ApplicationDetails> {
  const { data: json } = await client.request<JsonOf<ApplicationDetails>>({
    url: uri`/citizen/applications/${request.applicationId}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonApplicationDetails(json)
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getApplicationChildren
*/
export async function getApplicationChildren(
  headers?: AxiosHeaders
): Promise<CitizenChildren[]> {
  const { data: json } = await client.request<JsonOf<CitizenChildren[]>>({
    url: uri`/citizen/applications/children`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonCitizenChildren(e))
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getApplicationDecisions
*/
export async function getApplicationDecisions(
  request: {
    applicationId: UUID
  },
  headers?: AxiosHeaders
): Promise<DecisionWithValidStartDatePeriod[]> {
  const { data: json } = await client.request<JsonOf<DecisionWithValidStartDatePeriod[]>>({
    url: uri`/citizen/applications/${request.applicationId}/decisions`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonDecisionWithValidStartDatePeriod(e))
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getChildDuplicateApplications
*/
export async function getChildDuplicateApplications(
  request: {
    childId: UUID
  },
  headers?: AxiosHeaders
): Promise<Record<ApplicationType, boolean>> {
  const { data: json } = await client.request<JsonOf<Record<ApplicationType, boolean>>>({
    url: uri`/citizen/applications/duplicates/${request.childId}`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getChildPlacementStatusByApplicationType
*/
export async function getChildPlacementStatusByApplicationType(
  request: {
    childId: UUID
  },
  headers?: AxiosHeaders
): Promise<Record<ApplicationType, boolean>> {
  const { data: json } = await client.request<JsonOf<Record<ApplicationType, boolean>>>({
    url: uri`/citizen/applications/active-placements/${request.childId}`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getDecisions
*/
export async function getDecisions(
  headers?: AxiosHeaders
): Promise<ApplicationDecisions> {
  const { data: json } = await client.request<JsonOf<ApplicationDecisions>>({
    url: uri`/citizen/decisions`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonApplicationDecisions(json)
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getGuardianApplicationNotifications
*/
export async function getGuardianApplicationNotifications(
  headers?: AxiosHeaders
): Promise<number> {
  const { data: json } = await client.request<JsonOf<number>>({
    url: uri`/citizen/applications/by-guardian/notifications`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getGuardianApplications
*/
export async function getGuardianApplications(
  headers?: AxiosHeaders
): Promise<ApplicationsOfChild[]> {
  const { data: json } = await client.request<JsonOf<ApplicationsOfChild[]>>({
    url: uri`/citizen/applications/by-guardian`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonApplicationsOfChild(e))
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.getLiableCitizenFinanceDecisions
*/
export async function getLiableCitizenFinanceDecisions(
  headers?: AxiosHeaders
): Promise<FinanceDecisionCitizenInfo[]> {
  const { data: json } = await client.request<JsonOf<FinanceDecisionCitizenInfo[]>>({
    url: uri`/citizen/finance-decisions/by-liable-citizen`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonFinanceDecisionCitizenInfo(e))
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.rejectDecision
*/
export async function rejectDecision(
  request: {
    applicationId: UUID,
    body: RejectDecisionRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/applications/${request.applicationId}/actions/reject-decision`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<RejectDecisionRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.saveApplicationAsDraft
*/
export async function saveApplicationAsDraft(
  request: {
    applicationId: UUID,
    body: ApplicationFormUpdate
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/applications/${request.applicationId}/draft`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<ApplicationFormUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.sendApplication
*/
export async function sendApplication(
  request: {
    applicationId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/applications/${request.applicationId}/actions/send-application`.toString(),
    method: 'POST',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.updateApplication
*/
export async function updateApplication(
  request: {
    applicationId: UUID,
    body: CitizenApplicationUpdate
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/applications/${request.applicationId}`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<CitizenApplicationUpdate>
  })
  return json
}
