// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios from 'axios'
import { config } from '@/config'

export default {
  autocompleteAddressQuery(address) {
    return axios
      .get(config.api.autocomplete, {
        params: {
          address
        }
      })
      .then((response) => response.data)
  },
  geocodePlaceId(placeId) {
    return axios
      .get(config.api.geocoding, {
        params: {
          id: placeId
        }
      })
      .then((response) => response.data[0].geometry.coordinates)
      .then((coordinates) =>
        Object.assign({}, { lat: coordinates[0], lng: coordinates[1] })
      )
  }
}
