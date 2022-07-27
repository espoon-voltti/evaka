// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Dispatch, SetStateAction } from 'react'
import React, { createContext, useMemo, useState } from 'react'

import type { Result } from 'lib-common/api'
import { Loading } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'

import type { DaycareAclRow, UnitData, UnitResponse } from '../api/unit'
import { getDaycare, getDaycareAclRows, getUnitData } from '../api/unit'
import { UnitFilters } from '../utils/UnitFilters'

export interface UnitState {
  unitId: UUID
  unitInformation: Result<UnitResponse>
  unitData: Result<UnitData>
  reloadUnitData: () => void
  filters: UnitFilters
  setFilters: Dispatch<SetStateAction<UnitFilters>>
  daycareAclRows: Result<DaycareAclRow[]>
  reloadDaycareAclRows: () => void
}

const defaultState: UnitState = {
  unitId: '',
  unitInformation: Loading.of(),
  unitData: Loading.of(),
  reloadUnitData: () => undefined,
  filters: new UnitFilters(LocalDate.todayInSystemTz(), '1 day'),
  setFilters: () => undefined,
  daycareAclRows: Loading.of(),
  reloadDaycareAclRows: () => undefined
}

export const UnitContext = createContext<UnitState>(defaultState)

export const UnitContextProvider = React.memo(function UnitContextProvider({
  id,
  children
}: {
  id: UUID
  children: JSX.Element
}) {
  const [filters, setFilters] = useState(defaultState.filters)

  const [unitInformation] = useApiState(() => getDaycare(id), [id])
  const [unitData, reloadUnitData] = useApiState(
    () => getUnitData(id, filters.startDate, filters.endDate),
    [id, filters.startDate, filters.endDate]
  )

  const [daycareAclRows, reloadDaycareAclRows] = useApiState(
    () => getDaycareAclRows(id),
    [id]
  )

  const value = useMemo(
    () => ({
      unitId: id,
      unitInformation,
      unitData,
      reloadUnitData,
      filters,
      setFilters,
      daycareAclRows,
      reloadDaycareAclRows
    }),
    [
      id,
      unitInformation,
      unitData,
      reloadUnitData,
      filters,
      daycareAclRows,
      reloadDaycareAclRows
    ]
  )

  return <UnitContext.Provider value={value}>{children}</UnitContext.Provider>
})
