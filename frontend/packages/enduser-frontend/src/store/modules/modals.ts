// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { uuid } from '@/utils/helpers'

const baseZ = 999 // z-index for the modal

const initialState = {
  messages: {}
}

const actions = {
  /**
   * @example payload
   * {
   *  type: 'warning',
   *  title: 'unexpected error occurred'
   *  text: 'error message text'
   * }
   */
  message: ({ commit }, payload) => {
    commit('show', payload)
  },

  close: ({ commit }, modalId) => {
    commit('close', modalId)
  }
}

const mutations = {
  show: (state, payload) => {
    const id = payload.id || uuid()
    state.messages = {
      ...state.messages,
      [id]: {
        ...payload,
        id,
        stack: baseZ + Object.keys(state.messages).length
      }
    }
  },
  close: (state, modalId) => {
    state.messages = {
      ...Object.keys(state.messages)
        .filter((k) => k !== modalId)
        .map((k) => state.messages[k])
    }
  }
}

const getters = {
  messages: (state) => Object.keys(state.messages).map((k) => state.messages[k])
}

export default {
  namespaced: true,
  state: initialState,
  actions,
  mutations,
  getters
}
