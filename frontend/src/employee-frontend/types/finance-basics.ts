// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FeeThresholds } from 'lib-common/generated/api-types/invoicing'

export const familySizes = ['2', '3', '4', '5', '6'] as const
export type FamilySize = (typeof familySizes)[number]

export interface FeeThresholdsWithId {
  id: string
  thresholds: FeeThresholds
}

export type FeeThresholdsSaveError = 'date-overlap'
