// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, createContext, useContext, useEffect } from 'react'

import useLocalStorage from 'lib-common/utils/useLocalStorage'
import type { Translations as ComponentTranslations } from 'lib-components/i18n'
import { ComponentLocalizationContextProvider } from 'lib-components/i18n'
import type { Lang, Translations } from 'lib-customizations/employeeMobile'
import { langs, translations } from 'lib-customizations/employeeMobile'

interface I18nState {
  lang: Lang
  setLang: (l: Lang) => void
}

const defaultState = {
  lang: 'fi' as const,
  setLang: () => undefined
}

export const I18nContext = createContext<I18nState>(defaultState)

const validateLang = (value: string | null): value is Lang =>
  langs.some((l) => l === value)

export const I18nContextProvider = React.memo(function I18nContextProvider({
  children
}: {
  children: React.JSX.Element
}) {
  const [lang, setLang] = useLocalStorage(
    'evaka-employee-mobile.lang',
    defaultState.lang,
    validateLang
  )

  useEffect(() => {
    document.documentElement.lang = lang
  }, [lang])

  const value = useMemo(() => ({ lang, setLang }), [lang, setLang])

  return (
    <I18nContext.Provider value={value}>
      <ComponentLocalizationContextProvider
        useTranslations={useComponentTranslations}
      >
        {children}
      </ComponentLocalizationContextProvider>
    </I18nContext.Provider>
  )
})

export const useTranslation = (): { i18n: Translations; lang: Lang } => {
  const { lang } = useContext(I18nContext)
  return { i18n: translations[lang], lang }
}

function useComponentTranslations(): ComponentTranslations {
  const { i18n } = useTranslation()
  return i18n.components
}

export type { Lang, Translations }
