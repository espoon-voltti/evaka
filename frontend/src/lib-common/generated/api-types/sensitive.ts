// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from '../../local-date'
import { ContactInfo } from './attendance'
import { JsonOf } from '../../json'
import { PersonId } from './shared'
import { PlacementType } from './placement'

/**
* Generated from fi.espoo.evaka.sensitive.ChildBasicInformation
*/
export interface ChildBasicInformation {
  backupPickups: ContactInfo[]
  contacts: ContactInfo[]
  dateOfBirth: LocalDate
  firstName: string
  id: PersonId
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
  id: PersonId
  medication: string
  ssn: string
}


export function deserializeJsonChildBasicInformation(json: JsonOf<ChildBasicInformation>): ChildBasicInformation {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
  }
}
