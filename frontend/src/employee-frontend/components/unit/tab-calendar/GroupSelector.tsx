// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import _ from 'lodash'
import React, { useCallback, useEffect, useMemo } from 'react'
import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import Select from 'lib-components/atoms/dropdowns/Select'
import { getDaycareGroups } from '../../../api/unit'
import { useTranslation } from '../../../state/i18n'

interface Props {
  unitId: UUID
  selected: UUID | 'no-group' | null
  onSelect: (val: UUID | 'no-group') => void
  'data-qa'?: string
}

export default React.memo(function GroupSelector({
  unitId,
  selected,
  onSelect,
  'data-qa': dataQa
}: Props) {
  const { i18n } = useTranslation()
  const [groups] = useApiState(() => getDaycareGroups(unitId), [unitId])

  const options = useMemo(
    () => [
      ...groups
        .map((gs) =>
          _.sortBy(gs, ({ name }) => name)
            .filter(
              (group) =>
                group.id === selected ||
                new DateRange(group.startDate, group.endDate).includes(
                  LocalDate.today()
                )
            )
            .map(({ id }) => id)
        )
        .getOrElse([]),
      'no-group'
    ],
    [groups, selected]
  )

  const getItemLabel = useCallback(
    (item: string | null) =>
      groups
        .map((gs) =>
          item === 'no-group'
            ? i18n.unit.calendar.noGroup
            : gs.find(({ id }) => id === item)?.name ?? ''
        )
        .getOrElse(''),
    [i18n, groups]
  )

  useEffect(() => {
    if (selected === null && groups.isSuccess) {
      const defaultSelection =
        _.sortBy(groups.value, [(g) => g.name.toLowerCase()]).find((group) =>
          new DateRange(group.startDate, group.endDate).includes(
            LocalDate.today()
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
