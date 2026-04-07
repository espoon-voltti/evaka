// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { MapConfig } from 'lib-customizations/types'

const mapConfig: MapConfig = {
  center: [61.46685, 23.64141],
  initialZoom: 11,
  addressZoom: 14,
  searchAreaRect: {
    maxLatitude: 61.48725,
    minLatitude: 61.35686,
    maxLongitude: 23.47734,
    minLongitude: 23.74067
  },
  careTypeFilters: ['DAYCARE', 'PRESCHOOL', 'CLUB'],
  unitProviderTypeFilters: [
    'MUNICIPAL',
    'PURCHASED',
    'PRIVATE',
    'PRIVATE_SERVICE_VOUCHER'
  ]
}

export default mapConfig
