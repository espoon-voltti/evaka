// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext, useCallback } from 'react'

import { DaycareId } from 'lib-common/generated/api-types/shared'
import { UUID } from 'lib-common/types'

export type ErrorMessageType = 'warning' | 'error'

export interface ErrorMessage {
  type: ErrorMessageType
  title: string
  text?: string
  resolveLabel: string
  rejectLabel?: string
}

export interface UiState {
  uiMode: string
  toggleUiMode: (mode: string) => void
  clearUiMode: () => void
  errorMessage?: ErrorMessage | null
  setErrorMessage: (message: ErrorMessage | null) => void
  clearErrorMessage: () => void
  pairingState?: PairingState
  startPairing: (
    id: PairingState['id'],
    onClose?: PairingState['onClose']
  ) => void
  closePairingModal: () => void
}

const defaultState = {
  uiMode: '',
  toggleUiMode: () => undefined,
  clearUiMode: () => undefined,
  setErrorMessage: () => undefined,
  clearErrorMessage: () => undefined,
  startPairing: () => undefined,
  closePairingModal: () => undefined
}

export const UIContext = createContext<UiState>(defaultState)

export const UIContextProvider = React.memo(function UIContextProvider({
  children
}: {
  children: React.JSX.Element
}) {
  const [uiMode, setUiMode] = useState<string>('')
  const [errorMessage, setErrorMessage] = useState<ErrorMessage | null>(null)
  const clearErrorMessage = useCallback(() => setErrorMessage(null), [])
  const toggleUiMode = useCallback((mode: string) => setUiMode(mode), [])
  const clearUiMode = useCallback(() => setUiMode(''), [])

  const [pairingState, setPairingState] = useState<PairingState>()
  const startPairing = useCallback(
    (id: PairingState['id'], onClose?: PairingState['onClose']) => {
      setUiMode('pair-mobile-device')
      setPairingState({ id, onClose })
    },
    []
  )
  const closePairingModal = useCallback(() => {
    setUiMode('')
    pairingState?.onClose?.()
  }, [pairingState])

  const value = useMemo(
    () => ({
      uiMode,
      toggleUiMode,
      clearUiMode,
      errorMessage,
      setErrorMessage,
      clearErrorMessage,
      pairingState,
      startPairing,
      closePairingModal
    }),
    [
      uiMode,
      toggleUiMode,
      clearUiMode,
      errorMessage,
      clearErrorMessage,
      pairingState,
      startPairing,
      closePairingModal
    ]
  )

  return <UIContext.Provider value={value}>{children}</UIContext.Provider>
})

interface PairingState {
  id: { unitId: DaycareId } | { employeeId: UUID }
  onClose?: () => void
}
