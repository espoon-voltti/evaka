// SPDX-FileCopyrightText: 2023 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { MapConfig } from 'lib-customizations/types'

const mapConfig: MapConfig = {
  center: [61.30991, 23.61598],
  initialZoom: 11,
  addressZoom: 14,
  searchAreaRect: {
    maxLatitude: 61.39591,
    minLatitude: 61.19894,
    maxLongitude: 23.73969,
    minLongitude: 23.21942
  },
  careTypeFilters: ['DAYCARE'],
  unitProviderTypeFilters: ['MUNICIPAL', 'PURCHASED', 'PRIVATE']
}

export default mapConfig
