// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace */

import type LocalDate from '../../local-date'
import { type ContactInfo } from './attendance'
import { type PlacementType } from './placement'
import { type UUID } from '../../types'

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
