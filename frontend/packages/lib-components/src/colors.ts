// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import customizations from '@evaka/evaka-customization'

export const cityBrandColors = customizations.colors.cityBrandColors
export const primaryColors = customizations.colors.primaryColors
export const greyscale = customizations.colors.greyscale
export const accentColors = customizations.colors.accents

const colors = customizations.colors

export default colors

// todo: the below colors could be thought out again

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
  OTHER_ABSENCE: colors.primaryColors.dark,
  SICKLEAVE: colors.accents.violet,
  PLANNED_ABSENCE: colors.primaryColors.light,
  PARENTLEAVE: colors.primaryColors.primary,
  FORCE_MAJEURE: colors.accents.red,
  TEMPORARY_RELOCATION: colors.accents.orange,
  TEMPORARY_VISITOR: colors.accents.yellow,
  PRESENCE: colors.greyscale.white
}

export const absenceBorderColours: { [k in AbsenceType]: string } = {
  UNKNOWN_ABSENCE: colors.accents.green,
  OTHER_ABSENCE: colors.primaryColors.dark,
  SICKLEAVE: colors.accents.violet,
  PLANNED_ABSENCE: colors.primaryColors.light,
  PARENTLEAVE: colors.primaryColors.primary,
  FORCE_MAJEURE: colors.accents.red,
  TEMPORARY_RELOCATION: colors.accents.orange,
  TEMPORARY_VISITOR: colors.accents.yellow,
  PRESENCE: colors.greyscale.white
}
