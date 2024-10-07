// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from '../../local-date'
import { ContactInfo } from './attendance'
import { JsonOf } from '../../json'
import { PlacementType } from './placement'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.sensitive.ChildBasicInformation
*/
export interface ChildBasicInformation {
  backupPickups: ContactInfo[]
  contacts: ContactInfo[]
  dateOfBirth: LocalDate
  firstName: string
  id: UUID
  lastName: string
  placementType: PlacementType | null
  preferredName: string
}

/**
* Generated from fi.espoo.evaka.sensitive.ChildSensitiveInformation
*/
export interface ChildSensitiveInformation {
  additionalInfo: string
  allergies: string
  childAddress: string
  diet: string
  id: UUID
  medication: string
  ssn: string
}


export function deserializeJsonChildBasicInformation(json: JsonOf<ChildBasicInformation>): ChildBasicInformation {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
  }
}
