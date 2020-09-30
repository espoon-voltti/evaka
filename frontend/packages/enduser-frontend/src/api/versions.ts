// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios from 'axios'

export default {
  getVersion: async (name) => {
    const url = '/api/' + name + '/version'
    try {
      const v = await axios.get(url)
      return Object.assign(v.data, { url })
    } catch (e) {
      return { name, url, error: e.message }
    }
  },
  ownVersion: async () => {
    const v = await axios.get('static/version.json')
    return v.data
  }
}
