// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AbsenceType } from '~types/absence'

export const EspooBrandColors = {
  espooBlue: '#0050bb',
  espooTurquoise: '#249fff'
}

export const BlueColors = {
  dark: '#013c8c',
  medium: '#0050bb',
  primary: '#3273c9',
  light: '#99b9e4'
}

export const Greyscale = {
  darkest: '#0f0f0f',
  dark: '#6e6e6e',
  medium: '#b1b1b1',

  lighter: '#d8d8d8',
  lightest: '#f5f5f5',
  white: '#ffffff'
}

export const AccentColors = {
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

const Colors = {
  brandEspoo: EspooBrandColors,
  blues: BlueColors,
  primary: BlueColors.primary,
  primaryHover: BlueColors.medium,
  primaryActive: BlueColors.dark,
  greyscale: Greyscale,
  accents: AccentColors
}

export const absenceBackgroundColours: { [key in AbsenceType]: string } = {
  UNKNOWN_ABSENCE: Colors.accents.green,
  OTHER_ABSENCE: Colors.blues.dark,
  SICKLEAVE: Colors.accents.violet,
  PLANNED_ABSENCE: Colors.blues.light,
  PARENTLEAVE: Colors.blues.primary,
  FORCE_MAJEURE: Colors.accents.red,
  TEMPORARY_RELOCATION: Colors.accents.orange,
  TEMPORARY_VISITOR: Colors.accents.yellow,
  PRESENCE: Colors.greyscale.white
}

export const absenceBorderColours: { [key in AbsenceType]: string } = {
  UNKNOWN_ABSENCE: Colors.accents.green,
  OTHER_ABSENCE: Colors.blues.dark,
  SICKLEAVE: Colors.accents.violet,
  PLANNED_ABSENCE: Colors.blues.light,
  PARENTLEAVE: Colors.blues.primary,
  FORCE_MAJEURE: Colors.accents.red,
  TEMPORARY_RELOCATION: Colors.accents.orange,
  TEMPORARY_VISITOR: Colors.accents.yellow,
  PRESENCE: Colors.greyscale.white
}

export const absenceColours: { [key in AbsenceType]: string } = {
  UNKNOWN_ABSENCE: Colors.greyscale.darkest,
  OTHER_ABSENCE: Colors.greyscale.white,
  SICKLEAVE: Colors.greyscale.white,
  PLANNED_ABSENCE: Colors.greyscale.darkest,
  PARENTLEAVE: Colors.greyscale.white,
  FORCE_MAJEURE: Colors.greyscale.white,
  TEMPORARY_RELOCATION: Colors.greyscale.white,
  TEMPORARY_VISITOR: Colors.greyscale.white,
  PRESENCE: Colors.greyscale.white
}

export default Colors
