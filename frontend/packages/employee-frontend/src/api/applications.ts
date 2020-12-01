// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID, SearchOrder } from 'types'
import {
  ApplicationDetails,
  ApplicationListSummary,
  ApplicationsSearchResponse,
  SortByApplications,
  ApplicationSearchParams,
  ApplicationResponse,
  ApplicationType,
  ApplicationNote
} from 'types/application'
import { Failure, Result, Success } from 'api/index'
import { client } from 'api/client'
import { JsonOf } from '@evaka/lib-common/src/json'
import LocalDate from '@evaka/lib-common/src/local-date'
import { CreatePersonBody } from '~api/person'
import { DaycarePlacementPlan, PlacementDraft } from 'types/placementdraft'
import {
  PlacementPlanConfirmationStatus,
  PlacementPlanRejectReason
} from '~types/unit'

export async function getApplication(
  id: UUID
): Promise<Result<ApplicationResponse>> {
  return client
    .get<JsonOf<ApplicationResponse>>(`/v2/applications/${id}`)
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      application: {
        ...data.application,
        form: {
          ...data.application.form,
          child: {
            ...data.application.form.child,
            dateOfBirth: LocalDate.parseNullableIso(
              data.application.form.child.dateOfBirth
            ),
            futureAddress: data.application.form.child.futureAddress
              ? {
                  ...data.application.form.child.futureAddress,
                  movingDate: LocalDate.parseNullableIso(
                    data.application.form.child.futureAddress.movingDate
                  )
                }
              : null
          },
          guardian: {
            ...data.application.form.guardian,
            futureAddress: data.application.form.guardian.futureAddress
              ? {
                  ...data.application.form.guardian.futureAddress,
                  movingDate: LocalDate.parseNullableIso(
                    data.application.form.guardian.futureAddress.movingDate
                  )
                }
              : null
          },
          secondGuardian: data.application.form.secondGuardian
            ? {
                ...data.application.form.secondGuardian,
                futureAddress: data.application.form.secondGuardian
                  .futureAddress
                  ? {
                      ...data.application.form.secondGuardian.futureAddress,
                      movingDate: LocalDate.parseNullableIso(
                        data.application.form.secondGuardian.futureAddress
                          .movingDate
                      )
                    }
                  : null
              }
            : null,
          preferences: {
            ...data.application.form.preferences,
            preferredStartDate: LocalDate.parseNullableIso(
              data.application.form.preferences.preferredStartDate
            )
          }
        },
        createdDate: data.application.createdDate
          ? new Date(data.application.createdDate)
          : null,
        modifiedDate: data.application.modifiedDate
          ? new Date(data.application.modifiedDate)
          : null,
        sentDate: LocalDate.parseNullableIso(data.application.sentDate),
        dueDate: LocalDate.parseNullableIso(data.application.dueDate)
      },
      decisions: data.decisions.map((decision) => ({
        ...decision,
        startDate: LocalDate.parseIso(decision.startDate),
        endDate: LocalDate.parseIso(decision.endDate),
        sentDate: LocalDate.parseIso(decision.sentDate)
      })),
      guardians: data.guardians.map((guardian) => ({
        ...guardian,
        dateOfBirth: LocalDate.parseIso(guardian.dateOfBirth),
        dateOfDeath: LocalDate.parseNullableIso(guardian.dateOfDeath)
      })),
      attachments: data.attachments.map((attachment) => ({
        ...attachment,
        updated: new Date(attachment.updated)
      }))
    }))
    .then(Success)
    .catch(Failure)
}

export const deserializeApplicationSummary = (
  json: JsonOf<ApplicationListSummary>
): ApplicationListSummary => ({
  ...json,
  dateOfBirth: LocalDate.parseNullableIso(json.dateOfBirth),
  dueDate: LocalDate.parseNullableIso(json.dueDate),
  startDate: LocalDate.parseNullableIso(json.startDate)
})

