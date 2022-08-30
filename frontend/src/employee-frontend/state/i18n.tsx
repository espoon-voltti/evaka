// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { I18nProvider } from '@react-aria/i18n'
import React, { useMemo, useState, createContext, useContext } from 'react'

import { translations } from 'lib-customizations/employee'
import type { Lang, Translations } from 'lib-customizations/employee'

interface I18nState {
  lang: Lang
  setLang: (l: Lang) => void
}

const defaultState = {
  lang: 'fi' as const,
  setLang: () => undefined
}

const navigatorLocale: string[] | undefined =
  window.navigator.language?.split('-')

const withNavigatorRegion = (language: string, fallback: string) =>
  `${language}-${
    navigatorLocale[0] === language ? navigatorLocale[1] || fallback : fallback
  }`

export const I18nContext = createContext<I18nState>(defaultState)

export const I18nContextProvider = React.memo(function I18nContextProvider({
  children
}: {
  children: JSX.Element
}) {
  const [lang, setLang] = useState<Lang>('fi')

  const value = useMemo(() => ({ lang, setLang }), [lang, setLang])

  return (
    <I18nContext.Provider value={value}>
      <I18nProvider
        locale={
          {
            fi: withNavigatorRegion('fi', 'FI'),
            sv: withNavigatorRegion('sv', 'FI'),
            en: withNavigatorRegion('en', 'GB')
          }[lang]
        }
      >
        {children}
      </I18nProvider>
    </I18nContext.Provider>
  )
})

export const useTranslation = (): { i18n: Translations; lang: Lang } => {
  const { lang } = useContext(I18nContext)
  return { i18n: translations[lang], lang }
}

export type { Lang, Translations }
