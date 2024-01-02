// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import FiniteDateRange from '../../finite-date-range'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.calendarevent.AttendingChild
*/
export interface AttendingChild {
  groupName: string | null
  periods: FiniteDateRange[]
  type: string
  unitName: string | null
}

/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEvent
*/
export interface CalendarEvent {
  description: string
  groups: GroupInfo[]
  id: UUID
  individualChildren: IndividualChild[]
  period: FiniteDateRange
  title: string
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventForm
*/
export interface CalendarEventForm {
  description: string
  period: FiniteDateRange
  title: string
  tree: Record<string, UUID[] | null> | null
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventUpdateForm
*/
export interface CalendarEventUpdateForm {
  description: string
  title: string
}

/**
* Generated from fi.espoo.evaka.calendarevent.CitizenCalendarEvent
*/
export interface CitizenCalendarEvent {
  attendingChildren: Record<string, AttendingChild[]>
  description: string
  id: UUID
  title: string
}

/**
* Generated from fi.espoo.evaka.calendarevent.GroupInfo
*/
export interface GroupInfo {
  id: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.calendarevent.IndividualChild
*/
export interface IndividualChild {
  groupId: UUID
  id: UUID
  name: string
}
