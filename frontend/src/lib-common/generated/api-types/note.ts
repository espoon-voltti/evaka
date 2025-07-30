// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { ChildDailyNoteId } from './shared'
import type { ChildStickyNoteId } from './shared'
import type { GroupId } from './shared'
import type { GroupNoteId } from './shared'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import LocalDate from '../../local-date'
import type { PersonId } from './shared'

/**
* Generated from fi.espoo.evaka.note.child.daily.ChildDailyNote
*/
export interface ChildDailyNote {
  childId: PersonId
  feedingNote: ChildDailyNoteLevel | null
  id: ChildDailyNoteId
  modifiedAt: HelsinkiDateTime
  note: string
  reminderNote: string
  reminders: ChildDailyNoteReminder[]
  sleepingMinutes: number | null
  sleepingNote: ChildDailyNoteLevel | null
}

/**
* Generated from fi.espoo.evaka.note.child.daily.ChildDailyNoteBody
*/
export interface ChildDailyNoteBody {
  feedingNote: ChildDailyNoteLevel | null
  note: string
  reminderNote: string
  reminders: ChildDailyNoteReminder[]
  sleepingMinutes: number | null
  sleepingNote: ChildDailyNoteLevel | null
}

/**
* Generated from fi.espoo.evaka.note.child.daily.ChildDailyNoteLevel
*/
export const childDailyNoteLevelValues = [
  'GOOD',
  'MEDIUM',
  'NONE'
] as const

export type ChildDailyNoteLevel = typeof childDailyNoteLevelValues[number]

/**
* Generated from fi.espoo.evaka.note.child.daily.ChildDailyNoteReminder
*/
export const childDailyNoteReminderValues = [
  'DIAPERS',
  'CLOTHES',
  'LAUNDRY'
] as const

export type ChildDailyNoteReminder = typeof childDailyNoteReminderValues[number]

/**
* Generated from fi.espoo.evaka.note.child.sticky.ChildStickyNote
*/
export interface ChildStickyNote {
  childId: PersonId
  expires: LocalDate
  id: ChildStickyNoteId
  modifiedAt: HelsinkiDateTime
  note: string
}

/**
* Generated from fi.espoo.evaka.note.child.sticky.ChildStickyNoteBody
*/
export interface ChildStickyNoteBody {
  expires: LocalDate
  note: string
}

/**
* Generated from fi.espoo.evaka.note.group.GroupNote
*/
export interface GroupNote {
  created: HelsinkiDateTime
  expires: LocalDate
  groupId: GroupId
  id: GroupNoteId
  modifiedAt: HelsinkiDateTime
  note: string
}

/**
* Generated from fi.espoo.evaka.note.group.GroupNoteBody
*/
export interface GroupNoteBody {
  expires: LocalDate
  note: string
}

/**
* Generated from fi.espoo.evaka.note.NotesController.NotesByGroupResponse
*/
export interface NotesByGroupResponse {
  childDailyNotes: ChildDailyNote[]
  childStickyNotes: ChildStickyNote[]
  groupNotes: GroupNote[]
}


export function deserializeJsonChildDailyNote(json: JsonOf<ChildDailyNote>): ChildDailyNote {
  return {
    ...json,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt)
  }
}


export function deserializeJsonChildStickyNote(json: JsonOf<ChildStickyNote>): ChildStickyNote {
  return {
    ...json,
    expires: LocalDate.parseIso(json.expires),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt)
  }
}


export function deserializeJsonChildStickyNoteBody(json: JsonOf<ChildStickyNoteBody>): ChildStickyNoteBody {
  return {
    ...json,
    expires: LocalDate.parseIso(json.expires)
  }
}


export function deserializeJsonGroupNote(json: JsonOf<GroupNote>): GroupNote {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created),
    expires: LocalDate.parseIso(json.expires),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt)
  }
}


export function deserializeJsonGroupNoteBody(json: JsonOf<GroupNoteBody>): GroupNoteBody {
  return {
    ...json,
    expires: LocalDate.parseIso(json.expires)
  }
}


export function deserializeJsonNotesByGroupResponse(json: JsonOf<NotesByGroupResponse>): NotesByGroupResponse {
  return {
    ...json,
    childDailyNotes: json.childDailyNotes.map(e => deserializeJsonChildDailyNote(e)),
    childStickyNotes: json.childStickyNotes.map(e => deserializeJsonChildStickyNote(e)),
    groupNotes: json.groupNotes.map(e => deserializeJsonGroupNote(e))
  }
}
