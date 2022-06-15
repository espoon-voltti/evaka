// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useCallback, useMemo, useState } from 'react'

import { Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'

import {
  getUnreadPedagogicalDocumentsCount,
  getUnreadVasuDocumentsCount
} from './api'

export interface ChildDocumentsState {
  unreadPedagogicalDocumentsCount: number | undefined
  unreadVasuDocumentsCount: number | undefined
  refreshUnreadPedagogicalDocumentsCount: () => void
  refreshUnreadVasuDocumentsCount: () => void
}

const defaultState: ChildDocumentsState = {
  unreadPedagogicalDocumentsCount: undefined,
  unreadVasuDocumentsCount: undefined,
  refreshUnreadPedagogicalDocumentsCount: () => undefined,
  refreshUnreadVasuDocumentsCount: () => undefined
}

export const ChildDocumentsContext =
  createContext<ChildDocumentsState>(defaultState)

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

    const [unreadVasuDocumentsCount, setUnreadVasuDocumentsCount] =
      useState<number>()

    const setUnreadPedagogicalDocumentCountResult = useCallback(
      (res: Result<number>) => {
        if (res.isSuccess) {
          setUnreadPedagogicalDocumentsCount(res.value)
        }
      },
      []
    )

    const refreshUnreadPedagogicalDocumentsCount = useRestApi(
      getUnreadPedagogicalDocumentsCount,
      setUnreadPedagogicalDocumentCountResult
    )

    const setUnreadVasuDocumentCountResult = useCallback(
      (res: Result<number>) => {
        if (res.isSuccess) {
          setUnreadVasuDocumentsCount(res.value)
        }
      },
      []
    )

    const refreshUnreadVasuDocumentsCount = useRestApi(
      getUnreadVasuDocumentsCount,
      setUnreadVasuDocumentCountResult
    )

    const value = useMemo(
      () => ({
        unreadPedagogicalDocumentsCount,
        unreadVasuDocumentsCount,
        refreshUnreadPedagogicalDocumentsCount,
        refreshUnreadVasuDocumentsCount
      }),
      [
        unreadPedagogicalDocumentsCount,
        refreshUnreadPedagogicalDocumentsCount,
        unreadVasuDocumentsCount,
        refreshUnreadVasuDocumentsCount
      ]
    )

    return (
      <ChildDocumentsContext.Provider value={value}>
        {children}
      </ChildDocumentsContext.Provider>
    )
  }
)
