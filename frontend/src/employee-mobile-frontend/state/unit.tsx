// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useEffect, useMemo, useState } from 'react'

import { Loading, Result } from 'lib-common/api'
import { UnitInfo } from 'lib-common/generated/api-types/attendance'
import { useApiState } from 'lib-common/utils/useRestApi'
import { getMobileUnitInfo } from '../api/unit'
import { useParams } from 'react-router-dom'
import { UUID } from 'lib-common/types'
import { idleTracker } from 'lib-common/utils/idleTracker'
import { client } from '../api/client'

interface UnitState {
  unitInfoResponse: Result<UnitInfo>
  showPresent: boolean
  setShowPresent: (value: boolean) => void
  reloadUnitInfo: () => void
}

const defaultState: UnitState = {
  unitInfoResponse: Loading.of(),
  showPresent: false,
  setShowPresent: () => undefined,
  reloadUnitInfo: () => undefined
}

export const UnitContext = createContext<UnitState>(defaultState)

export const UnitContextProvider = React.memo(function UnitContextProvider({
  children
}: {
  children: JSX.Element
}) {
  const { unitId } = useParams<{ unitId: UUID }>()

  const [unitInfoResponse, reloadUnitInfo] = useApiState(
    () => getMobileUnitInfo(unitId),
    [unitId]
  )

  const [showPresent, setShowPresent] = useState<boolean>(
    defaultState.showPresent
  )

  useEffect(
    () => idleTracker(client, reloadUnitInfo, { thresholdInMinutes: 5 }),
    [reloadUnitInfo]
  )

  const value = useMemo(
    () => ({
      unitInfoResponse,
      showPresent,
      setShowPresent,
      reloadUnitInfo
    }),
    [unitInfoResponse, showPresent, reloadUnitInfo]
  )

  return <UnitContext.Provider value={value}>{children}</UnitContext.Provider>
})
