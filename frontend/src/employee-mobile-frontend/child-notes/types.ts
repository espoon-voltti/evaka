// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ChildDailyNoteBody } from 'lib-common/generated/api-types/note'

export type ChildDailyNoteFormData = Omit<
  ChildDailyNoteBody,
  'sleepingMinutes'
> & {
  sleepingHours: number | null
  sleepingMinutes: number | null
}
