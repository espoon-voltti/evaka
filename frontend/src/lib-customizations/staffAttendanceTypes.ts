// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'

import type { FeatureFlags } from './types'

export const resolveStaffAttendanceTypes = (
  featureFlags: FeatureFlags,
  customizations: StaffAttendanceType[] | null | undefined
) =>
  customizations !== null && customizations !== undefined
    ? customizations
    : [
        'PRESENT',
        'OTHER_WORK',
        'TRAINING',
        'OVERTIME',
        'JUSTIFIED_CHANGE',
        'SICKNESS',
        'CHILD_SICKNESS'
      ]
        .filter((type): type is StaffAttendanceType =>
          featureFlags.hideOvertimeSelection ? type !== 'OVERTIME' : true
        )
        .filter((type): type is StaffAttendanceType =>
          featureFlags.hideSicknessSelection ? type !== 'SICKNESS' : true
        )
        .filter((type): type is StaffAttendanceType =>
          featureFlags.hideChildSicknessSelection
            ? type !== 'CHILD_SICKNESS'
            : true
        )
