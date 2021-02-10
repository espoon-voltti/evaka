// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as types from '@/store/mutation-types'
import googleApi from '@/api/google'
import { config } from '@/config'
import * as _ from 'lodash/lang'

const initialState = {
  autocompletedAddresses: [],
  selectedLocation: null,
  autocompletedDestinations: [],
  selectedDestination: null,
  disabledDestination: null
}

const currentState = _.cloneDeep(initialState)

const getters = {
  autocompletedAddresses: (state) => state.autocompletedAddresses,
  selectedLocation: (state) => state.selectedLocation,
  autocompletedDestinations: (state) => state.autocompletedDestinations,
  selectedDestination: (state) => state.selectedDestination
}

const actions = {
  async searchAddresses({ commit }, address) {
    if (
      address.length >= config.filters.addressMinimumAutocompleteInputLength
    ) {
      commit(
        types.UPDATE_ADDRESSES,
        await googleApi.autocompleteAddressQuery(address)
      )
    } else {
      commit(types.CLEAR_ADDRESSES)
    }
  },

  async searchDestinations({ commit }, address) {
    if (address.length > config.filters.addressMinimumAutocompleteInputLength) {
      commit(
        types.UPDATE_DESTINATION,
        await googleApi.autocompleteAddressQuery(address)
      )
    } else {
      commit(types.CLEAR_DESTINATION)
    }
  },

  async selectLocation({ commit }, place) {
    if (place) {
      const location = await googleApi.geocodePlaceId(place.placeId)
      commit(types.SELECT_LOCATION, location)
    } else {
      commit(types.SELECT_LOCATION, null)
      commit(types.SELECT_DESTINATION, null)
    }
    commit(types.CLEAR_ADDRESSES)
  },

  async selectDestination({ commit }, place) {
    if (place) {
      const location = await googleApi.geocodePlaceId(place.placeId)
      commit(types.SELECT_DESTINATION, location)
    } else {
      commit(types.SELECT_DESTINATION, null)
    }
    commit(types.CLEAR_DESTINATION)
  },

  setDestination({ commit }, location) {
    commit(types.SELECT_DESTINATION, location)
  },

  enableDestination({ commit, state }) {
    if (state.selectedDestination === null) {
      commit(types.ENABLE_DESTINATION)
    }
  }
}

const mutations = {
  [types.UPDATE_ADDRESSES](state, results) {
    state.autocompletedAddresses = results.map((address) => ({
      address: address.name,
      placeId: address.id
    }))
  },

  [types.UPDATE_DESTINATION](state, results) {
    state.autocompletedDestinations = results.map((destination) => ({
      address: destination.name,
      placeId: destination.id
    }))
  },

  [types.CLEAR_ADDRESSES](state) {
    state.autocompletedAddresses = []
  },

  [types.CLEAR_DESTINATION](state) {
    state.autocompletedDestinations = []
  },

  [types.SELECT_LOCATION](state, selectedLocation) {
    state.selectedLocation = selectedLocation
  },

  [types.SELECT_DESTINATION](state, selectedDestination) {
    state.selectedDestination = selectedDestination
  },

  [types.INIT_ADDRESSES](state) {
    Object.assign(state, _.cloneDeep(initialState))
  },

  [types.ENABLE_DESTINATION]() {
    currentState.selectedDestination = _.cloneDeep(
      currentState.disabledDestination
    )
    currentState.disabledDestination = null
  },

  [types.DISABLE_DESTINATION]() {
    currentState.disabledDestination = _.cloneDeep(
      currentState.selectedDestination
    )
    currentState.selectedDestination = null
  }
}

export default {
  state: currentState,
  getters,
  actions,
  mutations
}
