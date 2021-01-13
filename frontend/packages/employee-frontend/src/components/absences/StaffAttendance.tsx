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
import { Td, Tr } from '@evaka/lib-components/src/layout/Table'
import { DisabledCell } from '~components/absences/AbsenceCell'
import { useTranslation } from '~state/i18n'
import { AbsencesContext } from '~state/absence'
import { StaffAttendance, StaffAttendanceGroup } from '~types/absence'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { getStaffAttendances, postStaffAttendance } from '~api/absences'
import './Absences.scss'
import { useDebounce } from '~utils/useDebounce'
import { formatDecimal } from '~components/utils'
import { faTimes } from '@evaka/lib-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import styled from 'styled-components'
import Tooltip from '~components/common/Tooltip'
import { isOperationDay } from '~components/absences/utils'
import { DayOfWeek } from '~types'
import colors from '@evaka/lib-components/src/colors'

type Props = {
  groupId: string
  emptyCols: number[]
  operationDays: DayOfWeek[]
}

export default memo(function StaffAttendance({
  groupId,
  emptyCols,
  operationDays
}: Props) {
  const isMountedRef = useRef(true)
  const { selectedDate } = useContext(AbsencesContext)

  const [attendance, setAttendance] = useState<Result<StaffAttendanceGroup>>(
    Loading.of()
  )

  const refreshAttendances = useCallback(() => {
    isMountedRef.current = true

    void getStaffAttendances(groupId, {
      year: selectedDate.getYear(),
      month: selectedDate.getMonth()
    }).then((r) => {
      if (isMountedRef.current) {
        setAttendance(r)
      }
    })

    return () => {
      isMountedRef.current = false
    }
  }, [groupId, selectedDate])

  useEffect(() => {
    isMountedRef.current = true

    if (isMountedRef.current) {
      refreshAttendances()
    }

    return () => {
      isMountedRef.current = false
    }
  }, [groupId, selectedDate])

  const updateAttendances = (attendance: StaffAttendance) =>
    postStaffAttendance(attendance).then(refreshAttendances)

  return attendance.isSuccess ? (
    <StaffAttendanceRow
      emptyCols={emptyCols}
      attendanceGroup={attendance.value}
      updateAttendances={updateAttendances}
      operationDays={operationDays}
    />
  ) : null
})

interface StaffAttendanceRowProps {
  attendanceGroup: StaffAttendanceGroup
  emptyCols: number[]
  updateAttendances: (staffAttendance: StaffAttendance) => Promise<() => void>
  operationDays: DayOfWeek[]
}

const StaffAttendanceRow = memo(function StaffAttendanceRow({
  emptyCols,
  attendanceGroup,
  updateAttendances,
  operationDays
}: StaffAttendanceRowProps) {
  const { i18n } = useTranslation()
  const attendanceMap = attendanceGroup.attendances

  const isDisabled = (attendance: StaffAttendance): boolean => {
    return attendance.date < attendanceGroup.startDate
      ? true
      : !attendanceGroup.endDate
      ? false
      : attendance.date > attendanceGroup.endDate
  }

  return (
    <Tr className={'staff-attendance-row'}>
      <Td colSpan={2}>{i18n.absences.table.staffRow}</Td>
      {Object.keys(attendanceMap)
        .sort()
        .map((key) => {
          return isOperationDay(attendanceMap[key].date, operationDays) ? (
            <Td key={key}>
              <StaffAttendanceCell
                updateAttendances={updateAttendances}
                attendance={attendanceMap[key]}
                disabled={isDisabled(attendanceMap[key])}
              />
            </Td>
          ) : (
            <Td key={key}>
              <DisabledCell />
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

interface StaffAttendanceCellProps {
  attendance: StaffAttendance
  updateAttendances: (attendance: StaffAttendance) => Promise<() => void>
  disabled: boolean
}

const DisabledStaffIcon = styled(FontAwesomeIcon)`
  font-size: 15px;
  color: ${colors.greyscale.dark};
`

const StaffAttendanceCell = memo(function StaffAttendanceCell({
  attendance,
  updateAttendances,
  disabled
}: StaffAttendanceCellProps) {
  const [inputValue, setInputValue] = useState(formatDecimal(attendance.count))
  const [updatedValue, setUpdatedValue] = useState<number>()
  const [editing, setEditing] = useState(false)
  const { i18n } = useTranslation()

  useEffect(() => {
    if (!editing) {
      setInputValue(formatDecimal(attendance.count))
    }
  }, [attendance, editing])

  const debouncedValue = useDebounce(updatedValue, 400)

  useEffect(() => {
    if (debouncedValue !== undefined) {
      updateAttendances({
        ...attendance,
        count: debouncedValue
      }).catch((e) => {
        console.error(e)
        setInputValue(formatDecimal(attendance.count))
      })

      setUpdatedValue(undefined)
    }
  }, [debouncedValue])

  const update = (event: React.ChangeEvent<HTMLInputElement>) => {
    setUpdatedValue(parseFloat(event.target.value.replace(',', '.')) || 0)
    setInputValue(event.target.value)
  }

  const toggleEditing = useCallback(() => setEditing((prev) => !prev), [
    editing
  ])

  const DisabledStaffCell = () => (
    <div className={'absence-cell disabled-staff-cell-container'}>
      <Tooltip
        tooltipId={`tooltip_disabled-staff-cell-${attendance.date.formatIso()}`}
        tooltipText={i18n.absences.table.disabledStaffCellTooltip}
        place={'top'}
      >
        <DisabledStaffIcon icon={faTimes} />
      </Tooltip>
    </div>
  )

  return (
    <div className={'field staff-attendance-cell'}>
      {/*input component from styleguide needs to be updated to include onKey events*/}
      <div className={'control'}>
        {disabled ? (
          <DisabledStaffCell />
        ) : (
          <input
            className={'input'}
            value={inputValue ? inputValue + '' : ''}
            onChange={update}
            onFocus={toggleEditing}
            onBlur={toggleEditing}
            data-qa="staff-attendance-input"
          />
        )}
      </div>
    </div>
  )
})
