// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext } from 'react'

export type ErrorMessageType = 'warning' | 'error'

export interface ErrorMessage {
  type: ErrorMessageType
  title: string
  text?: string
  resolveLabel?: string
}

export interface OverlayState {
  errorMessage: ErrorMessage | null
  setErrorMessage: (message: ErrorMessage | null) => void
  clearErrorMessage: () => void
}

const defaultState = {
  errorMessage: null,
  setErrorMessage: () => undefined,
  clearErrorMessage: () => undefined
}

export const OverlayContext = createContext<OverlayState>(defaultState)

export const OverlayContextProvider = React.memo(
  function OverlayContextProvider({ children }: { children: React.ReactNode }) {
    const [errorMessage, setErrorMessage] = useState<ErrorMessage | null>(null)
    const clearErrorMessage = () => setErrorMessage(null)

    const value = useMemo(
      () => ({
        errorMessage,
        setErrorMessage,
        clearErrorMessage
      }),
      [errorMessage, setErrorMessage]
    )

    return (
      <OverlayContext.Provider value={value}>
        {children}
      </OverlayContext.Provider>
    )
  }
)
