// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from '@evaka/lib-common/src/api'
import { client } from '~/api/client'
import { UUID } from '~types'
import { FamilyContact, FamilyOverview } from '~types/family-overview'
import { JsonOf } from '@evaka/lib-common/src/json'
import LocalDate from '@evaka/lib-common/src/local-date'

export async function getFamilyOverview(
  adultId: UUID
): Promise<Result<FamilyOverview>> {
  return client
    .get<JsonOf<FamilyOverview>>(`/family/by-adult/${adultId}`)
    .then(({ data }) => ({
      ...data,
      headOfFamily: {
        ...data.headOfFamily,
        dateOfBirth: LocalDate.parseIso(data.headOfFamily.dateOfBirth)
      },
      partner: data.partner
        ? {
            ...data.partner,
            dateOfBirth: LocalDate.parseIso(data.partner.dateOfBirth)
          }
        : undefined,
      children: data.children.map((child) => ({
        ...child,
        dateOfBirth: LocalDate.parseIso(child.dateOfBirth)
      }))
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getFamilyContacts(
  childId: UUID
): Promise<Result<FamilyContact[]>> {
  return client
    .get<JsonOf<FamilyContact[]>>('/family/contacts', {
      params: { childId }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
