// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { LocalizationContextProvider } from './state'
import fi from './fi'

export { Lang, useLang, useTranslation } from './state'

export type Translations = typeof fi

export const Localization = LocalizationContextProvider
