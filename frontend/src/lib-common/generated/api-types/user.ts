// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.user.EvakaUser
*/
export interface EvakaUser {
  id: UUID
  name: string
  type: EvakaUserType
}

/**
* Generated from fi.espoo.evaka.user.EvakaUserType
*/
export type EvakaUserType =
  | 'SYSTEM'
  | 'CITIZEN'
  | 'EMPLOYEE'
  | 'MOBILE_DEVICE'
  | 'UNKNOWN'
