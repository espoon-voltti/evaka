// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FeeThresholds } from 'lib-common/api-types/finance'

export { FeeThresholds } from 'lib-common/api-types/finance'

export const familySizes = ['2', '3', '4', '5', '6'] as const
export type FamilySize = typeof familySizes[number]

export interface FeeThresholdsWithId {
  id: string
  thresholds: FeeThresholds
}
