{
  /*
SPDX-FileCopyrightText: 2021 City of Tampere

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import type { MapConfig } from 'lib-customizations/types'

const mapConfig: MapConfig = {
  center: [61.49836, 23.76036],
  initialZoom: 11,
  addressZoom: 14,
  searchAreaRect: {
    maxLatitude: 61.83715535012332,
    minLatitude: 61.42731906621412,
    maxLongitude: 24.118938446044925,
    minLongitude: 23.54256391525269
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
