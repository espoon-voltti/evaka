// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as types from '@/store/mutation-types'
import * as _ from 'lodash/lang'

const initialState = {
  filters: {
    address: null,
    language: [],
    daycareType: 'club',
    roundTheClock: false,
    district: null
  },
  isDisabled: false,
  disabledFilters: null
}

const currentState = _.cloneDeep(initialState)

const getters = {
  filters: (state) => state.filters,
  address: (state) => state.filters.address
}

const actions = {
  applyAddressFilter({ dispatch, commit }, addressFilter) {
    commit(types.APPLY_ADDRESS_FILTER, addressFilter)
  },
  clearAddressFilter({ commit }) {
    commit(types.CLEAR_ADDRESS_FILTER)
  },
  applyLanguageFilter({ dispatch, commit }, languageFilter) {
    commit(types.APPLY_LANGUAGE_FILTER, languageFilter)
  },
  applyDaycareFilter({ dispatch, commit }, daycareFilter) {
    commit(types.APPLY_DAYCARE_FILTER, daycareFilter)
  },
  disableFilters({ commit, state }) {
    if (!state.isDisabled) {
      commit(types.DISABLE_FILTERS)
    }
  },
  enableFilters({ commit, state }) {
    if (state.isDisabled) {
      commit(types.ENABLE_FILTERS)
    }
  },
  initialFilters({ commit }) {
    commit(types.SELECT_LOCATION, null)
    commit(types.INIT_FILTERS)
  }
}

const mutations = {
  [types.APPLY_ADDRESS_FILTER](state, addressFilter) {
    state.filters.address = addressFilter
  },
  [types.CLEAR_ADDRESS_FILTER](state) {
    state.filters.address = null
  },
  [types.APPLY_LANGUAGE_FILTER](state, languageFilter) {
    state.filters.language = languageFilter
  },
  [types.APPLY_DAYCARE_FILTER](state, daycareFilter) {
    state.filters.daycareType = daycareFilter
  },
  [types.DISABLE_FILTERS](state) {
    state.disabledFilters = state.filters
    state.filters = _.cloneDeep(initialState.filters)
    state.filters.daycareType = state.disabledFilters.daycareType
    // Address marker should be visible
    if (state.disabledFilters.address) {
      state.filters.address = _.cloneDeep(state.disabledFilters.address)
      state.filters.address.distance = 50000
    }
    state.isDisabled = true
  },
  [types.ENABLE_FILTERS](state) {
    state.filters = state.disabledFilters
    state.isDisabled = false
  },
  [types.INIT_FILTERS](state) {
    Object.assign(state, _.cloneDeep(initialState))
  }
}

export default {
  state: currentState,
  getters,
  actions,
  mutations
}
