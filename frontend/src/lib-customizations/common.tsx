// SPDX-FileCopyrightText: 2017-2022 City of Espoo
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

const { main, grayscale, accents, status } = colors

export const absenceColors = {
  UNKNOWN_ABSENCE: accents.a6turquoise,
  OTHER_ABSENCE: grayscale.g100,
  SICKLEAVE: accents.a9pink,
  PLANNED_ABSENCE: status.success,
  PARENTLEAVE: accents.a5orangeLight,
  FORCE_MAJEURE: status.danger,
  TEMPORARY_RELOCATION: status.warning,
  TEMPORARY_VISITOR: status.warning,
  NO_ABSENCE: accents.a8lightBlue
}

export const attendanceColors = {
  ABSENT: grayscale.g35,
  DEPARTED: main.m3,
  PRESENT: status.success,
  COMING: accents.a5orangeLight
}

export const applicationBasisColors = {
  ADDITIONAL_INFO: main.m1,
  ASSISTANCE_NEED: accents.a6turquoise,
  CLUB_CARE: accents.a2orangeDark,
  DAYCARE: status.warning,
  DUPLICATE_APPLICATION: accents.a3emerald,
  EXTENDED_CARE: main.m3,
  HAS_ATTACHMENTS: accents.a9pink,
  SIBLING_BASIS: status.success,
  URGENT: status.danger
}

export const careTypeColors = {
  'backup-care': accents.a5orangeLight,
  club: grayscale.g15,
  daycare: status.success,
  daycare5yo: accents.a1greenDark,
  preparatory: accents.a6turquoise,
  preschool: main.m1,
  'school-shift-care': grayscale.g70,
  temporary: accents.a4violet
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
