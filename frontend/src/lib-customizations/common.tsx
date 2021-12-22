// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import customizations from '@evaka/customizations/common'
import { isArray } from 'lodash'
import React from 'react'
import type { CommonCustomizations } from './types'

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const { theme }: CommonCustomizations = customizations

export { theme }

// mimic lib-components/colors api:

export const { colors } = theme

const { main, greyscale, accents } = colors

export const absenceColours = {
  UNKNOWN_ABSENCE: accents.successGreen,
  OTHER_ABSENCE: main.dark,
  SICKLEAVE: accents.violet,
  PLANNED_ABSENCE: main.light,
  PARENTLEAVE: main.primary,
  FORCE_MAJEURE: accents.dangerRed,
  TEMPORARY_RELOCATION: accents.warningOrange,
  TEMPORARY_VISITOR: accents.peach,
  PRESENCE: greyscale.white
}

export const applicationBasisColors = {
  ADDITIONAL_INFO: main.dark,
  ASSISTANCE_NEED: accents.turquoise,
  CLUB_CARE: accents.orangeDark,
  DAYCARE: accents.warningOrange,
  DUPLICATE_APPLICATION: accents.emerald,
  EXTENDED_CARE: main.light,
  HAS_ATTACHMENTS: accents.pink,
  SIBLING_BASIS: accents.successGreen,
  URGENT: accents.dangerRed
}

export const careTypeColors = {
  'backup-care': colors.accents.peach,
  club: colors.greyscale.lighter,
  daycare: colors.accents.successGreen,
  daycare5yo: colors.accents.greenDark,
  preparatory: colors.accents.turquoise,
  preschool: colors.main.dark,
  'school-shift-care': colors.greyscale.dark,
  temporary: colors.accents.violet
}

export const translationsMergeCustomizer = (
  origValue: Record<string, unknown>,
  customizedValue: Record<string, unknown>
): Record<string, unknown> | undefined => {
  if (
    customizedValue != undefined &&
    (isArray(origValue) || React.isValidElement(origValue))
  ) {
    return customizedValue
  }
  return undefined
}

export default colors
