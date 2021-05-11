// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export interface Theme {
  colors: {
    espooBrandColors: {
      espooBlue: string
      espooTurquoise: string
      espooTurquoiseLight: string
    }
    blueColors: {
      dark: string
      medium: string
      primary: string
      primaryHover: string
      primaryActive: string
      light: string
      lighter: string
    }
    greyscale: {
      darkest: string
      dark: string
      medium: string
      lighter: string
      lightest: string
      white: string
    }
    accentColors: {
      orange: string
      orangeDark: string
      green: string
      greenDark: string
      water: string
      yellow: string
      red: string
      petrol: string
      emerald: string
      violet: string
    }
  }
}
