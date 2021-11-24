// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Loading, Result } from 'lib-common/api'
import { UnitInfo } from 'lib-common/generated/api-types/attendance'
import { idleTracker } from 'lib-common/utils/idleTracker'
import { useApiState } from 'lib-common/utils/useRestApi'
import React, { createContext, useEffect, useMemo } from 'react'
import { client } from '../api/client'
import { getMobileUnitInfo } from '../api/unit'

interface UnitState {
  unitInfoResponse: Result<UnitInfo>
  reloadUnitInfo: () => void
}

const defaultState: UnitState = {
  unitInfoResponse: Loading.of(),
  reloadUnitInfo: () => undefined
}

export const UnitContext = createContext<UnitState>(defaultState)

export const UnitContextProvider = React.memo(function UnitContextProvider({
  unitId,
  children
}: {
  unitId: string
  children: JSX.Element
}) {
  const [unitInfoResponse, reloadUnitInfo] = useApiState(
    () => getMobileUnitInfo(unitId),
    [unitId]
  )

  useEffect(
    () => idleTracker(client, reloadUnitInfo, { thresholdInMinutes: 5 }),
    [reloadUnitInfo]
  )

  const value = useMemo(
    () => ({
      unitInfoResponse,
      reloadUnitInfo
    }),
    [unitInfoResponse, reloadUnitInfo]
  )

  return <UnitContext.Provider value={value}>{children}</UnitContext.Provider>
})
