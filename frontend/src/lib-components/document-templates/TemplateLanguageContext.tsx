// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useContext } from 'react'

import type { UiLanguage } from 'lib-common/generated/api-types/shared'

export type TemplateLanguage = 'fi' | 'sv' | 'en'

export const localeByTemplateLanguage: Record<UiLanguage, TemplateLanguage> = {
  FI: 'fi',
  SV: 'sv',
  EN: 'en'
}

const TemplateLanguageContext = createContext<TemplateLanguage>('fi')

export const TemplateLanguageProvider = React.memo(
  function TemplateLanguageProvider({
    value,
    children
  }: {
    value: TemplateLanguage
    children: React.ReactNode
  }) {
    return (
      <TemplateLanguageContext.Provider value={value}>
        {children}
      </TemplateLanguageContext.Provider>
    )
  }
)

export const useTemplateLanguage = (): TemplateLanguage =>
  useContext(TemplateLanguageContext)
