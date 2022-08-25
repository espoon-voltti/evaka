// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from 'citizen-frontend/api-client'
import { Failure, Result, Success } from 'lib-common/api'
import { CitizenChildConsent } from 'lib-common/generated/api-types/children'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

export function getChildConsents(): Promise<
  Result<Record<UUID, CitizenChildConsent[]>>
> {
  return client
    .get<JsonOf<Record<UUID, CitizenChildConsent[]>>>(
      '/citizen/children/consents'
    )
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export function insertChildConsents(
  childId: UUID,
  consents: CitizenChildConsent[]
): Promise<Result<void>> {
  return client
    .post(`/citizen/children/${childId}/consent`, consents)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function getChildConsentNotifications(): Promise<
  Result<Record<string, number>>
> {
  return client
    .get<JsonOf<Record<string, number>>>(
      '/citizen/children/consents/notifications'
    )
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}
