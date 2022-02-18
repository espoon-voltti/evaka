// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { LocalizationContextProvider } from './state'

export { useLang, useTranslation } from './state'
export { langs } from 'lib-customizations/citizen'
export type { Lang, Translations } from 'lib-customizations/citizen'

export const Localization = LocalizationContextProvider
