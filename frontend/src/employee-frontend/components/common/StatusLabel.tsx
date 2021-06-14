// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import DateRange from 'lib-common/date-range'
import colors from 'lib-customizations/common'
import { useTranslation } from '../../state/i18n'
import { getStatusLabelByDateRange } from '../../utils/date'

export type StatusLabelType = 'coming' | 'active' | 'completed' | 'conflict'

const Container = styled.div<{ status: StatusLabelType }>`
  height: 24px;
  line-height: 22px;
  width: fit-content;
  border-radius: 12px;
  padding: 0 10px;
  text-align: center;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0;

  ${(p) =>
    p.status == 'coming'
      ? `
   color: ${colors.accents.emerald};
   border: 1px solid ${colors.accents.emerald};
  `
      : ''}

  ${(p) =>
    p.status == 'active'
      ? `
   color: ${colors.greyscale.white};
   background: ${colors.accents.emerald};
  `
      : ''}

  ${(p) =>
    p.status == 'completed'
      ? `
   color: ${colors.greyscale.dark};
   border: 1px solid ${colors.greyscale.medium};
  `
      : ''}

  ${(p) =>
    p.status == 'conflict'
      ? `
   color: ${colors.greyscale.white};
   background: ${colors.accents.red};
  `
      : ''}
`

export type Props =
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

  return <Container status={status}>{i18n.common.statuses[status]}</Container>
}

export default StatusLabel
