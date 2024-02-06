// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  ApplicationDetails,
  deserializeApplicationDetails
} from 'lib-common/api-types/application/ApplicationDetails'
import {
  deserializeClubTerm,
  deserializePreschoolTerm
} from 'lib-common/api-types/units/terms'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  ApplicationNoteResponse,
  ApplicationSummary,
  ApplicationType,
  ApplicationSortColumn,
  PlacementProposalConfirmationUpdate,
  PagedApplicationSummaries
} from 'lib-common/generated/api-types/application'
import { ClubTerm, PreschoolTerm } from 'lib-common/generated/api-types/daycare'
import { CreatePersonBody } from 'lib-common/generated/api-types/pis'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { SearchOrder } from '../types'
import {
  ApplicationResponse,
  ApplicationSearchParams
} from '../types/application'
import { DaycarePlacementPlan, PlacementDraft } from '../types/placementdraft'

import { client } from './client'

export async function getApplication(
  id: UUID
): Promise<Result<ApplicationResponse>> {
  return client
    .get<JsonOf<ApplicationResponse>>(`/v2/applications/${id}`)
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      application: deserializeApplicationDetails(data.application),
      decisions: data.decisions.map((decision) => ({
        ...decision,
        startDate: LocalDate.parseIso(decision.startDate),
        endDate: LocalDate.parseIso(decision.endDate),
        sentDate: LocalDate.parseNullableIso(decision.sentDate),
        requestedStartDate: LocalDate.parseNullableIso(
          decision.requestedStartDate
        ),
        resolved: LocalDate.parseNullableIso(decision.resolved)
      })),
      guardians: data.guardians.map((guardian) => ({
        ...guardian,
        dateOfBirth: LocalDate.parseIso(guardian.dateOfBirth),
        dateOfDeath: LocalDate.parseNullableIso(guardian.dateOfDeath),
        updatedFromVtj: guardian.updatedFromVtj
          ? HelsinkiDateTime.parseIso(guardian.updatedFromVtj)
          : null
      })),
      attachments: data.attachments.map((attachment) => ({
        ...attachment,
        updated: HelsinkiDateTime.parseIso(attachment.updated),
        receivedAt: HelsinkiDateTime.parseIso(attachment.receivedAt)
      })),
      permittedActions: new Set(data.permittedActions)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export const deserializeApplicationSummary = (
  json: JsonOf<ApplicationSummary>
): ApplicationSummary => ({
  ...json,
  dateOfBirth: LocalDate.parseNullableIso(json.dateOfBirth),
  dueDate: LocalDate.parseNullableIso(json.dueDate),
  startDate: LocalDate.parseNullableIso(json.startDate),
  placementPlanStartDate: LocalDate.parseNullableIso(
    json.placementPlanStartDate
  )
})

export async function getApplications(
  page: number,
  pageSize: number,
  sortBy: ApplicationSortColumn,
  sortDir: SearchOrder,
  params: ApplicationSearchParams
): Promise<Result<PagedApplicationSummaries>> {
  return client
    .post<JsonOf<PagedApplicationSummaries>>('v2/applications/search', {
      page: page,
      pageSize,
      sortBy,
      sortDir,
      ...params
    })
    .then(({ data }) => ({
      ...data,
      data: data.data.map(deserializeApplicationSummary)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function updateApplication({
  dueDate,
  form,
  id
}: ApplicationDetails): Promise<Result<void>> {
  return client
    .put(`v2/applications/${id}`, { form, dueDate })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function createPaperApplication(
  request: PaperApplicationRequest
): Promise<Result<UUID>> {
  return client
    .post<UUID>('v2/applications', {
      ...request,
      type: request.type,
      sentDate: request.sentDate.formatIso()
    })
    .then((v) => Success.of(v.data))
    .catch((e) => Failure.fromError(e))
}

export interface PaperApplicationRequest {
  childId: UUID
  type: ApplicationType
  sentDate: LocalDate
  hideFromGuardian: boolean
  guardianId?: UUID
  guardianSsn?: string
  guardianToBeCreated?: CreatePersonBody
}

export async function sendApplication(
  applicationId: UUID
): Promise<Result<void>> {
  return client
    .post(`/v2/applications/${applicationId}/actions/send-application`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function moveToWaitingPlacement(
  applicationId: UUID
): Promise<void> {
  return client.post(
    `/v2/applications/${applicationId}/actions/move-to-waiting-placement`
  )
}

export async function returnToSent(applicationId: UUID): Promise<void> {
  return client.post(`/v2/applications/${applicationId}/actions/return-to-sent`)
}

export async function cancelApplication(applicationId: UUID): Promise<void> {
  return client.post(
    `/v2/applications/${applicationId}/actions/cancel-application`
  )
}

export async function setVerified(applicationId: UUID): Promise<void> {
  return client.post(`/v2/applications/${applicationId}/actions/set-verified`)
}

export async function setUnverified(applicationId: UUID): Promise<void> {
  return client.post(`/v2/applications/${applicationId}/actions/set-unverified`)
}

export async function cancelPlacementPlan(applicationId: UUID): Promise<void> {
  return client.post(
    `/v2/applications/${applicationId}/actions/cancel-placement-plan`
  )
}

export async function sendDecisionsWithoutProposal(
  applicationId: UUID
): Promise<void> {
  return client.post(
    `/v2/applications/${applicationId}/actions/send-decisions-without-proposal`
  )
}

export async function sendPlacementProposal(
  applicationId: UUID
): Promise<void> {
  return client.post(
    `/v2/applications/${applicationId}/actions/send-placement-proposal`
  )
}

export async function withdrawPlacementProposal(
  applicationId: UUID
): Promise<void> {
  return client.post(
    `/v2/applications/${applicationId}/actions/withdraw-placement-proposal`
  )
}

export type RespondToPlacementProposal = {
  applicationId: UUID
} & PlacementProposalConfirmationUpdate

export async function respondToPlacementProposal({
  applicationId,
  status,
  reason,
  otherReason
}: RespondToPlacementProposal): Promise<void> {
  return client
    .post(
      `/v2/applications/${applicationId}/actions/respond-to-placement-proposal`,
      {
        status,
        reason,
        otherReason
      }
    )
    .then(() => undefined)
}

export async function confirmDecisionMailed(
  applicationId: UUID
): Promise<void> {
  return client.post(
    `/v2/applications/${applicationId}/actions/confirm-decision-mailed`
  )
}

export function getPlacementDraft(id: UUID): Promise<Result<PlacementDraft>> {
  return client
    .get<JsonOf<PlacementDraft>>(`/v2/applications/${id}/placement-draft`)
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      child: {
        ...data.child,
        dob: LocalDate.parseIso(data.child.dob)
      },
      period: FiniteDateRange.parseJson(data.period),
      preschoolDaycarePeriod: data.preschoolDaycarePeriod
        ? FiniteDateRange.parseJson(data.preschoolDaycarePeriod)
        : undefined,
      placements: data.placements.map((placement) => ({
        ...placement,
        startDate: LocalDate.parseIso(placement.startDate),
        endDate: LocalDate.parseIso(placement.endDate)
      }))
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function createPlacementPlan(
  applicationId: UUID,
  placementPlan: DaycarePlacementPlan
): Promise<Result<void>> {
  return client
    .post(
      `/v2/applications/${applicationId}/actions/create-placement-plan`,
      placementPlan
    )
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function acceptPlacementProposal(unitId: UUID): Promise<void> {
  return client
    .post(`/v2/applications/placement-proposals/${unitId}/accept`)
    .then(() => undefined)
}

export async function acceptDecision(
  applicationId: UUID,
  decisionId: UUID,
  requestedStartDate: LocalDate
): Promise<Result<void>> {
  return client
    .post(`/v2/applications/${applicationId}/actions/accept-decision`, {
      decisionId,
      requestedStartDate
    })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function rejectDecision(
  applicationId: UUID,
  decisionId: UUID
): Promise<Result<void>> {
  return client
    .post(`/v2/applications/${applicationId}/actions/reject-decision`, {
      decisionId
    })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function batchMoveToWaitingPlacement(
  applicationIds: UUID[]
): Promise<void> {
  return client.post(
    '/v2/applications/batch/actions/move-to-waiting-placement',
    { applicationIds }
  )
}

export async function batchReturnToSent(applicationIds: UUID[]): Promise<void> {
  return client.post('/v2/applications/batch/actions/return-to-sent', {
    applicationIds
  })
}

export async function batchCancelPlacementPlan(
  applicationIds: UUID[]
): Promise<void> {
  return client.post('/v2/applications/batch/actions/cancel-placement-plan', {
    applicationIds
  })
}

export async function batchSendDecisionsWithoutProposal(
  applicationIds: UUID[]
): Promise<void> {
  return client.post(
    '/v2/applications/batch/actions/send-decisions-without-proposal',
    { applicationIds }
  )
}

export async function batchSendPlacementProposal(
  applicationIds: UUID[]
): Promise<void> {
  return client.post('/v2/applications/batch/actions/send-placement-proposal', {
    applicationIds
  })
}

export async function batchWithdrawPlacementProposal(
  applicationIds: UUID[]
): Promise<void> {
  return client.post(
    '/v2/applications/batch/actions/withdraw-placement-proposal',
    { applicationIds }
  )
}

export async function getApplicationNotes(
  applicationId: UUID
): Promise<ApplicationNoteResponse[]> {
  return client
    .get<
      JsonOf<ApplicationNoteResponse[]>
    >(`/note/application/${applicationId}`)
    .then((res) =>
      res.data.map(({ note, permittedActions }) => ({
        note: {
          ...note,
          created: HelsinkiDateTime.parseIso(note.created),
          updated: HelsinkiDateTime.parseIso(note.updated)
        },
        permittedActions
      }))
    )
}

export async function createNote(
  applicationId: UUID,
  text: string
): Promise<void> {
  return client.post(`/note/application/${applicationId}`, { text })
}

export async function updateNote(id: UUID, text: string): Promise<void> {
  return client.put(`/note/${id}`, { text })
}

export async function deleteNote(id: UUID): Promise<void> {
  return client.delete(`/note/${id}`)
}

export async function updateServiceWorkerNote(
  applicationId: UUID,
  text: string
): Promise<Result<void>> {
  return client
    .put(`/note/service-worker/application/${applicationId}`, {
      text
    })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function getClubTerms(): Promise<Result<ClubTerm[]>> {
  try {
    const result = await client.get<JsonOf<ClubTerm[]>>(`/public/club-terms`)
    return Success.of(result.data.map(deserializeClubTerm))
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function getPreschoolTerms(): Promise<Result<PreschoolTerm[]>> {
  try {
    const result = await getPreschoolTermsResult()
    return Success.of(result)
  } catch (e) {
    return Failure.fromError(e)
  }
}

// For query client use (to be refactored and remove V2 from name)
export async function getPreschoolTermsResult(): Promise<PreschoolTerm[]> {
  return client
    .get<JsonOf<PreschoolTerm[]>>(`/public/preschool-terms`)
    .then((res) => res.data.map(deserializePreschoolTerm))
}
