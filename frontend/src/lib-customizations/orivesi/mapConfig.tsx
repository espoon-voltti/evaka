// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { MapConfig } from 'lib-customizations/types'

const mapConfig: MapConfig = {
  center: [61.67744, 24.35737],
  initialZoom: 13,
  addressZoom: 14,
  searchAreaRect: {
    maxLatitude: 61.69872,
    minLatitude: 61.64029,
    maxLongitude: 24.37864,
    minLongitude: 24.32914
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
