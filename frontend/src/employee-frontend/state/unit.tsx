// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  useMemo,
  useState,
  useCallback,
  Dispatch,
  SetStateAction
} from 'react'
import { Loading, Result } from 'lib-common/api'
import { UnitData, UnitResponse } from '../api/unit'
import { UnitFilters } from '../utils/UnitFilters'
import LocalDate from 'lib-common/local-date'

export interface UnitState {
  unitInformation: Result<UnitResponse>
  setUnitInformation: (res: Result<UnitResponse>) => void
  filters: UnitFilters
  setFilters: Dispatch<SetStateAction<UnitFilters>>
  unitData: Result<UnitData>
  setUnitData: (res: Result<UnitData>) => void
  scrollToPosition: () => void
  savePosition: () => void
}

const defaultState: UnitState = {
  unitInformation: Loading.of(),
  setUnitInformation: () => undefined,
  filters: new UnitFilters(LocalDate.today(), '3 months'),
  setFilters: () => undefined,
  unitData: Loading.of(),
  setUnitData: () => undefined,
  scrollToPosition: () => undefined,
  savePosition: () => undefined
}

export const UnitContext = createContext<UnitState>(defaultState)

export const UnitContextProvider = React.memo(function UnitContextProvider({
  children
}: {
  children: JSX.Element
}) {
  const [unitInformation, setUnitInformation] = useState<Result<UnitResponse>>(
    Loading.of()
  )
  const [filters, setFilters] = useState(defaultState.filters)
  const [unitData, setUnitData] = useState<Result<UnitData>>(Loading.of())
  const [position, setPosition] = useState<number>(-1)
  const savePosition = useCallback(() => void setPosition(window.scrollY), [
    setPosition
  ])
  const scrollToPosition = useCallback(() => {
    if (position > -1) {
      window.scrollTo(0, position)
      setPosition(-1)
    }
  }, [position, setPosition])

  const value = useMemo(
    () => ({
      unitInformation,
      setUnitInformation,
      filters,
      setFilters,
      unitData,
      setUnitData,
      scrollToPosition,
      savePosition
    }),
    [
      unitInformation,
      setUnitInformation,
      filters,
      setFilters,
      unitData,
      setUnitData,
      scrollToPosition,
      savePosition
    ]
  )

  return <UnitContext.Provider value={value}>{children}</UnitContext.Provider>
})
