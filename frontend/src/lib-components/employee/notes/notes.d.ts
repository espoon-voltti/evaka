// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

export type StickyNoteBody = { note: string; expires: LocalDate }
export type Note = StickyNoteBody & { id: UUID }
export type EditedNote = StickyNoteBody & { id?: UUID }
