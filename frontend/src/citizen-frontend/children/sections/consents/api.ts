// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from 'citizen-frontend/api-client'
import { CitizenChildConsent } from 'lib-common/generated/api-types/children'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

export function getChildConsents(): Promise<
  Record<UUID, CitizenChildConsent[]>
> {
  return client
    .get<Record<UUID, CitizenChildConsent[]>>('/citizen/children/consents')
    .then((res) => res.data)
}

export function insertChildConsents({
  childId,
  consents
}: {
  childId: UUID
  consents: CitizenChildConsent[]
}): Promise<void> {
  return client.post(`/citizen/children/${childId}/consent`, consents)
}

export function getChildConsentNotifications(): Promise<
  Record<string, number>
> {
  return client
    .get<
      JsonOf<Record<string, number>>
    >('/citizen/children/consents/notifications')
    .then((res) => res.data)
}
