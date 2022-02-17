// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useCallback, useMemo, useState } from 'react'

import { Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'

import { getUnreadPedagogicalDocumentsCount } from './api'

export interface PedagogicalDocumentsState {
  unreadPedagogicalDocumentsCount: number | undefined
  refreshUnreadPedagogicalDocumentsCount: () => void
}

const defaultState: PedagogicalDocumentsState = {
  unreadPedagogicalDocumentsCount: undefined,
  refreshUnreadPedagogicalDocumentsCount: () => undefined
}

export const PedagogicalDocumentsContext =
  createContext<PedagogicalDocumentsState>(defaultState)

export const PedagogicalDocumentsContextProvider = React.memo(
  function PedagogicalDocumentsContextProvider({
    children
  }: {
    children: React.ReactNode
  }) {
    const [
      unreadPedagogicalDocumentsCount,
      setUnreadPedagogicalDocumentsCount
    ] = useState<number>()

    const setUnreadResult = useCallback((res: Result<number>) => {
      if (res.isSuccess) {
        setUnreadPedagogicalDocumentsCount(res.value)
      }
    }, [])

    const refreshUnreadPedagogicalDocumentsCount = useRestApi(
      getUnreadPedagogicalDocumentsCount,
      setUnreadResult
    )

    const value = useMemo(
      () => ({
        unreadPedagogicalDocumentsCount,
        refreshUnreadPedagogicalDocumentsCount
      }),
      [unreadPedagogicalDocumentsCount, refreshUnreadPedagogicalDocumentsCount]
    )

    return (
      <PedagogicalDocumentsContext.Provider value={value}>
        {children}
      </PedagogicalDocumentsContext.Provider>
    )
  }
)
