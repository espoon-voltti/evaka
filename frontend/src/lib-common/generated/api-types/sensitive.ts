// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import LocalDate from '../../local-date'
import { ContactInfo } from './attendance'
import { PlacementType } from './placement'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.sensitive.ChildSensitiveInformation
*/
export interface ChildSensitiveInformation {
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
  placementTypes: PlacementType[]
  preferredName: string
  ssn: string
}
