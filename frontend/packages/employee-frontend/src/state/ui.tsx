// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext } from 'react'

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
}

const defaultState = {
  uiMode: '',
  toggleUiMode: () => undefined,
  clearUiMode: () => undefined,
  setErrorMessage: () => undefined,
  clearErrorMessage: () => undefined
}

export const UIContext = createContext<UiState>(defaultState)

export const UIContextProvider = React.memo(function UIContextProvider({
  children
}: {
  children: JSX.Element
}) {
  const [uiMode, setUiMode] = useState<string>('')
  const [errorMessage, setErrorMessage] = useState<ErrorMessage | null>(null)
  const clearErrorMessage = () => setErrorMessage(null)
  const toggleUiMode = (mode: string) => setUiMode(mode)
  const clearUiMode = () => setUiMode('')

  const value = useMemo(
    () => ({
      uiMode,
      toggleUiMode,
      clearUiMode,
      errorMessage,
      setErrorMessage,
      clearErrorMessage
    }),
    [uiMode, setUiMode, errorMessage, setErrorMessage]
  )

  return <UIContext.Provider value={value}>{children}</UIContext.Provider>
})
