// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { MapConfig } from 'lib-customizations/types'

const mapConfig: MapConfig = {
  center: [60.184147, 24.704897],
  initialZoom: 12,
  addressZoom: 14,
  searchAreaRect: {
    maxLatitude: 60.35391259995084,
    minLatitude: 59.9451623086072,
    maxLongitude: 25.32055693401933,
    minLongitude: 24.271362626190594
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
