// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useContext, useMemo, useState } from 'react'
import fi from './fi'
import sv from './sv'
import en from './en'

export const langs = ['fi', 'sv', 'en'] as const

export type Lang = typeof langs[number]

const getDefaultLanguage: () => Lang = () => {
  const params = new URLSearchParams(window.location.search)
  const lang = params.get('lang')
  if (lang && ['fi', 'sv', 'en'].includes(lang)) {
    return lang as Lang
  } else {
    const language = (
      (window.navigator['userLanguage'] as string) || window.navigator.language
    ).split('-')[0]
    if (language === 'fi' || language === 'sv') {
      return language as Lang
    } else {
      return 'fi' as const
    }
  }
}

type LocalizationState = {
  lang: Lang
  setLang: (lang: Lang) => void
}

const defaultState = {
  lang: getDefaultLanguage(),
  setLang: () => undefined
}

const LocalizationContext = createContext<LocalizationState>(defaultState)

export const LocalizationContextProvider = React.memo(
  function LocalizationContextProvider({ children }) {
    const [lang, setLang] = useState<Lang>(defaultState.lang)

    const value = useMemo(
      () => ({
        lang,
        setLang
      }),
      [lang, setLang]
    )

    return (
      <LocalizationContext.Provider value={value}>
        {children}
      </LocalizationContext.Provider>
    )
  }
)

const localizations = {
  fi,
  sv,
  en
}

export const useTranslation = () => {
  const { lang } = useContext(LocalizationContext)

  return localizations[lang]
}

export const useLang = () => {
  const context = useContext(LocalizationContext)

  const value: [Lang, (lang: Lang) => void] = useMemo(
    () => [context.lang, context.setLang],
    [context.lang, context.setLang]
  )

  return value
}
