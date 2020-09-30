// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios from 'axios'
import { config } from '@evaka/enduser-frontend/src/config'

export default {
  getAreas() {
    return axios.get(config.api.areas).then((response) => response.data)
  },
  getApplicationUnits(date, type) {
    return axios.get('/api/application/public/units', { params: { date, type }})
      .then((res) => res.data)
  }
}
