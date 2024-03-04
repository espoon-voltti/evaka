// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const familySizes = ['2', '3', '4', '5', '6'] as const
export type FamilySize = (typeof familySizes)[number]

export type FeeThresholdsSaveError = 'date-overlap'
