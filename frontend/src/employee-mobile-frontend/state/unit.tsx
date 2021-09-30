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
}

const defaultState: UnitState = {
  unitInfoResponse: Loading.of()
}

export const UnitContext = createContext<UnitState>(defaultState)

export const UnitContextProvider = React.memo(function UnitContextProvider({
  children
}: {
  children: JSX.Element
}) {
  const { unitId } = useParams<{ unitId: UUID }>()

  const [unitInfoResponse, setUnitInfoResponse] = useState<Result<UnitInfo>>(
    Loading.of()
  )

  const loadUnitInfo = useRestApi(getMobileUnitInfo, setUnitInfoResponse)
  useEffect(() => loadUnitInfo(unitId), [loadUnitInfo, unitId])

  const value = useMemo(
    () => ({
      unitInfoResponse
    }),
    [unitInfoResponse]
  )

  return <UnitContext.Provider value={value}>{children}</UnitContext.Provider>
})
