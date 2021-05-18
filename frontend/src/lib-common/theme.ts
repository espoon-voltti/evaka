// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export interface Theme {
  colors: {
    brand: {
      primary: string
      secondary: string
      secondaryLight: string
    }
    main: {
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
