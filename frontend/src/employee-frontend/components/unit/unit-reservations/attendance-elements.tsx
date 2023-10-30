// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { css } from 'styled-components'

import { Result } from 'lib-common/api'
import { OperationalDay } from 'lib-common/api-types/reservations'
import { JsonOf } from 'lib-common/json'
import { TimeRange } from 'lib-common/reservations'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { Td, Th, Thead, Tr, TrProps } from 'lib-components/layout/Table'
import { LabelLike } from 'lib-components/typography'
import { defaultMargins, SpacingSize } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faCheck } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { stickyTopBarHeight } from '../TabCalendar'

import { TimeRangeWithErrors } from './reservation-table-edit-state'

export const EditStateIndicator = React.memo(function EditStateIndicator({
  status,
  stopEditing
}: {
  status: Result<void>
  stopEditing: () => void
}) {
  const { i18n } = useTranslation()

  return (
    <IconButton
      icon={faCheck}
      onClick={stopEditing}
      disabled={status.isLoading}
      data-qa="inline-editor-state-button"
      aria-label={i18n.common.save}
    />
  )
})

interface CustomThProps {
  shrink?: boolean
}

const CustomTh = styled(Th)<CustomThProps>`
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
  min-width: 150px;
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
}

export const StyledTd = styled(Td)<StyledTdProps>`
  border-right: 1px solid ${colors.grayscale.g15};
  vertical-align: middle;
  ${(p) =>
    p.partialRow &&
    p.rowIndex < (p.maxRows ?? 1) &&
    `border-bottom-style: dashed;`}
  ${(p) => p.partialRow && p.rowIndex > 0 && `border-top-style: dashed;`}
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
  width: 350px;
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
        {operationalDays.map(({ date, isHoliday }) => (
          <DateTh shrink key={date.formatIso()} faded={isHoliday}>
            <Date highlight={date.isToday()}>
              {date.format('EEEEEE d.M.', lang)}
            </Date>
          </DateTh>
        ))}
        <CustomTh shrink />
      </Tr>
    </Thead>
  )
})

export const TimeRangeEditor = React.memo(function TimeRangeEditor({
  timeRange,
  update,
  save
}: {
  timeRange: TimeRangeWithErrors
  update: (v: JsonOf<TimeRange>) => void
  save: () => void
}) {
  const { startTime, endTime, errors } = timeRange

  return (
    <>
      <TimeInputWithoutPadding
        value={startTime}
        onChange={(value) => update({ startTime: value, endTime })}
        onBlur={save}
        info={errors.startTime ? { status: 'warning', text: '' } : undefined}
        data-qa="input-start-time"
      />
      <TimeInputWithoutPadding
        value={endTime}
        onChange={(value) => update({ startTime, endTime: value })}
        onBlur={save}
        info={errors.endTime ? { status: 'warning', text: '' } : undefined}
        data-qa="input-end-time"
      />
    </>
  )
})

export const TimeInputWithoutPadding = styled(TimeInput)`
  padding: 0;
  width: calc(2.8em);
  max-width: calc(2.8em);
  background: transparent;

  &:focus {
    padding: 0;
  }
`
