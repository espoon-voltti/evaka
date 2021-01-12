// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import customizations from 'Theme/src/index'

export const espooBrandColors = {
  espooBlue: '#0050bb',
  espooTurquoise: '#249fff'
}

export const blueColors = customizations.colors.primaryColors

export const greyscale = {
  darkest: '#0f0f0f',
  dark: '#6e6e6e',
  medium: '#b1b1b1',

  lighter: '#d8d8d8',
  lightest: '#f5f5f5',
  white: '#ffffff'
}

export const accentColors = {
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

const colors = {
  brandEspoo: espooBrandColors,
  blues: blueColors,
  primary: blueColors.primary,
  primaryHover: blueColors.medium,
  primaryActive: blueColors.dark,
  greyscale: greyscale,
  accents: accentColors
}

export const absenceColours = {
  UNKNOWN_ABSENCE: colors.greyscale.darkest,
  OTHER_ABSENCE: colors.greyscale.white,
  SICKLEAVE: colors.greyscale.white,
  PLANNED_ABSENCE: colors.greyscale.darkest,
  PARENTLEAVE: colors.greyscale.white,
  FORCE_MAJEURE: colors.greyscale.white,
  TEMPORARY_RELOCATION: colors.greyscale.white,
  TEMPORARY_VISITOR: colors.greyscale.white,
  PRESENCE: colors.greyscale.white
}

type AbsenceType = keyof typeof absenceColours

export const absenceBackgroundColours: { [k in AbsenceType]: string } = {
  UNKNOWN_ABSENCE: colors.accents.green,
  OTHER_ABSENCE: colors.blues.dark,
  SICKLEAVE: colors.accents.violet,
  PLANNED_ABSENCE: colors.blues.light,
  PARENTLEAVE: colors.blues.primary,
  FORCE_MAJEURE: colors.accents.red,
  TEMPORARY_RELOCATION: colors.accents.orange,
  TEMPORARY_VISITOR: colors.accents.yellow,
  PRESENCE: colors.greyscale.white
}

export const absenceBorderColours: { [k in AbsenceType]: string } = {
  UNKNOWN_ABSENCE: colors.accents.green,
  OTHER_ABSENCE: colors.blues.dark,
  SICKLEAVE: colors.accents.violet,
  PLANNED_ABSENCE: colors.blues.light,
  PARENTLEAVE: colors.blues.primary,
  FORCE_MAJEURE: colors.accents.red,
  TEMPORARY_RELOCATION: colors.accents.orange,
  TEMPORARY_VISITOR: colors.accents.yellow,
  PRESENCE: colors.greyscale.white
}

export default colors
