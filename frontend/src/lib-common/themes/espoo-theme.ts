// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Theme } from 'lib-common/theme'

const blueColors = {
  m1: '#00358a',
  m2: '#0047b6',
  m3: '#4d7fcc',
  m4: '#d9e4f4'
}

const theme: Theme = {
  colors: {
    main: {
      ...blueColors,
      m2Hover: blueColors.m1,
      m2Active: blueColors.m1,
      m2Focus: blueColors.m3
    },
    grayscale: {
      g100: '#091c3b',
      g70: '#536076',
      g35: '#a9b0bb',
      g15: '#dadde2',
      g4: '#f7f7f7',
      g0: '#ffffff'
    },
    status: {
      danger: '#ff4f57',
      warning: '#ff8e31',
      success: '#70c673',
      info: blueColors.m2
    },
    accents: {
      a1greenDark: '#014b30',
      a2orangeDark: '#ad581a',
      a3emerald: '#148190',
      a4violet: '#8f41b9',
      a5orangeLight: '#ffc386',
      a6turquoise: '#7ff6fc',
      a7mint: '#bcfdce',
      a8lightBlue: '#c9d4dd',
      a9pink: '#fca5c7',
      a10powder: '#fde6db'
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
      weight: 500,
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
