// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { dummyIdpUrl } from '../config'

export interface DummyIdpVtjPerson {
  firstNames: string
  lastName: string
  socialSecurityNumber: string
  address?: {
    streetAddress: string | null
    postalCode: string | null
    postOffice: string | null
    streetAddressSe?: string | null
    postOfficeSe?: string | null
  } | null
  nativeLanguage?: { languageName?: string; code: string } | null
  nationalities?: { countryName?: string; countryCode: string }[]
  restrictedDetails?: { enabled: boolean; endDate?: string | null } | null
  dateOfDeath?: string | null
  residenceCode?: string | null
  municipalityOfResidence?: string | null
  comment?: string
}

export interface DummyIdpVtjDataset {
  persons: DummyIdpVtjPerson[]
  guardianDependants: Record<string, string[]>
}

export async function upsertDummyIdpVtjDataset(
  dataset: DummyIdpVtjDataset
): Promise<void> {
  const res = await fetch(`${dummyIdpUrl}/idp/users`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(dataset)
  })
  if (!res.ok) {
    throw new Error(
      `Failed to upsert dummy-idp VTJ dataset: ${res.status} ${res.statusText}: ${await res.text()}`
    )
  }
}
