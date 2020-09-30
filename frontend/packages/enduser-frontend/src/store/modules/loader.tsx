// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ActionTree, MutationTree, Module, GetterTree } from 'vuex'
import { uuid } from '@/utils/helpers'

export interface Loader {
  id: string
  message: string
}

export interface LoaderState {
  loaders: Loader[]
}

const initialState: LoaderState = {
  loaders: []
}

const actions: ActionTree<LoaderState, any> = {
  add({ dispatch, commit }, message: string) {
    const id = uuid()

    commit('add', {
      id,
      message
    })
    return () => dispatch('remove', id)
  },

  remove({ commit }, id: string) {
    commit('remove', id)
  },
  clear({ commit }) {
    commit('clear')
  }
}

const mutations: MutationTree<LoaderState> = {
  add(state, payload: Loader) {
    state.loaders = [...state.loaders, payload]
  },
  clear(state) {
    state.loaders = []
  },
  remove(state, id: string) {
    state.loaders = [...state.loaders.filter((l) => l.id !== id)]
  }
}

const getters: GetterTree<LoaderState, any> = {
  activeLoader({ loaders }): Loader | false {
    const lastIndex = loaders.length - 1
    return lastIndex > -1 ? loaders[lastIndex] : false
  }
}

const store: Module<LoaderState, any> = {
  namespaced: true,
  state: initialState,
  mutations,
  actions,
  getters
}

export default store
