// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext, useEffect } from 'react'

import { Loading, Result } from 'lib-common/api'
import { UnitInfo } from 'lib-common/generated/api-types/attendance'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { getMobileUnitInfo } from '../api/unit'
import { useParams } from 'react-router-dom'
import { UUID } from 'lib-common/types'

interface UnitState {
  unitInfoResponse: Result<UnitInfo>
  showPresent: boolean
  setShowPresent: (value: boolean) => void
}

const defaultState: UnitState = {
  unitInfoResponse: Loading.of(),
  showPresent: false,
  setShowPresent: () => undefined
}

export const UnitContext = createContext<UnitState>(defaultState)

export const UnitContextProvider = React.memo(function UnitContextProvider({
  children
}: {
  children: JSX.Element
}) {
  const { unitId } = useParams<{ unitId: UUID }>()

  const [unitInfoResponse, setUnitInfoResponse] = useState<Result<UnitInfo>>(
    defaultState.unitInfoResponse
  )

  const loadUnitInfo = useRestApi(getMobileUnitInfo, setUnitInfoResponse)
  useEffect(() => loadUnitInfo(unitId), [loadUnitInfo, unitId])

  const [showPresent, setShowPresent] = useState<boolean>(
    defaultState.showPresent
  )

  const value = useMemo(
    () => ({
      unitInfoResponse,
      showPresent,
      setShowPresent
    }),
    [unitInfoResponse, showPresent, setShowPresent]
  )

  return <UnitContext.Provider value={value}>{children}</UnitContext.Provider>
})
