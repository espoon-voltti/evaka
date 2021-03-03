// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext, useContext } from 'react'
import { translations, Lang, Translations } from '../assets/i18n'

interface I18nState {
  lang: Lang
  setLang: (l: Lang) => void
}

const defaultState = {
  lang: 'fi' as const,
  setLang: () => undefined
}

export const I18nContext = createContext<I18nState>(defaultState)

export const I18nContextProvider = React.memo(function I18nContextProvider({
  children
}: {
  children: JSX.Element
}) {
  const [lang, setLang] = useState<Lang>('fi')

  const value = useMemo(() => ({ lang, setLang }), [lang, setLang])

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
})

export const useTranslation = (): { i18n: Translations; lang: Lang } => {
  const { lang } = useContext(I18nContext)
  return { i18n: translations[lang], lang }
}

export { Lang, Translations }
