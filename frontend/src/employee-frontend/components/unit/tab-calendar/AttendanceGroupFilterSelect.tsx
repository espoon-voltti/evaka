// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useMemo } from 'react'

import DateRange from 'lib-common/date-range'
import type { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import Select from 'lib-components/atoms/dropdowns/Select'

import { useTranslation } from '../../../state/i18n'
import type { AttendanceGroupFilter } from '../TabCalendar'

interface Props {
  groups: DaycareGroup[]
  value: AttendanceGroupFilter
  onChange: (val: AttendanceGroupFilter) => void
  'data-qa'?: string
  realtimeStaffAttendanceEnabled: boolean
}

export default React.memo(function AttendanceGroupFilterSelect({
  groups,
  value,
  onChange,
  'data-qa': dataQa,
  realtimeStaffAttendanceEnabled
}: Props) {
  const { i18n } = useTranslation()

  const options = useMemo(() => {
    const result: AttendanceGroupFilter[] = sortBy(groups, ({ name }) => name)
      .filter(
        (group) =>
          (value.type === 'group' && value.id === group.id) ||
          new DateRange(group.startDate, group.endDate).overlapsWith(
            new DateRange(LocalDate.todayInHelsinkiTz(), null)
          )
      )
      .map((group) => ({ type: 'group', id: group.id }))
    result.push(
      { type: 'no-group' },
      { type: 'shiftcare' },
      { type: 'all-children' }
    )
    if (realtimeStaffAttendanceEnabled) {
      result.push({ type: 'staff' })
    }
    return result
  }, [groups, value, realtimeStaffAttendanceEnabled])

  const getItemLabel = useCallback(
    (item: AttendanceGroupFilter) => {
      switch (item.type) {
        case 'no-group':
          return i18n.unit.calendar.noGroup
        case 'shiftcare':
          return i18n.unit.calendar.shiftcare
        case 'staff':
          return i18n.unit.calendar.staff
        case 'all-children':
          return i18n.unit.calendar.allChildren
        default:
          return groups.find(({ id }) => id === item.id)?.name ?? ''
      }
    },
    [i18n, groups]
  )

  return (
    <Select
      selectedItem={value}
      items={options}
      getItemValue={(item: AttendanceGroupFilter) =>
        item.type === 'group' ? item.id : item.type
      }
      getItemLabel={getItemLabel}
      onChange={(value) => onChange(value ?? { type: 'no-group' })}
      data-qa={dataQa}
    />
  )
})
