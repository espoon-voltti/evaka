{
  /*
SPDX-FileCopyrightText: 2021 City of Turku

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import type { MapConfig } from 'lib-customizations/types'

const mapConfig: MapConfig = {
  center: [60.451389, 22.266667],
  initialZoom: 11,
  addressZoom: 14,
  searchAreaRect: {
    maxLatitude: 60.7,
    minLatitude: 60.3,
    maxLongitude: 22.8,
    minLongitude: 22.0
  },
  careTypeFilters: ['DAYCARE', 'PRESCHOOL', 'CLUB'],
  unitProviderTypeFilters: [
    'MUNICIPAL',
    'PRIVATE',
    'PRIVATE_SERVICE_VOUCHER',
    'PURCHASED'
  ]
}

export default mapConfig
