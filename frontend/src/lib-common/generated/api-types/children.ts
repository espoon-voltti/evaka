// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.children.Child
*/
export interface Child {
  firstName: string
  group: Group | null
  id: UUID
  imageId: UUID | null
  lastName: string
}

/**
* Generated from fi.espoo.evaka.children.ChildrenResponse
*/
export interface ChildrenResponse {
  children: Child[]
}

/**
* Generated from fi.espoo.evaka.children.Group
*/
export interface Group {
  id: UUID
  name: string
}
