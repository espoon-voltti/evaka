// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { css } from 'styled-components'

import { OperationalDay } from 'lib-common/generated/api-types/reservations'
import { Td, Th, Thead, Tr, TrProps } from 'lib-components/layout/Table'
import { LabelLike } from 'lib-components/typography'
import { defaultMargins, SpacingSize } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../../state/i18n'
import { stickyTopBarHeight } from '../TabCalendar'

interface CustomThProps {
  shrink?: boolean
}

const CustomTh = styled(Th)<CustomThProps>`
  width: 100px;
  vertical-align: bottom;
  font-size: 16px;
  text-transform: unset;

  ${(p) =>
    p.shrink &&
    css`
      width: 0; // causes the column to take as little space as possible
    `}
`

// Use min- and max-width to ensure that columns in two tables in the same layout are aligned
const DateTh = styled(CustomTh)<CustomThProps & { faded: boolean }>`
  max-width: 150px;
  ${(p) => p.faded && `color: ${colors.grayscale.g35};`}
`

const Date = styled(LabelLike)<{ highlight: boolean }>`
  text-align: center;
  text-transform: capitalize;

  ${(p) =>
    p.highlight &&
    css`
      color: ${colors.main.m1};
    `}
`

interface StyledTdProps {
  partialRow: boolean
  rowIndex: number
  maxRows?: number
  width?: number
}

export const StyledTd = styled(Td)<StyledTdProps>`
  border-right: 1px solid ${colors.grayscale.g15};
  vertical-align: middle;
  ${(p) =>
    p.partialRow &&
    p.rowIndex < (p.maxRows ?? 1) &&
    `border-bottom-style: dashed;`}
  ${(p) => p.partialRow && p.rowIndex > 0 && `border-top-style: dashed;`}
  ${(p) => p.width && `width: ${p.width}px;`}
`

export const DayTd = styled(StyledTd)`
  padding: 0;
  vertical-align: top;
`

export const DayTr = styled(Tr)<TrProps>`
  > ${DayTd}.is-today {
    border-left: 2px solid ${colors.status.success};
    border-right: 2px solid ${colors.status.success};
  }

  &:first-child {
    > ${DayTd}.is-today {
      border-top: 2px solid ${colors.status.success};
    }
  }
  &:last-child(2) {
    > ${DayTd}.is-today {
      border-bottom: 2px solid ${colors.status.success};
    }
  }
`

export const NameTd = styled(StyledTd)<StyledTdProps>`
  ${(p) =>
    p.partialRow &&
    p.rowIndex < (p.maxRows ?? 1) &&
    `border-bottom-style: none;`}
  ${(p) => p.partialRow && p.rowIndex > 0 && `border-top-style: none;`}
`

export const NameWrapper = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: flex-start;
  align-items: center;

  a {
    margin-left: ${defaultMargins.xs};
  }
`

export const ChipWrapper = styled.div<{ spacing?: SpacingSize }>`
  > * {
    margin-right: ${(p) =>
      p.spacing ? defaultMargins[p.spacing] : defaultMargins.s};
    &:last-child {
      margin-right: 0;
    }
  }
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: flex-start;
  align-items: center;
`

interface AttendanceTableHeaderProps {
  operationalDays: OperationalDay[]
  nameColumnLabel: string
}

export const AttendanceTableHeader = React.memo(function AttendanceTableHeader({
  operationalDays,
  nameColumnLabel
}: AttendanceTableHeaderProps) {
  const { lang } = useTranslation()
  return (
    <Thead sticky={`${stickyTopBarHeight}px`}>
      <Tr>
        <CustomTh>
          <LabelLike>{nameColumnLabel}</LabelLike>
        </CustomTh>
        {operationalDays.map(({ date, dateInfo }) => (
          <DateTh shrink key={date.formatIso()} faded={dateInfo.isHoliday}>
            <Date highlight={date.isToday()}>
              {date.format('EEEEEE d.M.', lang)}
            </Date>
          </DateTh>
        ))}
      </Tr>
    </Thead>
  )
})
