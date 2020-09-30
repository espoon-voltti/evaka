// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// master language, defines the keys of other translations
import { fi } from './fi'

export type Lang = 'fi'

export type Translations = typeof fi

export const translations: { [K in Lang]: Translations } = { fi }
