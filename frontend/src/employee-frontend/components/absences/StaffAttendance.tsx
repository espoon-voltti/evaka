// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, {
  ChangeEvent,
  useCallback,
  useEffect,
  useMemo,
  useRef
} from 'react'
import styled from 'styled-components'
import { isLoading, Result } from 'lib-common/api'
import { GroupStaffAttendanceForDates } from 'lib-common/api-types/codegen-excluded'
import LocalDate from 'lib-common/local-date'
import { isAutomatedTest } from 'lib-common/utils/helpers'
import { formatDecimal, stringToNumber } from 'lib-common/utils/number'
import { useDebouncedSave } from 'lib-common/utils/useDebouncedSave'
import { useApiState } from 'lib-common/utils/useRestApi'
import { Td, Tr } from 'lib-components/layout/Table'
import colors from 'lib-customizations/common'
import { faTimes } from 'lib-icons'
import { getStaffAttendances, postStaffAttendance } from '../../api/absences'
import Tooltip from '../../components/common/Tooltip'
import { useTranslation } from '../../state/i18n'
import { DisabledCell } from './AbsenceCell'

type Props = {
  groupId: string
  selectedDate: LocalDate
  emptyCols: number[]
  operationDays: LocalDate[]
}

export default React.memo(function StaffAttendance({
  groupId,
  selectedDate,
  emptyCols,
  operationDays
}: Props) {
  const [attendance] = useApiState(() => {
    const year = selectedDate.getYear()
    const month = selectedDate.getMonth()

    return getStaffAttendances(groupId, { year, month })
  }, [groupId, selectedDate])

  const updateAttendance = useCallback(
    (date: LocalDate, count: number) =>
      postStaffAttendance({
        groupId,
        date,
        count,
        countOther: null
      }),
    [groupId]
  )

  const firstOfMonth = LocalDate.of(selectedDate.year, selectedDate.month, 1)
  const lastOfMonth = firstOfMonth.lastDayOfMonth()
  const daysOfMonth = LocalDate.range(firstOfMonth, lastOfMonth)

  return (
    <StaffAttendanceRow
      emptyCols={emptyCols}
      groupAttendances={attendance}
      updateAttendance={updateAttendance}
      daysOfMonth={daysOfMonth}
      operationDays={operationDays}
    />
  )
})

interface StaffAttendanceRowProps {
  groupAttendances: Result<GroupStaffAttendanceForDates>
  emptyCols: number[]
  updateAttendance: (date: LocalDate, count: number) => Promise<unknown>
  daysOfMonth: LocalDate[]
  operationDays: LocalDate[]
}

const StaffAttendanceRow = React.memo(function StaffAttendanceRow({
  emptyCols,
  groupAttendances,
  updateAttendance,
  daysOfMonth,
  operationDays
}: StaffAttendanceRowProps) {
  const { i18n } = useTranslation()

  const isActive = (date: LocalDate): boolean =>
    groupAttendances
      .map(
        ({ startDate, endDate }) =>
          date.isEqualOrAfter(startDate) &&
          (!endDate || date.isEqualOrBefore(endDate))
      )
      .getOrElse(true)

  const isOperational = (date: LocalDate) =>
    operationDays.some((operationDay) => operationDay.isEqual(date))

  return (
    <Tr className="staff-attendance-row">
      <Td>{i18n.absences.table.staffRow}</Td>
      {daysOfMonth.map((date) => {
        const staffCount = groupAttendances
          .map(({ attendances }) => attendances.get(date.toString()))
          .map((attendance) => attendance?.count)
        return (
          <Td key={date.toString()}>
            {!isOperational(date) ||
            !staffCount.isSuccess ||
            isLoading(staffCount) ? (
              <DisabledCell />
            ) : !isActive(date) ? (
              <InactiveCell date={date} />
            ) : (
              <StaffAttendanceCell
                date={date}
                initialCount={staffCount.value}
                updateAttendance={updateAttendance}
              />
            )}
          </Td>
        )
      })}
      {emptyCols.map((item) => (
        <Td key={item}>
          <DisabledCell />
        </Td>
      ))}
    </Tr>
  )
})

const DisabledStaffIcon = styled(FontAwesomeIcon)`
  font-size: 15px;
  color: ${colors.grayscale.g70};
`

const InactiveCell = ({ date }: { date: LocalDate }) => {
  const { i18n } = useTranslation()
  return (
    <div className="absence-cell disabled-staff-cell-container">
      <Tooltip
        tooltipId={`tooltip_disabled-staff-cell-${date.formatIso()}`}
        tooltipText={i18n.absences.table.disabledStaffCellTooltip}
        place="top"
      >
        <DisabledStaffIcon icon={faTimes} />
      </Tooltip>
    </div>
  )
}

interface StaffAttendanceCellProps {
  date: LocalDate
  initialCount: number | undefined
  updateAttendance: (date: LocalDate, count: number) => Promise<unknown>
}

const StaffAttendanceCell = React.memo(function StaffAttendanceCell({
  date,
  initialCount,
  updateAttendance
}: StaffAttendanceCellProps) {
  const mountedRef = useRef(true)
  useEffect(
    () => () => {
      mountedRef.current = false
    },
    []
  )

  const initialValue = useMemo(
    () => formatDecimal(initialCount) ?? '',
    [initialCount]
  )

  const save = useCallback(
    async (value: string) => {
      const numberValue = stringToNumber(value)
      if (numberValue !== undefined) {
        await updateAttendance(date, numberValue)
      }
    },
    [date, updateAttendance]
  )

  const { value, setValue, saveImmediately, state } = useDebouncedSave(
    initialValue,
    save,
    isAutomatedTest ? 200 : 2000
  )

  const handleChange = useCallback(
    (e: ChangeEvent<HTMLInputElement>) => {
      setValue(e.target.value)
    },
    [setValue]
  )

  return (
    <div
      className="field staff-attendance-cell"
      data-qa="staff-attendance-cell"
      data-state={state}
    >
      <div className="control">
        <input
          className="input"
          value={value}
          onChange={handleChange}
          onBlur={saveImmediately}
        />
      </div>
    </div>
  )
})
