// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  memo,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState
} from 'react'
import LocalDate from 'lib-common/local-date'
import { Td, Tr } from 'lib-components/layout/Table'
import { DisabledCell } from './AbsenceCell'
import { useTranslation } from '../../state/i18n'
import { AbsencesContext } from '../../state/absence'
import {
  GroupStaffAttendance,
  GroupStaffAttendanceForDates
} from 'lib-common/api-types/staffAttendances'
import { Loading, Result } from 'lib-common/api'
import { getStaffAttendances, postStaffAttendance } from '../../api/absences'
import { useDebounce } from 'lib-common/utils/useDebounce'
import { formatDecimal } from 'lib-common/utils/number'
import { faTimes } from 'lib-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import styled from 'styled-components'
import Tooltip from '../../components/common/Tooltip'
import colors from 'lib-customizations/common'

type Props = {
  groupId: string
  emptyCols: number[]
  operationDays: LocalDate[]
}

export default memo(function StaffAttendance({
  groupId,
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

  const { selectedDate } = useContext(AbsencesContext)

  const [attendance, setAttendance] = useState<
    Result<GroupStaffAttendanceForDates>
  >(Loading.of())

  const refreshAttendances = useCallback(() => {
    const year = selectedDate.getYear()
    const month = selectedDate.getMonth()

    void getStaffAttendances(groupId, { year, month }).then((r) => {
      if (isMountedRef.current) {
        setAttendance(r)
      }
    })
  }, [groupId, selectedDate])

  useEffect(() => {
    refreshAttendances()
  }, [refreshAttendances])

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

  return attendance.isSuccess ? (
    <StaffAttendanceRow
      emptyCols={emptyCols}
      attendanceGroup={attendance.value}
      updateAttendance={updateAttendance}
      daysOfMonth={daysOfMonth}
      operationDays={operationDays}
    />
  ) : null
})

interface StaffAttendanceRowProps {
  attendanceGroup: GroupStaffAttendanceForDates
  emptyCols: number[]
  updateAttendance: (date: LocalDate, count: number) => Promise<void>
  daysOfMonth: LocalDate[]
  operationDays: LocalDate[]
}

const StaffAttendanceRow = memo(function StaffAttendanceRow({
  emptyCols,
  attendanceGroup,
  updateAttendance,
  daysOfMonth,
  operationDays
}: StaffAttendanceRowProps) {
  const { i18n } = useTranslation()
  const attendanceMap = attendanceGroup.attendances

  const isActive = (date: LocalDate): boolean =>
    date.isEqualOrAfter(attendanceGroup.startDate) &&
    (!attendanceGroup.endDate || date.isEqualOrBefore(attendanceGroup.endDate))

  const isOperational = (date: LocalDate) =>
    operationDays.some((operationDay) => operationDay.isEqual(date))

  return (
    <Tr className={'staff-attendance-row'}>
      <Td colSpan={2}>{i18n.absences.table.staffRow}</Td>
      {daysOfMonth.map((date) => {
        const attendance = attendanceMap.get(date.toString())
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
  attendance: GroupStaffAttendance | undefined
  updateAttendance: (count: number) => Promise<void>
}

const StaffAttendanceCell = memo(function StaffAttendanceCell({
  attendance,
  updateAttendance
}: StaffAttendanceCellProps) {
  const initial: number | undefined = attendance?.count
  const [inputValue, setInputValue] = useState(formatDecimal(initial))
  const [updatedValue, setUpdatedValue] = useState<number>()
  const [editing, setEditing] = useState(false)

  useEffect(() => {
    if (!editing) {
      setInputValue(formatDecimal(initial))
    }
  }, [initial, editing])

  const debouncedValue = useDebounce(updatedValue, 400)

  useEffect(() => {
    if (debouncedValue !== undefined) {
      updateAttendance(debouncedValue).catch(() => {
        setInputValue(formatDecimal(initial))
      })
      setUpdatedValue(undefined)
    }
  }, [updateAttendance, debouncedValue, initial])

  const update = (event: React.ChangeEvent<HTMLInputElement>) => {
    setUpdatedValue(parseFloat(event.target.value.replace(',', '.')) || 0)
    setInputValue(event.target.value)
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const toggleEditing = useCallback(() => setEditing((prev) => !prev), [
    editing
  ])

  return (
    <div className={'field staff-attendance-cell'}>
      {/*input component from styleguide needs to be updated to include onKey events*/}
      <div className={'control'}>
        <input
          className={'input'}
          value={inputValue ? inputValue + '' : ''}
          onChange={update}
          onFocus={toggleEditing}
          onBlur={toggleEditing}
          data-qa="staff-attendance-input"
        />
      </div>
    </div>
  )
})
