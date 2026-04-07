// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { MapConfig } from 'lib-customizations/types'

const mapConfig: MapConfig = {
  center: [61.63827, 23.19625],
  initialZoom: 14,
  addressZoom: 14,
  searchAreaRect: {
    maxLatitude: 61.65501,
    minLatitude: 61.63087,
    maxLongitude: 23.21197,
    minLongitude: 23.17912
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
