// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

export type SelectedGroupId =
  | { type: 'all'; unitId: UUID }
  | { type: 'one'; unitId: UUID; id: UUID }

export const toSelectedGroupId = ({
  unitId,
  groupId
}: {
  unitId: UUID
  groupId: UUID | undefined | null
}): SelectedGroupId =>
  groupId && groupId !== 'all'
    ? { type: 'one', unitId, id: groupId }
    : { type: 'all', unitId }
