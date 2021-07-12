// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { client } from './client'
import { UUID } from '../types'
import { FamilyContact, FamilyOverview } from '../types/family-overview'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

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

interface FamilyContactUpdateBody {
  childId: string
  contactPersonId: string
  priority?: number
}

export async function updateFamilyContacts(
  body: FamilyContactUpdateBody
): Promise<Result<void>> {
  return client
    .post<void>('/family/contacts', body)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}
