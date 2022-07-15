// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useEffect, useMemo } from 'react'

import { Result } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import Select from 'lib-components/atoms/dropdowns/Select'

import { useTranslation } from '../../../state/i18n'

interface Props {
  groups: Result<DaycareGroup[]>
  selected: UUID | 'no-group' | 'staff' | null
  onSelect: (val: UUID | 'no-group' | 'staff') => void
  'data-qa'?: string
  realtimeStaffAttendanceEnabled: boolean
}

export default React.memo(function GroupSelector({
  groups,
  selected,
  onSelect,
  'data-qa': dataQa,
  realtimeStaffAttendanceEnabled
}: Props) {
  const { i18n } = useTranslation()

  const options = useMemo(
    () => [
      ...groups
        .map((gs) =>
          sortBy(gs, ({ name }) => name)
            .filter(
              (group) =>
                group.id === selected ||
                new DateRange(group.startDate, group.endDate).includes(
                  LocalDate.todayInSystemTz()
                )
            )
            .map(({ id }) => id)
        )
        .getOrElse([]),
      'no-group',
      ...(realtimeStaffAttendanceEnabled ? ['staff'] : [])
    ],
    [groups, selected, realtimeStaffAttendanceEnabled]
  )

  const getItemLabel = useCallback(
    (item: string | null) =>
      groups
        .map((gs) => {
          switch (item) {
            case 'no-group':
              return i18n.unit.attendances.noGroup
            case 'staff':
              return i18n.unit.attendances.staff
            default:
              return gs.find(({ id }) => id === item)?.name ?? ''
          }
        })
        .getOrElse(''),
    [i18n, groups]
  )

  useEffect(() => {
    if (selected === null && groups.isSuccess) {
      const defaultSelection =
        sortBy(groups.value, [(g) => g.name.toLowerCase()]).find((group) =>
          new DateRange(group.startDate, group.endDate).includes(
            LocalDate.todayInSystemTz()
          )
        )?.id ?? 'no-group'
      onSelect(defaultSelection)
    }
  }, [selected, onSelect, groups])

  return selected ? (
    <Select
      selectedItem={selected}
      items={options}
      getItemLabel={getItemLabel}
      onChange={(group) => onSelect(group ?? 'no-group')}
      data-qa={dataQa}
    />
  ) : null
})
