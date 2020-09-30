// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as types from '@/store/mutation-types'

const initialState = {
  mapApiLoaded: false
}

const getters = {
  mapApiLoaded: (state) => state.mapApiLoaded
}

const mutations = {
  [types.MAP_API_LOADED](state) {
    state.mapApiLoaded = true
  }
}

export default {
  state: initialState,
  getters,
  mutations
}
