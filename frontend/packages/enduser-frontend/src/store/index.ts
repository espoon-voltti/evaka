// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Vue from 'vue'
import Vuex from 'vuex'
import units, { UnitsState } from '@/store/modules/units'
import addresses from '@/store/modules/addresses'
import filters from '@/store/modules/map-filters'
import forms from '@/store/modules/forms'
import applications, { ApplicationsState } from '@/store/modules/applications'
import maps from '@/store/modules/maps'
import * as types from './mutation-types'
import modals from '@/store/modules/modals'
import translations from '@/store/modules/translations'
import auth from '@/store/modules/auth'
import decisions, { DecisionsState } from '@/store/modules/decisions'
import loader, { LoaderState } from '@/store/modules/loader'

Vue.use(Vuex)

const debug = process.env.NODE_ENV !== 'production'

export interface RootState {
  addresses: any
  applications: ApplicationsState
  auth: any
  decisions: DecisionsState
  filters: any
  forms: any
  loader: LoaderState
  maps: any
  modals: any
  translations: any
  units: UnitsState
}

export default new Vuex.Store<RootState>({
  actions: {
    async init({ dispatch, commit }) {
      await Promise.all([
        dispatch('loader/add', 'loader.initializing'),
        dispatch('loadTranslations'),
        dispatch('loadUnits'),
      ])

      commit(types.INIT_FILTERS)
      commit(types.INIT_ADDRESSES)
    },
    async loadApplicationsAndDecisions({ dispatch }, userId) {
      await Promise.all([
        dispatch('loader/add', 'loader.initializing'),
        dispatch('loadApplications', userId),
        dispatch('loadDecisions', userId)
      ])
      dispatch('loader/clear')
    }
  },
  modules: {
    auth,
    decisions,
    units,
    addresses,
    filters,
    forms,
    applications,
    maps,
    translations,
    modals,
    loader
  },
  strict: debug
})
