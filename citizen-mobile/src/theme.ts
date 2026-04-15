// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { MD3LightTheme } from 'react-native-paper'

export const evakaTheme = {
  ...MD3LightTheme,
  roundness: 2,
  colors: {
    ...MD3LightTheme.colors,
    primary: '#00358a',
    onPrimary: '#ffffff',
    secondary: '#249fff',
    error: '#b53434'
  }
}
