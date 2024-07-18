// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import LocalTime from '../../local-time'
import TimeRange from '../../time-range'
import { JsonOf } from '../../json'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.calendarevent.AttendanceType
*/
export type AttendanceType =
  | 'INDIVIDUAL'
  | 'GROUP'
  | 'UNIT'

/**
* Generated from fi.espoo.evaka.calendarevent.AttendingChild
*/
export interface AttendingChild {
  groupName: string | null
  periods: FiniteDateRange[]
  type: AttendanceType
  unitName: string | null
}

/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEvent
*/
export interface CalendarEvent {
  contentModifiedAt: HelsinkiDateTime
  description: string
  eventType: CalendarEventType
  groups: GroupInfo[]
  id: UUID
  individualChildren: IndividualChild[]
  period: FiniteDateRange
  times: CalendarEventTime[]
  title: string
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventForm
*/
export interface CalendarEventForm {
  description: string
  eventType: CalendarEventType
  period: FiniteDateRange
  times: CalendarEventTimeForm[] | null
  title: string
  tree: Record<UUID, UUID[] | null> | null
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventTime
*/
export interface CalendarEventTime {
  childId: UUID | null
  date: LocalDate
  endTime: LocalTime
  id: UUID
  startTime: LocalTime
}

/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventTimeCitizenReservationForm
*/
export interface CalendarEventTimeCitizenReservationForm {
  calendarEventTimeId: UUID
  childId: UUID
}

/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventTimeEmployeeReservationForm
*/
export interface CalendarEventTimeEmployeeReservationForm {
  calendarEventTimeId: UUID
  childId: UUID | null
}

/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventTimeForm
*/
export interface CalendarEventTimeForm {
  date: LocalDate
  timeRange: TimeRange
}

/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventType
*/
export type CalendarEventType =
  | 'DAYCARE_EVENT'
  | 'DISCUSSION_SURVEY'

/**
* Generated from fi.espoo.evaka.calendarevent.CalendarEventUpdateForm
*/
export interface CalendarEventUpdateForm {
  description: string
  title: string
  tree: Record<UUID, UUID[] | null> | null
}

/**
* Generated from fi.espoo.evaka.calendarevent.CitizenCalendarEvent
*/
export interface CitizenCalendarEvent {
  attendingChildren: Record<UUID, AttendingChild[]>
  description: string
  eventType: CalendarEventType
  id: UUID
  period: FiniteDateRange
  timesByChild: Record<UUID, CitizenCalendarEventTime[]>
  title: string
}

/**
* Generated from fi.espoo.evaka.calendarevent.CitizenCalendarEventTime
*/
export interface CitizenCalendarEventTime {
  childId: UUID | null
  date: LocalDate
  endTime: LocalTime
  id: UUID
  isEditable: boolean
  startTime: LocalTime
}

/**
* Generated from fi.espoo.evaka.calendarevent.DiscussionReservationDay
*/
export interface DiscussionReservationDay {
  date: LocalDate
  events: CalendarEvent[]
  isHoliday: boolean
  isOperationalDay: boolean
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
  firstName: string
  groupId: UUID
  id: UUID
  lastName: string
}


export function deserializeJsonAttendingChild(json: JsonOf<AttendingChild>): AttendingChild {
  return {
    ...json,
    periods: json.periods.map(e => FiniteDateRange.parseJson(e))
  }
}


export function deserializeJsonCalendarEvent(json: JsonOf<CalendarEvent>): CalendarEvent {
  return {
    ...json,
    contentModifiedAt: HelsinkiDateTime.parseIso(json.contentModifiedAt),
    period: FiniteDateRange.parseJson(json.period),
    times: json.times.map(e => deserializeJsonCalendarEventTime(e))
  }
}


export function deserializeJsonCalendarEventForm(json: JsonOf<CalendarEventForm>): CalendarEventForm {
  return {
    ...json,
    period: FiniteDateRange.parseJson(json.period),
    times: (json.times != null) ? json.times.map(e => deserializeJsonCalendarEventTimeForm(e)) : null
  }
}


export function deserializeJsonCalendarEventTime(json: JsonOf<CalendarEventTime>): CalendarEventTime {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    endTime: LocalTime.parseIso(json.endTime),
    startTime: LocalTime.parseIso(json.startTime)
  }
}


export function deserializeJsonCalendarEventTimeForm(json: JsonOf<CalendarEventTimeForm>): CalendarEventTimeForm {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    timeRange: TimeRange.parseJson(json.timeRange)
  }
}


export function deserializeJsonCitizenCalendarEvent(json: JsonOf<CitizenCalendarEvent>): CitizenCalendarEvent {
  return {
    ...json,
    attendingChildren: Object.fromEntries(Object.entries(json.attendingChildren).map(
      ([k, v]) => [k, v.map(e => deserializeJsonAttendingChild(e))]
    )),
    period: FiniteDateRange.parseJson(json.period),
    timesByChild: Object.fromEntries(Object.entries(json.timesByChild).map(
      ([k, v]) => [k, v.map(e => deserializeJsonCitizenCalendarEventTime(e))]
    ))
  }
}


export function deserializeJsonCitizenCalendarEventTime(json: JsonOf<CitizenCalendarEventTime>): CitizenCalendarEventTime {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    endTime: LocalTime.parseIso(json.endTime),
    startTime: LocalTime.parseIso(json.startTime)
  }
}


export function deserializeJsonDiscussionReservationDay(json: JsonOf<DiscussionReservationDay>): DiscussionReservationDay {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    events: json.events.map(e => deserializeJsonCalendarEvent(e))
  }
}
