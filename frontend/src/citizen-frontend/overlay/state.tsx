// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext } from 'react'
import { IconProp } from '@fortawesome/fontawesome-svg-core'
import { IconColour } from '@evaka/lib-components/src/molecules/modals/FormModal'

export interface InfoMessage {
  title: string
  text?: string
  'data-qa'?: string
  icon: IconProp
  iconColour: IconColour
  resolve: {
    action: () => void
    label: string
  }
  reject?: {
    action: () => void
    label: string
  }
}

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

  infoMessage: InfoMessage | null
  setInfoMessage: (message: InfoMessage | null) => void
  clearInfoMessage: () => void
}

const defaultState = {
  errorMessage: null,
  setErrorMessage: () => undefined,
  clearErrorMessage: () => undefined,

  infoMessage: null,
  setInfoMessage: () => undefined,
  clearInfoMessage: () => undefined
}

export const OverlayContext = createContext<OverlayState>(defaultState)

export const OverlayContextProvider = React.memo(
  function OverlayContextProvider({ children }: { children: React.ReactNode }) {
    const [errorMessage, setErrorMessage] = useState<ErrorMessage | null>(null)
    const clearErrorMessage = () => setErrorMessage(null)

    const [infoMessage, setInfoMessage] = useState<InfoMessage | null>(null)
    const clearInfoMessage = () => setInfoMessage(null)

    const value = useMemo(
      () => ({
        errorMessage,
        setErrorMessage,
        clearErrorMessage,
        infoMessage,
        setInfoMessage,
        clearInfoMessage
      }),
      [errorMessage, setErrorMessage, infoMessage, setInfoMessage]
    )

    return (
      <OverlayContext.Provider value={value}>
        {children}
      </OverlayContext.Provider>
    )
  }
)
