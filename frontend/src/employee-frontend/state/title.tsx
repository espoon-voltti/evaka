// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, createContext } from 'react'

import { useTranslation } from './i18n'

export interface TitleState {
  setTitle: (title: string | undefined, hideDefault?: boolean) => void
  formatTitleName: (firstName: string | null, lastName: string | null) => string
}

const defaultState = {
  setTitle: () => undefined,
  formatTitleName: () => ''
}

export const TitleContext = createContext<TitleState>(defaultState)

export const TitleContextProvider = React.memo(function TitleContextProvider({
  children
}: {
  children: React.JSX.Element
}) {
  const { i18n } = useTranslation()

  const setTitle = useCallback(
    (title?: string, hideDefault?: boolean) => {
      document.title = title
        ? `${title}${hideDefault ? '' : ` - ${i18n.titles.defaultTitle}`}`
        : i18n.titles.defaultTitle
    },
    [i18n]
  )

  const formatTitleName = useCallback(
    (maybeFirstName: string | null, maybeLastName: string | null): string => {
      const firstName = maybeFirstName || i18n.common.noFirstName
      const lastName = maybeLastName || i18n.common.noLastName
      return firstName && lastName
        ? `${lastName} ${firstName}`
        : lastName
          ? lastName
          : firstName
    },
    [i18n]
  )

  const value = useMemo(
    () => ({ setTitle, formatTitleName }),
    [setTitle, formatTitleName]
  )

  return <TitleContext.Provider value={value}>{children}</TitleContext.Provider>
})
