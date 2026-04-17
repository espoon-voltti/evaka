// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { MapConfig } from 'lib-customizations/types'

const mapConfig: MapConfig = {
  center: [61.48031, 23.50107],
  initialZoom: 11,
  addressZoom: 14,
  searchAreaRect: {
    maxLatitude: 61.50401,
    minLatitude: 61.43442,
    maxLongitude: 23.58649,
    minLongitude: 23.31557
  },
  careTypeFilters: ['DAYCARE', 'PRESCHOOL'],
  unitProviderTypeFilters: [
    'MUNICIPAL',
    'PURCHASED',
    'PRIVATE',
    'PRIVATE_SERVICE_VOUCHER'
  ]
}

export default mapConfig
