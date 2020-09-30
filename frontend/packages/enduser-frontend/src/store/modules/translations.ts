// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import translationsApi from '@/api/translations'
import * as types from '@/store/mutation-types'

const initialState = {
  locale: 'fi',
  translations: {
    countries: [],
    languages: []
  }
}
const getters = {
  countries: (state) => state.translations.countries,
  languages: (state) => state.translations.languages
}

const actions = {
  async loadTranslations({ commit, state }) {
    const countries = await translationsApi.getCountries(state.locale)
    commit(types.ADD_TRANSLATIONS, { countries })

    const languages = await translationsApi.getLanguages(state.locale)
    commit(types.ADD_TRANSLATIONS, { languages })
  }
}

const mutations = {
  [types.ADD_TRANSLATIONS](state, translations) {
    Object.assign(state.translations, translations)
  }
}

export default {
  state: initialState,
  getters,
  actions,
  mutations
}
