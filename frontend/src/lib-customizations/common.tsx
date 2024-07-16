// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import defaultsUntyped from '@evaka/customizations/common'
import isArray from 'lodash/isArray'
import mergeWith from 'lodash/mergeWith'
import React from 'react'

import { JsonOf } from 'lib-common/json'
import {
  faBabyCarriage,
  faEuroSign,
  faThermometer,
  faTreePalm
} from 'lib-icons'

import type { CommonCustomizations } from './types'

export const mergeCustomizer = (
  original: unknown,
  customized: unknown
): unknown =>
  isArray(customized) || React.isValidElement(customized as never)
    ? customized
    : undefined // fall back to default merge logic

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const defaults: CommonCustomizations = defaultsUntyped

declare global {
  interface EvakaWindowConfig {
    commonCustomizations?: Partial<JsonOf<CommonCustomizations>>
  }
}

const overrides =
  typeof window !== 'undefined' ? window.evaka?.commonCustomizations : undefined

const customizations: CommonCustomizations = overrides
  ? mergeWith({}, defaults, overrides, mergeCustomizer)
  : defaults

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
  FREE_ABSENCE: accents.a7mint,
  TEMPORARY_RELOCATION: status.warning,
  NO_ABSENCE: accents.a8lightBlue,
  UNAUTHORIZED_ABSENCE: accents.a4violet,
  ...colors.absences
}

export const additionalLegendItemColors = {
  CONTRACT_DAYS: accents.a1greenDark
}

export const absenceIcons = {
  UNKNOWN_ABSENCE: '?',
  OTHER_ABSENCE: faTreePalm,
  SICKLEAVE: faThermometer,
  PLANNED_ABSENCE: 'P',
  PARENTLEAVE: faBabyCarriage,
  FORCE_MAJEURE: faEuroSign,
  FREE_ABSENCE: faEuroSign,
  TEMPORARY_RELOCATION: '-',
  UNAUTHORIZED_ABSENCE: '-',
  NO_ABSENCE: '-'
}

export const additionalLegendItemIcons = {
  CONTRACT_DAYS: 'S'
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
  'connected-daycare': status.success,
  club: grayscale.g15,
  daycare: status.success,
  daycare5yo: accents.a1greenDark,
  preparatory: accents.a6turquoise,
  preschool: main.m1,
  'school-shift-care': grayscale.g70,
  temporary: accents.a4violet
}

export default colors
