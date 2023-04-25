// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconProp } from '@fortawesome/fontawesome-svg-core'
import React, { useMemo, useState, createContext, useCallback } from 'react'

import type { ModalType } from 'lib-components/molecules/modals/BaseModal'

export interface InfoMessage {
  title: string
  text?: string
  'data-qa'?: string
  icon: IconProp
  type: ModalType
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
  modalOpen: boolean
  setModalOpen: (v: boolean) => void
}

const defaultState = {
  errorMessage: null,
  setErrorMessage: () => undefined,
  clearErrorMessage: () => undefined,
  infoMessage: null,
  setInfoMessage: () => undefined,
  clearInfoMessage: () => undefined,
  modalOpen: false,
  setModalOpen: () => undefined
}

export const OverlayContext = createContext<OverlayState>(defaultState)

export const OverlayContextProvider = React.memo(
  function OverlayContextProvider({ children }: { children: React.ReactNode }) {
    const [errorMessage, setErrorMessage] = useState<ErrorMessage | null>(null)
    const clearErrorMessage = useCallback(() => setErrorMessage(null), [])

    const [infoMessage, setInfoMessage] = useState<InfoMessage | null>(null)
    const clearInfoMessage = useCallback(() => setInfoMessage(null), [])

    const [modalOpen, setModalOpen] = useState<boolean>(false)

    const value = useMemo(
      () => ({
        errorMessage,
        setErrorMessage,
        clearErrorMessage,
        infoMessage,
        setInfoMessage,
        clearInfoMessage,
        modalOpen: errorMessage !== null || infoMessage !== null || modalOpen,
        setModalOpen
      }),
      [
        errorMessage,
        clearErrorMessage,
        infoMessage,
        clearInfoMessage,
        modalOpen,
        setModalOpen
      ]
    )

    return (
      <OverlayContext.Provider value={value}>
        {children}
      </OverlayContext.Provider>
    )
  }
)
