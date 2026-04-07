{
  /*
SPDX-FileCopyrightText: 2021 City of Oulu

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import type { MapConfig } from 'lib-customizations/types'

const mapConfig: MapConfig = {
  center: [65.01419, 25.47125],
  initialZoom: 11,
  addressZoom: 14,
  searchAreaRect: {
    maxLatitude: 65.02936831566872,
    minLatitude: 65.00209678931624,
    maxLongitude: 25.55391750178106,
    minLongitude: 25.448039386735797
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
