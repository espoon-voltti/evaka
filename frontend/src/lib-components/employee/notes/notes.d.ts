// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Id } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'

export type StickyNoteBody = { note: string; expires: LocalDate }
export type Note<IdType extends Id<string>> = StickyNoteBody & { id: IdType }
export type EditedNote<IdType extends Id<string>> = StickyNoteBody & {
  id?: IdType
}
