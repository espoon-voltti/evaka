// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import { UUID } from 'lib-common/types'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { Loading, Result } from 'lib-common/api'
import { DaycareGroup } from '../../../types/unit'
import { getDaycareGroups } from '../../../api/unit'
import SimpleSelect from 'lib-components/atoms/form/SimpleSelect'
import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'
import _ from 'lodash'

interface Props {
  unitId: UUID
  selected: UUID | 'no-group' | null
  onSelect: (val: UUID | 'no-group') => void
}

export default React.memo(function GroupSelector({
  unitId,
  selected,
  onSelect
}: Props) {
  const [groups, setGroups] = useState<Result<DaycareGroup[]>>(Loading.of())
  const loadGroups = useRestApi(getDaycareGroups, setGroups)
  useEffect(() => loadGroups(unitId), [unitId, loadGroups])

  const options = useMemo(
    () => [
      ..._.sortBy(
        groups
          .getOrElse([])
          .filter(
            (group) =>
              group.id === selected ||
              new DateRange(group.startDate, group.endDate).includes(
                LocalDate.today()
              )
          )
          .map((group) => ({ label: group.name, value: group.id })),
        [(i) => i.label.toLowerCase()]
      ),
      { label: 'Ei ryhmää', value: 'no-group' }
    ],
    [groups, selected]
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
    <SimpleSelect
      value={selected}
      options={options}
      onChange={(e) => onSelect(e.target.value)}
      placeholder={''}
    />
  ) : null
})
