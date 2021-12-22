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

export const {
  colors: { main, greyscale, accents }
} = theme

const colors = {
  main,
  greyscale,
  accents
}

export const absenceColours = {
  UNKNOWN_ABSENCE: colors.accents.successGreen,
  OTHER_ABSENCE: colors.main.dark,
  SICKLEAVE: colors.accents.violet,
  PLANNED_ABSENCE: colors.main.light,
  PARENTLEAVE: colors.main.primary,
  FORCE_MAJEURE: colors.accents.dangerRed,
  TEMPORARY_RELOCATION: colors.accents.warningOrange,
  TEMPORARY_VISITOR: colors.accents.peach,
  PRESENCE: colors.greyscale.white
}

export const applicationBasisColors = {
  ADDITIONAL_INFO: colors.main.dark,
  ASSISTANCE_NEED: colors.accents.turquoise,
  CLUB_CARE: colors.accents.orangeDark,
  DAYCARE: colors.accents.warningOrange,
  DUPLICATE_APPLICATION: colors.accents.emerald,
  EXTENDED_CARE: colors.main.light,
  HAS_ATTACHMENTS: colors.accents.pink,
  SIBLING_BASIS: colors.accents.successGreen,
  URGENT: colors.accents.dangerRed
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
