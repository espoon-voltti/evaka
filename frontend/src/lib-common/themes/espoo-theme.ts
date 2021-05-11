// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Theme } from 'lib-common/theme'

const espooBrandColors = {
  espooBlue: '#0050bb',
  espooTurquoise: '#249fff',
  espooTurquoiseLight: '#e9f5ff'
}

const blueColors = {
  dark: '#013c8c',
  medium: '#0050bb',
  primary: '#3273c9',
  light: '#99b9e4',
  lighter: '#dce5f2'
}

const greyscale = {
  darkest: '#0f0f0f',
  dark: '#6e6e6e',
  medium: '#b1b1b1',

  lighter: '#d8d8d8',
  lightest: '#f5f5f5',
  white: '#ffffff'
}

const accentColors = {
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

const theme: Theme = {
  colors: {
    espooBrandColors,
    blueColors: {
      ...blueColors,
      primaryHover: blueColors.medium,
      primaryActive: blueColors.dark
    },
    greyscale,
    accentColors
  }
}

export default theme
