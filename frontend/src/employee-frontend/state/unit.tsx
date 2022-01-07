// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  Dispatch,
  SetStateAction,
  useMemo,
  useState
} from 'react'
import { Loading, Result } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { getDaycare, getUnitData, UnitData, UnitResponse } from '../api/unit'
import { UnitFilters } from '../utils/UnitFilters'

export interface UnitState {
  unitInformation: Result<UnitResponse>
  unitData: Result<UnitData>
  reloadUnitData: () => void
  filters: UnitFilters
  setFilters: Dispatch<SetStateAction<UnitFilters>>
}

const defaultState: UnitState = {
  unitInformation: Loading.of(),
  unitData: Loading.of(),
  reloadUnitData: () => undefined,
  filters: new UnitFilters(LocalDate.today(), '3 months'),
  setFilters: () => undefined
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

  const value = useMemo(
    () => ({
      unitInformation,
      unitData,
      reloadUnitData,
      filters,
      setFilters
    }),
    [unitInformation, unitData, reloadUnitData, filters]
  )

  return <UnitContext.Provider value={value}>{children}</UnitContext.Provider>
})
