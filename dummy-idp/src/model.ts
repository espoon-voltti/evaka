// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'

export const personAddressSchema = z.object({
  streetAddress: z.string().nullish(),
  postalCode: z.string().nullish(),
  postOffice: z.string().nullish(),
  streetAddressSe: z.string().nullish(),
  postOfficeSe: z.string().nullish()
})

export const nationalitySchema = z.object({
  countryName: z.string().default(''),
  countryCode: z.string()
})

export const nativeLanguageSchema = z.object({
  languageName: z.string().default(''),
  code: z.string()
})

export const restrictedDetailsSchema = z.object({
  enabled: z.boolean(),
  endDate: z.string().nullish()
})

export type VtjPerson = z.infer<typeof vtjPersonSchema>
export const vtjPersonSchema = z.object({
  firstNames: z.string(),
  lastName: z.string(),
  socialSecurityNumber: z.string(),
  address: personAddressSchema.nullish(),
  nativeLanguage: nativeLanguageSchema.nullish(),
  nationalities: z.array(nationalitySchema).default([]),
  restrictedDetails: restrictedDetailsSchema.nullish(),
  dateOfDeath: z.string().nullish(),
  residenceCode: z.string().nullish(),
  municipalityOfResidence: z.string().nullish(),
  comment: z.string().optional()
})

export type MockVtjDataset = z.infer<typeof mockVtjDatasetSchema>
export const mockVtjDatasetSchema = z.object({
  persons: z.array(vtjPersonSchema),
  guardianDependants: z.record(z.string(), z.array(z.string())).default({})
})

export const sfiSamlAttrs = {
  'urn:oid:1.2.246.21': 'nationalIdentificationNumber',
  'urn:oid:2.5.4.3': 'cn',
  'urn:oid:2.16.840.1.113730.3.1.241': 'displayName',
  'urn:oid:2.5.4.42': 'givenName',
  'urn:oid:2.5.4.4': 'sn'
}
export type SfiSamlAttrUrn = keyof typeof sfiSamlAttrs
export const sfiSamlAttrUrns: SfiSamlAttrUrn[] = Object.keys(
  sfiSamlAttrs
) as SfiSamlAttrUrn[]

export function toSfiSamlAttrs(
  person: VtjPerson
): Record<SfiSamlAttrUrn, string> {
  const fullName = `${person.firstNames} ${person.lastName}`
  return {
    'urn:oid:1.2.246.21': person.socialSecurityNumber,
    'urn:oid:2.5.4.3': fullName,
    'urn:oid:2.16.840.1.113730.3.1.241': fullName,
    'urn:oid:2.5.4.42': person.firstNames,
    'urn:oid:2.5.4.4': person.lastName
  }
}

declare module 'express-session' {
  interface SessionData {
    user?: {
      nameId: string
      person: VtjPerson
    }
  }
}
