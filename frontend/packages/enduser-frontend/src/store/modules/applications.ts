// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import applicationApi from '@/api/applications'
import * as types from '@/store/mutation-types'
import router from '@/router'
import { APPLICATION_TYPE } from '../../constants'
import { Module } from 'vuex'
import { RootState } from '@/store'

export interface ApplicationsState {
  loading: boolean
  loaded: boolean
  applications: any[]
}

const initialState: ApplicationsState = {
  loading: false,
  loaded: false,
  applications: []
}

const getRouterPush = (name, id) => router.push({ name, params: { id } })

const module: Module<ApplicationsState, RootState> = {
  state: initialState,
  getters: {
    applicationsLoaded: (state) => state.loaded && !state.loading,
    applications: (state) => state.applications
  },
  actions: {
    async loadApplications({ commit }) {
      commit(types.START_LOADING_APPLICATIONS)
      const applications = await applicationApi.getApplications()
      commit(types.LOAD_APPLICATIONS, applications)
    },
    async loadApplication(
      { commit, state, dispatch },
      { type, applicationId }
    ) {
      commit(types.CLEAR_FORM, 'application')
      commit(types.CLEAR_DAYCARE_FORM)
      const application = await applicationApi.getApplication(applicationId)
      commit(type === 'CLUB' ? types.LOAD_APPLICATION_FORM : types.LOAD_DAYCARE_APPLICATION_FORM, { application })
    },
    async removeApplication({ commit }, { applicationId }) {
      await applicationApi.removeApplication(applicationId)
      commit(types.REMOVE_APPLICATION, applicationId)
      localStorage.removeItem('isNew')
    },
    async saveApplication({ getters, commit }, { type, applicationId, form }) {
      let application
      switch (type) {
        case APPLICATION_TYPE.DAYCARE.value:
          application = form
          break
        case APPLICATION_TYPE.PRESCHOOL.value:
          application = form
          break
        case APPLICATION_TYPE.CLUB.value:
          application = {
            ...getters.applicationForm,
            guardianInformed: getters.applicationForm.guardiansSeparated
          }
          break
        default:
          throw new Error('invalid application type')
      }

      const savedApplication = await applicationApi.updateApplication(
        type,
        application
      )
      commit(types.SAVE_APPLICATION, savedApplication)
      localStorage.removeItem('isNew')
      return applicationId
    },
    async sendApplication(
      { getters, dispatch, commit },
      { type, applicationId, form }
    ) {
      await dispatch('saveApplication', { type, applicationId, form })

      await applicationApi.sendApplication(applicationId)
      commit(types.LOAD_APPLICATIONS)

      localStorage.removeItem('isNew')
      return applicationId
    },
    async newApplication({ commit }, { type, childId }) {
      commit(types.CLEAR_FORM, 'application')
      commit(types.CLEAR_DAYCARE_FORM)
      commit(types.NEW_APPLICATION, true)

      await applicationApi
        .createApplication(type, childId)
        .then((res) => getRouterPush(`${type.toString().toLowerCase()}-application`, res.data))
    },
    editApplication({ commit }, { id, type }) {
      commit(types.CLEAR_FORM, 'application')
      commit(types.CLEAR_DAYCARE_FORM)
      commit(types.NEW_APPLICATION, false)

      return router.push({
        name: `${type.toString().toLowerCase()}-application`,
        params: {
          id
        }
      })
    },
    previewApplication({ commit }, { type, id }) {
      commit(types.CLEAR_FORM, 'application')
      commit(types.CLEAR_DAYCARE_FORM)
      commit(types.NEW_APPLICATION, false)
      return router.push(`/applications/preview/${type.toString().toLowerCase()}/${id}`)
    }
  },
  mutations: {
    [types.START_LOADING_APPLICATIONS](state) {
      state.loading = true
    },
    [types.LOAD_APPLICATIONS](state, applications) {
      state.applications = applications || []
      state.loading = false
      state.loaded = true
    },
    [types.SAVE_APPLICATION](state, application) {
      state.applications = state.applications
        .filter((a) => a.id !== application.id)
        .concat(application)
    },
    [types.REMOVE_APPLICATION](state, applicationId) {
      state.applications = state.applications.filter(
        (application) => application.id !== applicationId
      )
    }
  }
}

export default module