export async function getApplications(
  page: number,
  pageSize: number,
  sortBy: SortByApplications,
  sortDir: SearchOrder,
  params: ApplicationSearchParams
): Promise<Result<ApplicationsSearchResponse>> {
  return client
    .get<JsonOf<ApplicationsSearchResponse>>('v2/applications', {
      params: { page: page, pageSize, sortBy, sortDir, ...params }
    })
    .then(({ data }) => ({
      ...data,
      data: data.data.map(deserializeApplicationSummary)
    }))
    .then(Success)
    .catch(Failure)
}

export async function updateApplication(
  application: ApplicationDetails
): Promise<void> {
  return client.put(`v2/applications/${application.id}`, application.form)
}

export async function createPaperApplication(
  request: PaperApplicationRequest
): Promise<UUID> {
  return client
    .post<UUID>('v2/applications', {
      ...request,
      type: request.type,
      sentDate: request.sentDate.formatIso()
    })
    .then((res) => res.data)
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

export async function sendApplication(applicationId: UUID): Promise<void> {
  return client.post(
    `/v2/applications/${applicationId}/actions/send-application`
  )
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
export async function respondToPlacementProposal(
  applicationId: UUID,
  status: PlacementPlanConfirmationStatus,
  reason?: PlacementPlanRejectReason,
  otherReason?: string
): Promise<void> {
  return client.post(
    `/v2/applications/${applicationId}/actions/respond-to-placement-proposal`,
    {
      status,
      reason,
      otherReason
    }
  )
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
    .then((res) => {
      return res.data
    })
    .then((data) => {
      return {
        ...data,
        child: {
          ...data.child,
          dob: LocalDate.parseIso(data.child.dob)
        },
        period: {
          start: LocalDate.parseIso(data.period.start),
          end: LocalDate.parseIso(data.period.end)
        },
        preschoolDaycarePeriod: data.preschoolDaycarePeriod
          ? {
              start: LocalDate.parseIso(data.preschoolDaycarePeriod.start),
              end: LocalDate.parseIso(data.preschoolDaycarePeriod.end)
            }
          : undefined,
        placements: data.placements.map((placement) => {
          return {
            ...placement,
            startDate: LocalDate.parseIso(placement.startDate),
            endDate: LocalDate.parseIso(placement.endDate)
          }
        })
      }
    })
    .then(Success)
    .catch(Failure)
}

export async function createPlacementPlan(
  applicationId: UUID,
  placementPlan: DaycarePlacementPlan
): Promise<void> {
  return client.post(
    `/v2/applications/${applicationId}/actions/create-placement-plan`,
    placementPlan
  )
}
export async function acceptPlacementProposal(unitId: UUID): Promise<void> {
  return client.post(`/v2/applications/placement-proposals/${unitId}/accept`)
}
export async function acceptDecision(
  applicationId: UUID,
  decisionId: UUID,
  requestedStartDate: LocalDate
): Promise<void> {
  return client.post(
    `/v2/applications/${applicationId}/actions/accept-decision`,
    { decisionId, requestedStartDate }
  )
}

export async function rejectDecision(
  applicationId: UUID,
  decisionId: UUID
): Promise<void> {
  return client.post(
    `/v2/applications/${applicationId}/actions/reject-decision`,
    { decisionId }
  )
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
export async function batchConfirmDecisionMailed(
  applicationIds: UUID[]
): Promise<void> {
  return client.post('/v2/applications/batch/actions/confirm-decision-mailed', {
    applicationIds
  })
}

export async function getApplicationNotes(
  applicationId: UUID
): Promise<Result<ApplicationNote[]>> {
  return client
    .post<JsonOf<ApplicationNote[]>>('/note/search', {
      applicationIds: [applicationId]
    })
    .then((res) =>
      res.data.map((note) => ({
        ...note,
        created: new Date(note.created),
        updated: new Date(note.updated)
      }))
    )
    .then(Success)
    .catch(Failure)
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
