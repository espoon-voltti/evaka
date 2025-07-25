// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import type { AssistanceNeedDecisionId } from 'lib-common/generated/api-types/shared'
import type { AssistanceNeedPreschoolDecisionId } from 'lib-common/generated/api-types/shared'
import type { ChildDocumentId } from 'lib-common/generated/api-types/shared'
import type { FeeDecisionId } from 'lib-common/generated/api-types/shared'
import type { JsonOf } from 'lib-common/json'
import type { ProcessMetadataResponse } from 'lib-common/generated/api-types/caseprocess'
import type { VoucherValueDecisionId } from 'lib-common/generated/api-types/shared'
import { client } from '../../api/client'
import { deserializeJsonProcessMetadataResponse } from 'lib-common/generated/api-types/caseprocess'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.caseprocess.ProcessMetadataController.getApplicationMetadata
*/
export async function getApplicationMetadata(
  request: {
    applicationId: ApplicationId
  }
): Promise<ProcessMetadataResponse> {
  const { data: json } = await client.request<JsonOf<ProcessMetadataResponse>>({
    url: uri`/employee/process-metadata/applications/${request.applicationId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonProcessMetadataResponse(json)
}


/**
* Generated from fi.espoo.evaka.caseprocess.ProcessMetadataController.getAssistanceNeedDecisionMetadata
*/
export async function getAssistanceNeedDecisionMetadata(
  request: {
    decisionId: AssistanceNeedDecisionId
  }
): Promise<ProcessMetadataResponse> {
  const { data: json } = await client.request<JsonOf<ProcessMetadataResponse>>({
    url: uri`/employee/process-metadata/assistance-need-decisions/${request.decisionId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonProcessMetadataResponse(json)
}


/**
* Generated from fi.espoo.evaka.caseprocess.ProcessMetadataController.getAssistanceNeedPreschoolDecisionMetadata
*/
export async function getAssistanceNeedPreschoolDecisionMetadata(
  request: {
    decisionId: AssistanceNeedPreschoolDecisionId
  }
): Promise<ProcessMetadataResponse> {
  const { data: json } = await client.request<JsonOf<ProcessMetadataResponse>>({
    url: uri`/employee/process-metadata/assistance-need-preschool-decisions/${request.decisionId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonProcessMetadataResponse(json)
}


/**
* Generated from fi.espoo.evaka.caseprocess.ProcessMetadataController.getChildDocumentMetadata
*/
export async function getChildDocumentMetadata(
  request: {
    childDocumentId: ChildDocumentId
  }
): Promise<ProcessMetadataResponse> {
  const { data: json } = await client.request<JsonOf<ProcessMetadataResponse>>({
    url: uri`/employee/process-metadata/child-documents/${request.childDocumentId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonProcessMetadataResponse(json)
}


/**
* Generated from fi.espoo.evaka.caseprocess.ProcessMetadataController.getFeeDecisionMetadata
*/
export async function getFeeDecisionMetadata(
  request: {
    feeDecisionId: FeeDecisionId
  }
): Promise<ProcessMetadataResponse> {
  const { data: json } = await client.request<JsonOf<ProcessMetadataResponse>>({
    url: uri`/employee/process-metadata/fee-decisions/${request.feeDecisionId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonProcessMetadataResponse(json)
}


/**
* Generated from fi.espoo.evaka.caseprocess.ProcessMetadataController.getVoucherValueDecisionMetadata
*/
export async function getVoucherValueDecisionMetadata(
  request: {
    voucherValueDecisionId: VoucherValueDecisionId
  }
): Promise<ProcessMetadataResponse> {
  const { data: json } = await client.request<JsonOf<ProcessMetadataResponse>>({
    url: uri`/employee/process-metadata/voucher-value-decisions/${request.voucherValueDecisionId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonProcessMetadataResponse(json)
}
