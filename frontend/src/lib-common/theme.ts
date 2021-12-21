// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export interface Theme {
  colors: {
    main: {
      dark: string // main 1
      primary: string // main 2
      primaryHover: string
      primaryActive: string
      light: string // main 3
      lighter: string // main 4
    }
    greyscale: {
      darkest: string // 100
      dark: string // 70
      medium: string // 35
      lighter: string // 15
      lightest: string // 4
      white: string // 0
    }
    accents: {
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
  typography: {
    h1: FontSettings & HasBoldOption
    h2: FontSettings & HasBoldOption
    h3: FontSettings & HasBoldOption
    h4: FontSettings & HasBoldOption
    h5: FontSettings & HasBoldOption
  }
}

type FontSettings = {
  weight: FontWeight
  mobile?: Omit<FontSettings, 'mobile'>
}
type HasBoldOption = { bold: FontWeight }

type FontWeight =
  | 'normal'
  | 'bold'
  | 'lighter'
  | 'bolder'
  | number
  | 'inherit'
  | 'initial'
  | 'unset'
