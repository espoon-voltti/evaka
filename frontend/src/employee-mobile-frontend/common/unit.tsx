// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useEffect, useMemo } from 'react'

import { Loading, Result } from 'lib-common/api'
import { UnitInfo } from 'lib-common/generated/api-types/attendance'
import { UnreadCountByAccountAndGroup } from 'lib-common/generated/api-types/messaging'
import { useQueryResult } from 'lib-common/query'
import { idleTracker } from 'lib-common/utils/idleTracker'
import { useApiState } from 'lib-common/utils/useRestApi'

import { client } from '../client'
import { unreadCountsQuery } from '../messages/queries'
import { getMobileUnitInfo } from '../pairing/api'

interface UnitState {
  unitInfoResponse: Result<UnitInfo>
  reloadUnitInfo: () => void
  unreadCounts: UnreadCountByAccountAndGroup[]
}

const defaultState: UnitState = {
  unitInfoResponse: Loading.of(),
  reloadUnitInfo: () => undefined,
  unreadCounts: []
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

  const unreadCountsResponse = useQueryResult(unreadCountsQuery(unitId))

  const unreadCounts = useMemo(
    () => unreadCountsResponse.getOrElse([]),
    [unreadCountsResponse]
  )

  useEffect(
    () => idleTracker(client, reloadUnitInfo, { thresholdInMinutes: 5 }),
    [reloadUnitInfo]
  )

  const value = useMemo(
    () => ({
      unitInfoResponse,
      reloadUnitInfo,
      unreadCounts
    }),
    [unitInfoResponse, reloadUnitInfo, unreadCounts]
  )

  return <UnitContext.Provider value={value}>{children}</UnitContext.Provider>
})
