// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  memo,
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState
} from 'react'
import LocalDate from 'lib-common/local-date'
import { Td, Tr } from 'lib-components/layout/Table'
import { GroupStaffAttendanceForDates } from 'lib-common/api-types/codegen-excluded'
import { GroupStaffAttendance } from 'lib-common/generated/api-types/daycare'
import { DisabledCell } from './AbsenceCell'
import { useTranslation } from '../../state/i18n'
import { Result } from 'lib-common/api'
import { getStaffAttendances, postStaffAttendance } from '../../api/absences'
import { formatDecimal, stringToNumber } from 'lib-common/utils/number'
import { faTimes } from 'lib-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import styled from 'styled-components'
import Tooltip from '../../components/common/Tooltip'
import colors from 'lib-customizations/common'
import { useApiState } from 'lib-common/utils/useRestApi'

type Props = {
  groupId: string
  selectedDate: LocalDate
  emptyCols: number[]
  operationDays: LocalDate[]
}

export default memo(function StaffAttendance({
  groupId,
  selectedDate,
  emptyCols,
  operationDays
}: Props) {
  const isMountedRef = useRef(true)
  useEffect(
    () => () => {
      isMountedRef.current = false
    },
    []
  )

  const [attendance, refreshAttendances] = useApiState(() => {
    const year = selectedDate.getYear()
    const month = selectedDate.getMonth()

    return getStaffAttendances(groupId, { year, month })
  }, [groupId, selectedDate])

  const updateAttendance = (date: LocalDate, count: number) =>
    postStaffAttendance({
      groupId,
      date,
      count,
      countOther: null
    }).then(refreshAttendances)

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
  updateAttendance: (date: LocalDate, count: number) => Promise<void>
  daysOfMonth: LocalDate[]
  operationDays: LocalDate[]
}

const StaffAttendanceRow = memo(function StaffAttendanceRow({
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
    <Tr className={'staff-attendance-row'}>
      <Td>{i18n.absences.table.staffRow}</Td>
      {daysOfMonth.map((date) => {
        const attendance = groupAttendances.map(({ attendances }) =>
          attendances.get(date.toString())
        )
        return (
          <Td key={date.toString()}>
            {!isOperational(date) ? (
              <DisabledCell />
            ) : !isActive(date) ? (
              <InactiveCell date={date} />
            ) : (
              <StaffAttendanceCell
                updateAttendance={(count: number) =>
                  updateAttendance(date, count)
                }
                attendance={attendance}
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
  color: ${colors.greyscale.dark};
`

const InactiveCell = ({ date }: { date: LocalDate }) => {
  const { i18n } = useTranslation()
  return (
    <div className={'absence-cell disabled-staff-cell-container'}>
      <Tooltip
        tooltipId={`tooltip_disabled-staff-cell-${date.formatIso()}`}
        tooltipText={i18n.absences.table.disabledStaffCellTooltip}
        place={'top'}
      >
        <DisabledStaffIcon icon={faTimes} />
      </Tooltip>
    </div>
  )
}

interface StaffAttendanceCellProps {
  attendance: Result<GroupStaffAttendance | undefined>
  updateAttendance: (count: number) => Promise<void>
}

const StaffAttendanceCell = memo(function StaffAttendanceCell({
  attendance,
  updateAttendance
}: StaffAttendanceCellProps) {
  const mountedRef = useRef(true)
  useEffect(
    () => () => {
      mountedRef.current = false
    },
    []
  )

  const initialValue = attendance
    .map((a) => formatDecimal(a?.count) ?? '')
    .getOrElse('')

  const [value, setValue] = useState(initialValue)

  useLayoutEffect(() => {
    setValue(initialValue)
  }, [initialValue])

  const disableInput =
    attendance.isLoading || (attendance.isSuccess && attendance.isReloading)

  const save = useCallback(() => {
    if (value !== initialValue) {
      const numberValue = stringToNumber(value)
      if (numberValue !== undefined) {
        updateAttendance(numberValue).catch(() => {
          if (mountedRef.current) {
            setValue(initialValue)
          }
        })
      }
    }
  }, [value, initialValue, updateAttendance])

  // Save after a timeout
  useEffect(() => {
    const handle = setTimeout(() => {
      void save()
    }, 2000)

    return () => {
      if (mountedRef.current) {
        clearTimeout(handle)
      }
    }
  }, [save])

  return (
    <div className="field staff-attendance-cell">
      <div className="control">
        <input
          className="input"
          value={value}
          onChange={(e) => {
            setValue(e.target.value)
          }}
          onBlur={save}
          disabled={disableInput}
          data-qa="staff-attendance-input"
        />
      </div>
    </div>
  )
})
