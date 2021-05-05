// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { LocalizationContextProvider } from './state'

export { useLang, useTranslation } from './state'
export { langs } from 'lib-customizations/citizen'
export type { Lang } from 'lib-customizations/citizen'

export const Localization = LocalizationContextProvider
