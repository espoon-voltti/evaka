// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios from 'axios'

const EMPTY_USER = {
  id: '',
  firstName: '',
  lastName: '',
  children: []
}

const initialState = {
  isLoggedIn: false,
  user: EMPTY_USER
}
const getters = {
  isLoggedIn: (state) => state.isLoggedIn,
  userName: (state) => `${state.user.firstName} ${state.user.lastName}`,
  userId: (state) => state.user.id,
  children: (state) => state.user.children
}

const LOG_IN = 'SET_LOGGED_IN'
const SET_USER = 'SET_LOGGED_IN_USER'

const actions = {
  async loadAuth({ commit, dispatch }) {
    const { loggedIn, user } = await axios
      .get('/api/application/auth/status')
      .then((res) => res.data)

    commit(LOG_IN, loggedIn)
    if (loggedIn) {
      commit(SET_USER, user)
    } else {
      commit(SET_USER, EMPTY_USER)
    }
    return loggedIn
  }
}

const mutations = {
  [LOG_IN](state, loggedIn) {
    state.isLoggedIn = loggedIn
  },
  [SET_USER](state, user) {
    state.user = user
  }
}

export default {
  state: initialState,
  getters,
  actions,
  mutations
}
