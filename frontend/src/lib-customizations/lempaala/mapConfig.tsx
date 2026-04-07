// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { MapConfig } from 'lib-customizations/types'

const mapConfig: MapConfig = {
  center: [61.31373, 23.75649],
  initialZoom: 11,
  addressZoom: 14,
  searchAreaRect: {
    maxLatitude: 61.43158,
    minLatitude: 61.29899,
    maxLongitude: 23.72665,
    minLongitude: 23.80557
  },
  careTypeFilters: ['DAYCARE'],
  unitProviderTypeFilters: [
    'MUNICIPAL',
    'PURCHASED',
    'PRIVATE',
    'PRIVATE_SERVICE_VOUCHER'
  ]
}

export default mapConfig
