// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useMemo } from 'react'

import DateRange from 'lib-common/date-range'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import Select from 'lib-components/atoms/dropdowns/Select'

import { useTranslation } from '../../../state/i18n'

type GroupSelectId = UUID | 'no-group' | 'staff' | 'all'

interface Props {
  groups: DaycareGroup[]
  selected: GroupSelectId
  onSelect: (val: GroupSelectId) => void
  'data-qa'?: string
  realtimeStaffAttendanceEnabled: boolean
  onlyRealGroups?: boolean
}

export default React.memo(function GroupSelector({
  groups,
  selected,
  onSelect,
  'data-qa': dataQa,
  realtimeStaffAttendanceEnabled,
  onlyRealGroups
}: Props) {
  const { i18n } = useTranslation()

  const options = useMemo(
    () => [
      ...sortBy(groups, ({ name }) => name)
        .filter(
          (group) =>
            group.id === selected ||
            new DateRange(group.startDate, group.endDate).includes(
              LocalDate.todayInSystemTz()
            )
        )
        .map(({ id }) => id),
      ...(onlyRealGroups ? [] : ['no-group', 'all']),
      ...(!onlyRealGroups && realtimeStaffAttendanceEnabled ? ['staff'] : [])
    ],
    [groups, selected, onlyRealGroups, realtimeStaffAttendanceEnabled]
  )

  const getItemLabel = useCallback(
    (item: string | null) => {
      switch (item) {
        case 'no-group':
          return i18n.unit.calendar.noGroup
        case 'staff':
          return i18n.unit.calendar.staff
        case 'all':
          return i18n.unit.calendar.all
        default:
          return groups.find(({ id }) => id === item)?.name ?? ''
      }
    },
    [i18n, groups]
  )

  return (
    <Select
      selectedItem={selected}
      items={options}
      getItemLabel={getItemLabel}
      onChange={(group) => onSelect(group ?? 'no-group')}
      data-qa={dataQa}
    />
  )
})
