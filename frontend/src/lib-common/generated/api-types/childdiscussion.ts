// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace */

import LocalDate from '../../local-date'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.childdiscussion.ChildDiscussion
*/
export interface ChildDiscussion {
  childId: UUID
  counselingDate: LocalDate | null
  heldDate: LocalDate | null
  id: UUID
  offeredDate: LocalDate | null
}

/**
* Generated from fi.espoo.evaka.childdiscussion.ChildDiscussionBody
*/
export interface ChildDiscussionBody {
  counselingDate: LocalDate | null
  heldDate: LocalDate | null
  offeredDate: LocalDate | null
}
