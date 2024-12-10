// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DaycareId, GroupId } from 'lib-common/generated/api-types/shared'
import { UUID } from 'lib-common/types'

export type UnitOrGroup =
  | { type: 'unit'; unitId: DaycareId }
  | { type: 'group'; unitId: DaycareId; id: GroupId }

export const toUnitOrGroup = (
  unitId: UUID,
  groupId?: UUID | null
): UnitOrGroup =>
  groupId && groupId !== 'all'
    ? { type: 'group', unitId, id: groupId }
    : { type: 'unit', unitId }
