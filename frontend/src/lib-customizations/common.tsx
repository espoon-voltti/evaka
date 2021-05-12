{
  /*
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import customizations from '@evaka/customizations/common'
import type { CommonCustomizations } from './types'

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const { theme }: CommonCustomizations = customizations

export { theme }

// mimic lib-components/colors api:

export const espooBrandColors = {
  espooBlue: theme.colors.brand.primary,
  espooTurquoise: theme.colors.brand.secondary,
  espooTurquoiseLight: theme.colors.brand.secondaryLight
}

export const {
  colors: { main: blueColors, greyscale, accents: accentColors }
} = theme

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
