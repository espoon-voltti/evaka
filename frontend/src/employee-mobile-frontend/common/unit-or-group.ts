// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { DaycareId, GroupId } from 'lib-common/generated/api-types/shared'
import { fromUuid } from 'lib-common/id-type'

export type UnitOrGroup =
  | { type: 'unit'; unitId: DaycareId }
  | { type: 'group'; unitId: DaycareId; id: GroupId }
  | { type: 'shift-care'; unitId: DaycareId }

export const isUnitView = (
  unitOrGroup: UnitOrGroup
): unitOrGroup is Exclude<UnitOrGroup, { type: 'group' }> =>
  unitOrGroup.type === 'unit' || unitOrGroup.type === 'shift-care'

export const toUnitOrGroup = (
  unitId: DaycareId,
  groupId?: string | null
): UnitOrGroup =>
  groupId === 'shift-care'
    ? { type: 'shift-care', unitId }
    : groupId && groupId !== 'all'
      ? { type: 'group', unitId, id: fromUuid(groupId) }
      : { type: 'unit', unitId }
