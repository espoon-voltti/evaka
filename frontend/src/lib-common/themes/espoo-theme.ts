// SPDX-FileCopyrightText: 2017-2021 City of Espoo
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
      primaryActive: blueColors.dark
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
      orange: '#ff7300',
      orangeDark: '#b85300',
      green: '#c6db00',
      greenDark: '#6e7a00',
      water: '#9fc1d3',
      yellow: '#ffce00',
      red: '#db0c41',
      petrol: '#1f6390',
      emerald: '#038572',
      violet: '#9d55c3'
    }
  },
  typography: {
    h1: {
      weight: 200,
      bold: 600,
      mobile: {
        weight: 600
      }
    },
    h2: {
      weight: 300,
      bold: 600,
      mobile: {
        weight: 600
      }
    },
    h3: {
      weight: 'normal',
      bold: 600
    },
    h4: {
      weight: 'normal',
      bold: 600
    },
    h5: {
      weight: 'normal',
      bold: 600
    }
  }
}

export default theme
