// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type DateRange from 'lib-common/date-range'
import { StaticChip } from 'lib-components/atoms/Chip'
import { colors } from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'
import { getStatusLabelByDateRange } from '../../utils/date'

export type StatusLabelType =
  | 'coming'
  | 'active'
  | 'completed'
  | 'conflict'
  | 'guarantee'

const colorsByStatus: Record<StatusLabelType, string> = {
  active: colors.accents.a3emerald,
  coming: colors.accents.a7mint,
  completed: colors.accents.a8lightBlue,
  conflict: colors.status.warning,
  guarantee: colors.accents.a7mint
}
type Props =
  | {
      status: StatusLabelType
    }
  | {
      dateRange: DateRange
    }

function StatusLabel(props: Props) {
  const { i18n } = useTranslation()

  const status =
    'status' in props
      ? props.status
      : getStatusLabelByDateRange({
          startDate: props.dateRange.start,
          endDate: props.dateRange.end
        })

  return (
    <StaticChip color={colorsByStatus[status]}>
      {i18n.common.statuses[status]}
    </StaticChip>
  )
}

export default React.memo(StatusLabel)
