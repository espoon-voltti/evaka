// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import LocalDate from '../../local-date'
import { ContactInfo } from './attendance'
import { JsonOf } from '../../json'
import { PlacementType } from './placement'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.sensitive.ChildSensitiveInformation
*/
export interface ChildSensitiveInformation {
  additionalInfo: string
  allergies: string
  backupPickups: ContactInfo[]
  childAddress: string
  contacts: ContactInfo[]
  dateOfBirth: LocalDate
  diet: string
  firstName: string
  id: UUID
  lastName: string
  medication: string
  placementType: PlacementType | null
  placementTypes: PlacementType[]
  preferredName: string
  ssn: string
}


export function deserializeJsonChildSensitiveInformation(json: JsonOf<ChildSensitiveInformation>): ChildSensitiveInformation {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
  }
}
