// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AcceptDecisionRequest } from 'lib-common/generated/api-types/application'
import { AcceptPlacementProposalRequest } from 'lib-common/generated/api-types/application'
import { ApplicationNote } from 'lib-common/generated/api-types/application'
import { ApplicationNoteResponse } from 'lib-common/generated/api-types/application'
import { ApplicationResponse } from 'lib-common/generated/api-types/application'
import { ApplicationUpdate } from 'lib-common/generated/api-types/application'
import { DaycarePlacementPlan } from 'lib-common/generated/api-types/application'
import { DecisionDraftGroup } from 'lib-common/generated/api-types/application'
import { DecisionDraftUpdate } from 'lib-common/generated/api-types/decision'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { NoteRequest } from 'lib-common/generated/api-types/application'
import { PagedApplicationSummaries } from 'lib-common/generated/api-types/application'
import { PaperApplicationCreateRequest } from 'lib-common/generated/api-types/application'
import { PersonApplicationSummary } from 'lib-common/generated/api-types/application'
import { PlacementPlanDraft } from 'lib-common/generated/api-types/placement'
import { PlacementProposalConfirmationUpdate } from 'lib-common/generated/api-types/application'
import { PreschoolTerm } from 'lib-common/generated/api-types/daycare'
import { RejectDecisionRequest } from 'lib-common/generated/api-types/application'
import { SearchApplicationRequest } from 'lib-common/generated/api-types/application'
import { SimpleBatchRequest } from 'lib-common/generated/api-types/application'
import { UUID } from 'lib-common/types'
import { UnitApplications } from 'lib-common/generated/api-types/application'
import { client } from '../../api/client'
import { deserializeJsonApplicationNote } from 'lib-common/generated/api-types/application'
import { deserializeJsonApplicationNoteResponse } from 'lib-common/generated/api-types/application'
import { deserializeJsonApplicationResponse } from 'lib-common/generated/api-types/application'
import { deserializeJsonDecisionDraftGroup } from 'lib-common/generated/api-types/application'
import { deserializeJsonPagedApplicationSummaries } from 'lib-common/generated/api-types/application'
import { deserializeJsonPersonApplicationSummary } from 'lib-common/generated/api-types/application'
import { deserializeJsonPlacementPlanDraft } from 'lib-common/generated/api-types/placement'
import { deserializeJsonPreschoolTerm } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonUnitApplications } from 'lib-common/generated/api-types/application'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.acceptDecision
*/
export async function acceptDecision(
  request: {
    applicationId: UUID,
    body: AcceptDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/applications/${request.applicationId}/actions/accept-decision`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AcceptDecisionRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.acceptPlacementProposal
*/
export async function acceptPlacementProposal(
  request: {
    unitId: UUID,
    body: AcceptPlacementProposalRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/applications/placement-proposals/${request.unitId}/accept`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AcceptPlacementProposalRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.createPaperApplication
*/
export async function createPaperApplication(
  request: {
    body: PaperApplicationCreateRequest
  }
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee/applications`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PaperApplicationCreateRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.createPlacementPlan
*/
export async function createPlacementPlan(
  request: {
    applicationId: UUID,
    body: DaycarePlacementPlan
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/applications/${request.applicationId}/actions/create-placement-plan`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DaycarePlacementPlan>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.getApplicationDetails
*/
export async function getApplicationDetails(
  request: {
    applicationId: UUID
  }
): Promise<ApplicationResponse> {
  const { data: json } = await client.request<JsonOf<ApplicationResponse>>({
    url: uri`/employee/applications/${request.applicationId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonApplicationResponse(json)
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.getApplicationSummaries
*/
export async function getApplicationSummaries(
  request: {
    body: SearchApplicationRequest
  }
): Promise<PagedApplicationSummaries> {
  const { data: json } = await client.request<JsonOf<PagedApplicationSummaries>>({
    url: uri`/employee/applications/search`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<SearchApplicationRequest>
  })
  return deserializeJsonPagedApplicationSummaries(json)
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.getChildApplicationSummaries
*/
export async function getChildApplicationSummaries(
  request: {
    childId: UUID
  }
): Promise<PersonApplicationSummary[]> {
  const { data: json } = await client.request<JsonOf<PersonApplicationSummary[]>>({
    url: uri`/employee/applications/by-child/${request.childId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonPersonApplicationSummary(e))
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.getDecisionDrafts
*/
export async function getDecisionDrafts(
  request: {
    applicationId: UUID
  }
): Promise<DecisionDraftGroup> {
  const { data: json } = await client.request<JsonOf<DecisionDraftGroup>>({
    url: uri`/employee/applications/${request.applicationId}/decision-drafts`.toString(),
    method: 'GET'
  })
  return deserializeJsonDecisionDraftGroup(json)
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.getGuardianApplicationSummaries
*/
export async function getGuardianApplicationSummaries(
  request: {
    guardianId: UUID
  }
): Promise<PersonApplicationSummary[]> {
  const { data: json } = await client.request<JsonOf<PersonApplicationSummary[]>>({
    url: uri`/employee/applications/by-guardian/${request.guardianId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonPersonApplicationSummary(e))
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.getPlacementPlanDraft
*/
export async function getPlacementPlanDraft(
  request: {
    applicationId: UUID
  }
): Promise<PlacementPlanDraft> {
  const { data: json } = await client.request<JsonOf<PlacementPlanDraft>>({
    url: uri`/employee/applications/${request.applicationId}/placement-draft`.toString(),
    method: 'GET'
  })
  return deserializeJsonPlacementPlanDraft(json)
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.getUnitApplications
*/
export async function getUnitApplications(
  request: {
    unitId: UUID
  }
): Promise<UnitApplications> {
  const { data: json } = await client.request<JsonOf<UnitApplications>>({
    url: uri`/employee/applications/units/${request.unitId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonUnitApplications(json)
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.rejectDecision
*/
export async function rejectDecision(
  request: {
    applicationId: UUID,
    body: RejectDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/applications/${request.applicationId}/actions/reject-decision`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<RejectDecisionRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.respondToPlacementProposal
*/
export async function respondToPlacementProposal(
  request: {
    applicationId: UUID,
    body: PlacementProposalConfirmationUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/applications/${request.applicationId}/actions/respond-to-placement-proposal`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PlacementProposalConfirmationUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.sendApplication
*/
export async function sendApplication(
  request: {
    applicationId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/applications/${request.applicationId}/actions/send-application`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.simpleApplicationAction
*/
export async function simpleApplicationAction(
  request: {
    applicationId: UUID,
    action: string
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/applications/${request.applicationId}/actions/${request.action}`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.simpleBatchAction
*/
export async function simpleBatchAction(
  request: {
    action: string,
    body: SimpleBatchRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/applications/batch/actions/${request.action}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<SimpleBatchRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.updateApplication
*/
export async function updateApplication(
  request: {
    applicationId: UUID,
    body: ApplicationUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/applications/${request.applicationId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<ApplicationUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.ApplicationControllerV2.updateDecisionDrafts
*/
export async function updateDecisionDrafts(
  request: {
    applicationId: UUID,
    body: DecisionDraftUpdate[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/applications/${request.applicationId}/decision-drafts`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<DecisionDraftUpdate[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.PlacementToolController.getNextPreschoolTerm
*/
export async function getNextPreschoolTerm(): Promise<PreschoolTerm[]> {
  const { data: json } = await client.request<JsonOf<PreschoolTerm[]>>({
    url: uri`/employee/placement-tool/next-term`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonPreschoolTerm(e))
}


/**
* Generated from fi.espoo.evaka.application.notes.NoteController.createNote
*/
export async function createNote(
  request: {
    applicationId: UUID,
    body: NoteRequest
  }
): Promise<ApplicationNote> {
  const { data: json } = await client.request<JsonOf<ApplicationNote>>({
    url: uri`/employee/note/application/${request.applicationId}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<NoteRequest>
  })
  return deserializeJsonApplicationNote(json)
}


/**
* Generated from fi.espoo.evaka.application.notes.NoteController.deleteNote
*/
export async function deleteNote(
  request: {
    noteId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/note/${request.noteId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.notes.NoteController.getNotes
*/
export async function getNotes(
  request: {
    applicationId: UUID
  }
): Promise<ApplicationNoteResponse[]> {
  const { data: json } = await client.request<JsonOf<ApplicationNoteResponse[]>>({
    url: uri`/employee/note/application/${request.applicationId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonApplicationNoteResponse(e))
}


/**
* Generated from fi.espoo.evaka.application.notes.NoteController.updateNote
*/
export async function updateNote(
  request: {
    noteId: UUID,
    body: NoteRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/note/${request.noteId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<NoteRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.application.notes.NoteController.updateServiceWorkerNote
*/
export async function updateServiceWorkerNote(
  request: {
    applicationId: UUID,
    body: NoteRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/note/service-worker/application/${request.applicationId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<NoteRequest>
  })
  return json
}
