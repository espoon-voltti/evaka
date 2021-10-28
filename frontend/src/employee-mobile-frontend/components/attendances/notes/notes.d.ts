// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { ChildDailyNoteBody } from 'lib-common/generated/api-types/note'

export type StickyNoteBody = { note: string; expires: LocalDate }
export type Note = StickyNoteBody & { id: UUID }
export type EditedNote = StickyNoteBody & { id?: UUID }

export type ChildDailyNoteFormData = Omit<
  ChildDailyNoteBody,
  'sleepingMinutes'
> & {
  sleepingHours: number | null
  sleepingMinutes: number | null
}
