// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { JsonOf } from '../../json'
import LocalDate from '../../local-date'

/**
* Generated from evaka.core.vtj.VtjPersonPreview
*/
export interface VtjPersonPreview {
  dateOfBirth: LocalDate
  firstName: string
  lastName: string
  restrictedDetailsEnabled: boolean
  socialSecurityNumber: string
  streetAddress: string
}


export function deserializeJsonVtjPersonPreview(json: JsonOf<VtjPersonPreview>): VtjPersonPreview {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
  }
}
