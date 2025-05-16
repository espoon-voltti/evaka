// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  MissingBackupGroupPlacement,
  MissingGroupPlacement
} from 'lib-common/generated/api-types/placement'

export type MissingPlacement =
  | { type: 'group'; data: MissingGroupPlacement }
  | { type: 'backup'; data: MissingBackupGroupPlacement }

export function toMissingPlacements(
  missingGroupPlacements: MissingGroupPlacement[],
  missingBackupGroupPlacements: MissingBackupGroupPlacement[]
) {
  return [
    ...missingGroupPlacements.map((data) => ({
      type: 'group' as const,
      data
    })),
    ...missingBackupGroupPlacements.map((data) => ({
      type: 'backup' as const,
      data
    }))
  ]
}
