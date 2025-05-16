// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import { useMemo } from 'react'

import type { DaycareGroupResponse } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'

export const useGroupOptions = (groups: Record<string, DaycareGroupResponse>) =>
  useMemo(
    () =>
      sortBy(
        Object.values(groups).filter(
          ({ endDate }) =>
            endDate === null || endDate.isAfter(LocalDate.todayInHelsinkiTz())
        ),
        ({ name }) => name
      ),
    [groups]
  )
