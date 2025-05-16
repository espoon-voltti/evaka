// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ScheduleType } from '../generated/api-types/placement'

export function mapScheduleType<T>(
  scheduleType: ScheduleType,
  ops: Record<ScheduleType, () => T>
): T {
  return ops[scheduleType]()
}
