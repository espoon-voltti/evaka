// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Theme } from 'lib-common/theme'

const blueColors = {
  dark: '#00358a',
  primary: '#0047b6',
  light: '#4d7fcc',
  lighter: '#d9e4f4'
}

const theme: Theme = {
  colors: {
    main: {
      ...blueColors,
      primaryHover: blueColors.dark,
      primaryActive: blueColors.dark,
      primaryFocus: blueColors.light
    },
    greyscale: {
      darkest: '#091c3b',
      dark: '#536076',
      medium: '#a9b0bb',
      lighter: '#dadde2',
      lightest: '#f7f7f7',
      white: '#ffffff'
    },
    accents: {
      dangerRed: '#ff4f57',
      warningOrange: '#ff8e31',
      successGreen: '#70c673',
      infoBlue: blueColors.light,
      greenDark: '#014b30',
      orangeDark: '#ad581a',
      emerald: '#148190',
      violet: '#8f41b9',
      peach: '#ffc386',
      turquoise: '#7ff6fc',
      mint: '#bcfdce',
      lightBlue: '#c9d4dd',
      pink: '#fca5c7'
    }
  },
  typography: {
    h1: {
      weight: 500,
      bold: 600,
      mobile: {
        weight: 600
      }
    },
    h2: {
      weight: 500,
      bold: 600,
      mobile: {
        weight: 600
      }
    },
    h3: {
      weight: 400,
      bold: 600,
      mobile: {
        weight: 500
      }
    },
    h4: {
      weight: 400,
      bold: 600,
      mobile: {
        weight: 500
      }
    }
  }
}

export default theme
