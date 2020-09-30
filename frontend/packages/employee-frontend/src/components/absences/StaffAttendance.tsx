// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  memo,
  useCallback,
  useContext,
  useEffect,
  useState
} from 'react'
import { Table } from '~components/shared/alpha'
import { DisabledCell } from '~components/absences/AbsenceCell'
import { useTranslation } from '~state/i18n'
import { AbsencesContext } from '~state/absence'
import { StaffAttendance, StaffAttendanceGroup } from '~types/absence'
import { isSuccess, Loading, Result } from '~api'
import { getStaffAttendances, postStaffAttendance } from '~api/absences'
import './Absences.scss'
import { useDebounce } from '~utils/useDebounce'
import { formatDecimal } from '~components/utils'
import { faTimes } from 'icon-set'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import styled from 'styled-components'
import Tooltip from '~components/common/Tooltip'
import { EspooColours } from '~utils/colours'

type Props = {
  groupId: string
  emptyCols: number[]
}

export default memo(function StaffAttendance({ groupId, emptyCols }: Props) {
  const { selectedDate } = useContext(AbsencesContext)

  const [attendance, setAttendance] = useState<Result<StaffAttendanceGroup>>(
    Loading()
  )

  const refreshAttendances = useCallback(() => {
    void getStaffAttendances(groupId, {
      year: selectedDate.getYear(),
      month: selectedDate.getMonth()
    }).then(setAttendance)
  }, [groupId, selectedDate])

  useEffect(() => {
    refreshAttendances()
  }, [groupId, selectedDate])

  const updateAttendances = (attendance: StaffAttendance) =>
    postStaffAttendance(attendance).then(refreshAttendances)

  return isSuccess(attendance) ? (
    <StaffAttendanceRow
      emptyCols={emptyCols}
      attendanceGroup={attendance.data}
      updateAttendances={updateAttendances}
    />
  ) : null
})

interface StaffAttendanceRowProps {
  attendanceGroup: StaffAttendanceGroup
  emptyCols: number[]
  updateAttendances: (staffAttendance: StaffAttendance) => Promise<void>
}

const StaffAttendanceRow = memo(function StaffAttendanceRow({
  emptyCols,
  attendanceGroup,
  updateAttendances
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
    <Table.Row className={'staff-attendance-row'}>
      <Table.Td colSpan={2}>{i18n.absences.table.staffRow}</Table.Td>
      {Object.keys(attendanceMap)
        .sort()
        .map((key) => {
          return (
            <Table.Td key={key}>
              <StaffAttendanceCell
                updateAttendances={updateAttendances}
                attendance={attendanceMap[key]}
                disabled={isDisabled(attendanceMap[key])}
              />
            </Table.Td>
          )
        })}
      {emptyCols.map((item) => (
        <Table.Td key={item}>
          <DisabledCell />
        </Table.Td>
      ))}
    </Table.Row>
  )
})

interface StaffAttendanceCellProps {
  attendance: StaffAttendance
  updateAttendances: (attendance: StaffAttendance) => Promise<void>
  disabled: boolean
}

const DisabledStaffIcon = styled(FontAwesomeIcon)`
  font-size: 15px;
  color: ${EspooColours.greyDark};
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
          />
        )}
      </div>
    </div>
  )
})
