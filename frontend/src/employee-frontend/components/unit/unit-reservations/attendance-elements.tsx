// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { css } from 'styled-components'

import { Result } from 'lib-common/api'
import { OperationalDay } from 'lib-common/api-types/reservations'
import { TimeRange } from 'lib-common/generated/api-types/reservations'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { Td, Th, Thead, Tr, TrProps } from 'lib-components/layout/Table'
import { fontWeights, H4 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faCheck } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'

import { TimeRangeWithErrors } from './reservation-table-edit-state'

export const EditStateIndicator = React.memo(function EditStateIndicator({
  status,
  stopEditing
}: {
  status: Result<void>
  stopEditing: () => void
}) {
  return (
    <IconButton
      icon={faCheck}
      onClick={stopEditing}
      disabled={status.isLoading}
      data-qa="inline-editor-state-button"
    />
  )
})

const CustomTh = styled(Th)<{ shrink?: boolean }>`
  vertical-align: bottom;
  ${(p) =>
    p.shrink &&
    css`
      width: 0; // causes the column to take as little space as possible
    `}
`

const DateTh = styled(CustomTh)<{ faded: boolean }>`
  ${(p) => p.faded && `color: ${colors.grayscale.g35};`}
`

const DayHeader = styled.div`
  display: flex;
  justify-content: space-evenly;
  gap: ${defaultMargins.xs};
`

const Date = styled(H4)<{ highlight: boolean }>`
  margin: 0 0 ${defaultMargins.s};
  ${(p) =>
    p.highlight &&
    css`
      color: ${colors.main.m2};
      font-weight: ${fontWeights.semibold};
    `}
`

export const StyledTd = styled(Td)<{ partialRow: boolean; rowIndex: number }>`
  border-right: 1px solid ${colors.grayscale.g15};
  vertical-align: middle;
  ${(p) => p.partialRow && p.rowIndex === 0 && `border-bottom-style: dashed;`}
  ${(p) => p.partialRow && p.rowIndex > 0 && `border-top-style: dashed;`}
`

export const DayTd = styled(StyledTd)`
  padding: 0;
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
  &:last-child {
    > ${DayTd}.is-today {
      border-bottom: 2px solid ${colors.status.success};
    }
  }
`

export const NameTd = styled(StyledTd)`
  width: 350px;
  ${(p) => p.partialRow && p.rowIndex === 0 && `border-bottom-style: none;`}
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

interface AttendanceTableHeaderProps {
  operationalDays: OperationalDay[]
}

export const AttendanceTableHeader = React.memo(function AttendanceTableHeader({
  operationalDays
}: AttendanceTableHeaderProps) {
  const { i18n, lang } = useTranslation()
  return (
    <Thead>
      <Tr>
        <CustomTh />
        {operationalDays.map(({ date, isHoliday }) => (
          <DateTh shrink key={date.formatIso()} faded={isHoliday}>
            <Date centered highlight={date.isToday()}>
              {date.format('EEEEEE dd.MM.', lang)}
            </Date>
            <DayHeader>
              <span>{i18n.unit.attendanceReservations.startTime}</span>
              <span>{i18n.unit.attendanceReservations.endTime}</span>
            </DayHeader>
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
  update: (v: TimeRange) => void
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

const TimeInputWithoutPadding = styled(TimeInput)`
  padding: 0;
  width: calc(2.8em);
  max-width: calc(2.8em);
  background: transparent;

  &:focus {
    padding: 0;
  }
`
