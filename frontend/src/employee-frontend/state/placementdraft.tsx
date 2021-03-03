// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo, useState } from 'react'
import { Loading, Result } from '@evaka/lib-common/api'
import { PlacementDraft } from '../types/placementdraft'

export interface Unit {
  id: string
  name: string
  ghostUnit?: boolean
}

export interface PlacementDraftState {
  placementDraft: Result<PlacementDraft>
  setPlacementDraft: (request: Result<PlacementDraft>) => void
}

const defaultPlacementDraftState: PlacementDraftState = {
  placementDraft: Loading.of(),
  setPlacementDraft: () => undefined
}

export const PlacementDraftContext = createContext<PlacementDraftState>(
  defaultPlacementDraftState
)

export const PlacementDraftContextProvider = React.memo(
  function PlacementDraftContextProvider({
    children
  }: {
    children: JSX.Element
  }) {
    const [placementDraft, setPlacementDraft] = useState<
      Result<PlacementDraft>
    >(defaultPlacementDraftState.placementDraft)

    const value = useMemo(
      () => ({
        placementDraft,
        setPlacementDraft
      }),
      [placementDraft, setPlacementDraft]
    )

    return (
      <PlacementDraftContext.Provider value={value}>
        {children}
      </PlacementDraftContext.Provider>
    )
  }
)

export interface UnitsState {
  units: Result<Unit[]>
  setUnits: (request: Result<Unit[]>) => void
}

const defaultUnitsState: UnitsState = {
  units: Loading.of(),
  setUnits: () => undefined
}

export const UnitsContext = createContext<UnitsState>(defaultUnitsState)

export const UnitsContextProvider = React.memo(function UnitsContextProvider({
  children
}: {
  children: JSX.Element
}) {
  const [units, setUnits] = useState<Result<Unit[]>>(defaultUnitsState.units)

  const value = useMemo(
    () => ({
      units,
      setUnits
    }),
    [units, setUnits]
  )

  return <UnitsContext.Provider value={value}>{children}</UnitsContext.Provider>
})
