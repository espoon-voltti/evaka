// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useEffect,
  useRef
} from 'react'

import type {
  PersonId,
  UiLanguage
} from 'lib-common/generated/api-types/shared'
import { useMutation } from 'lib-common/query'
import useLocalStorage from 'lib-common/utils/useLocalStorage'
import type { Translations as ComponentTranslations } from 'lib-components/i18n'
import { ComponentLocalizationContextProvider } from 'lib-components/i18n'
import type { Lang } from 'lib-customizations/citizen'
import {
  langs,
  translations as localizations
} from 'lib-customizations/citizen'

import { useUser } from '../auth/state'

import { updatePreferredUiLanguageMutation } from './queries'

const getDefaultLanguage: () => Lang = () => {
  const params = new URLSearchParams(window.location.search)
  const lang = params.get('lang')
  if (lang && langs.includes(lang as Lang)) {
    return lang as Lang
  } else {
    const language = window.navigator.language.split('-')[0]
    if ((language === 'fi' || language === 'sv') && langs.includes(language)) {
      return language
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

export const LocalizationContext =
  createContext<LocalizationState>(defaultState)

const validateLang = (value: string | null): value is Lang => {
  for (const lang of langs) {
    if (lang === value) return true
  }
  return false
}

export const langByUiLanguage: Record<UiLanguage, Lang> = {
  FI: 'fi',
  SV: 'sv',
  EN: 'en'
}

export const uiLanguageByLang: Record<Lang, UiLanguage> = {
  fi: 'FI',
  sv: 'SV',
  en: 'EN'
}

export const LocalizationContextProvider = React.memo(
  function LocalizationContextProvider({
    children
  }: {
    children: React.ReactNode
  }) {
    const [lang, setLangInBrowser] = useLocalStorage(
      'evaka-citizen.lang',
      defaultState.lang,
      validateLang
    )
    const user = useUser()
    const { mutate: saveLang } = useMutation(updatePreferredUiLanguageMutation)
    const langSyncedForUser = useRef<PersonId | undefined>(undefined)

    useEffect(() => {
      document.documentElement.lang = lang
    }, [lang])

    useEffect(() => {
      if (user === undefined || langSyncedForUser.current === user.id) {
        return
      }
      langSyncedForUser.current = user.id

      if (user.preferredUiLanguage === null) {
        saveLang({ body: { preferredUiLanguage: uiLanguageByLang[lang] } })
        return
      }
      const savedLang = langByUiLanguage[user.preferredUiLanguage]
      if (langs.includes(savedLang)) setLangInBrowser(savedLang)
    }, [lang, saveLang, setLangInBrowser, user])

    const setLang = useCallback(
      (lang: Lang) => {
        setLangInBrowser(lang)
        if (user !== undefined) {
          saveLang({ body: { preferredUiLanguage: uiLanguageByLang[lang] } })
        }
      },
      [saveLang, setLangInBrowser, user]
    )

    const value = useMemo(
      () => ({
        lang,
        setLang
      }),
      [lang, setLang]
    )

    return (
      <LocalizationContext.Provider value={value}>
        <ComponentLocalizationContextProvider
          useTranslations={useComponentTranslations}
        >
          {children}
        </ComponentLocalizationContextProvider>
      </LocalizationContext.Provider>
    )
  }
)

export const useTranslation = () => {
  const { lang } = useContext(LocalizationContext)

  return localizations[lang]
}

function useComponentTranslations(): ComponentTranslations {
  const translations = useTranslation()
  return translations.components
}

export const useLang = () => {
  const context = useContext(LocalizationContext)

  const value: [Lang, (lang: Lang) => void] = useMemo(
    () => [context.lang, context.setLang],
    [context.lang, context.setLang]
  )

  return value
}
