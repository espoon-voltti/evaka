// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { CitizenIncomeCtas } from 'lib-common/generated/api-types/invoicing'
import type { DismissExpiringIncomeCtaRequest } from 'lib-common/generated/api-types/invoicing'
import type { IncomeNotificationId } from 'lib-common/generated/api-types/shared'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import { client } from '../../api-client'
import { deserializeJsonCitizenIncomeCtas } from 'lib-common/generated/api-types/invoicing'
import { uri } from 'lib-common/uri'


/**
* Generated from evaka.core.invoicing.controller.IncomeControllerCitizen.dismissExpiringIncomeCta
*/
export async function dismissExpiringIncomeCta(
  request: {
    body: DismissExpiringIncomeCtaRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/income/cta/expiring-income/dismiss`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DismissExpiringIncomeCtaRequest>
  })
  return json
}


/**
* Generated from evaka.core.invoicing.controller.IncomeControllerCitizen.dismissIncompleteIncomeReminderCta
*/
export async function dismissIncompleteIncomeReminderCta(
  request: {
    notificationId: IncomeNotificationId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/income/cta/incomplete-income-reminder/${request.notificationId}/dismiss`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from evaka.core.invoicing.controller.IncomeControllerCitizen.getIncomeCtas
*/
export async function getIncomeCtas(): Promise<CitizenIncomeCtas> {
  const { data: json } = await client.request<JsonOf<CitizenIncomeCtas>>({
    url: uri`/citizen/income/cta`.toString(),
    method: 'GET'
  })
  return deserializeJsonCitizenIncomeCtas(json)
}
