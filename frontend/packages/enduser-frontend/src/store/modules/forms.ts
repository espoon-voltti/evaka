// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as types from '@/store/mutation-types'
import _ from 'lodash'
import {
  APPLICATION_TYPE,
  NORMAL_CARE_END,
  NORMAL_CARE_START
} from '@/constants.ts'
import moment from 'moment'
import { Module } from 'vuex'
import { RootState } from '@/store'

const currentState = {
  application: {
    type: APPLICATION_TYPE.CLUB.value,
    form: {
      child: {
        person: {
          firstName: '',
          lastName: '',
          socialSecurityNumber: ''
        },
        address: {
          street: '',
          postalCode: '',
          city: ''
        },
        futureAddress: null,
        nationality: '',
        language: '',
        allergies: '',
        diet: '',
        assistanceNeeded: false,
        assistanceDescription: ''
      },
      guardian: {
        person: {
          firstName: '',
          lastName: '',
          socialSecurityNumber: ''
        },
        address: {
          street: '',
          postalCode: '',
          city: ''
        },
        futureAddress: null,
        phoneNumber: '',
        email: ''
      },
      secondGuardian: null,
      otherPartner: null,
      otherChildren: [],
      preferences: {
        preferredUnits: [],
        preferredStartDate: null,
        serviceNeed: null,
        siblingBasis: null,
        preparatory: false,
        urgent: false
      },
      otherInfo: '',
      maxFeeAccepted: false,
      clubDetails: null
    },
    status: 'CREATED',
    otherGuardianLivesInSameAddress: false,

    hasSecondGuardian: true,
    hasOtherVtjGuardian: false,
    term: null
  },

  loadedApplication: null,
  modified: {
    application: false
  },
  editing: {
    application: {
      isNew: true,
      validateAll: false,
      validations: {},
      activeSection: undefined
    }
  }
}

const defaultChild = {
  firstName: '',
  lastName: '',
  socialSecurityNumber: ''
}

const initialState = _.cloneDeep(currentState)

const isBeforeHours = (date, hours) => {
  return (
    !_.isEmpty(date) && moment(date, 'HH:mm').isBefore(moment(hours, 'HH:mm'))
  )
}

const isAfterHours = (date, hours) => {
  return (
    !_.isEmpty(date) && moment(date, 'HH:mm').isAfter(moment(hours, 'HH:mm'))
  )
}

const getAge = (birthday) => {
  const now = moment().startOf('day')
  return now.diff(birthday, 'years')
}

const module: Module<any, RootState> = {
  state: currentState,
  getters: {
    application: (state) => state.application,
    applicationId: (state) => state.application.id,
    fieldValue: (state) => (form, path) => _.get(state[form], path),
    isShiftCare: (state) =>
      state.application.form.preferences.serviceNeed &&
      state.application.form.preferences.serviceNeed.shiftCare,
    isPreschool: (state) => {
      const application = state.application
      return application.type === APPLICATION_TYPE.PRESCHOOL
    },
    validationsBySection: (state) => (section) => {
      return _(state.editing.application.validations)
        .values()
        .filter((v) => v.section.uuid === section)
        .value()
    },
    hasErrors: (state) =>
      _.filter(
        state.editing.application.validations,
        (validation) => validation.validator.invalid
      ).length > 0,
    hasServiceNeed: (state) => {
      return state.application.form.preferences.serviceNeed !== null
    },
    hasClubType: (state) => {
      return (
        state.application.type === APPLICATION_TYPE.CLUB.value
      )
    },
    hasOtherChildren: (state) => !_.isEmpty(state.application.form.otherChildren),
    getOtherChildren: (state) => state.application.form.otherChildren,
    getChildBirthday: (state) => {
      const ssn = state.application.form.child.socialSecurityNumber
      return ssn ? ssn.slice(0, 6) : null
    },
    getChildAge: (state, getters) =>
      getAge(moment(getters.getChildBirthday, 'DDMMYY')),
    selectedTerm: (state) => state.application.term
  },
  actions: {
    updateForm({ commit }, payload) {
      commit(types.UPDATE_FORM, payload)
    },
    updateValidation({ commit }, validation) {
      commit(types.UPDATE_VALIDATION, validation)
    },
    validateAll({ commit }) {
      commit(types.VALIDATE_ALL)
    },
    removeValidation({ commit }, validationId) {
      commit(types.REMOVE_VALIDATION, validationId)
    },
    toggleSection({ commit }, sectionId) {
      commit(types.TOGGLE_SECTION, sectionId)
    },
    addChild({ commit }) {
      commit(types.ADD_CHILD, Object.assign({}, defaultChild))
    },
    removeChild({ commit }, index) {
      commit(types.REMOVE_CHILD, index)
    },
    childrenUpdated({ commit }, { value, index }) {
      commit(types.UPDATE_CHILD, { value, index })
    },
    removeChildren({ commit }, index) {
      commit(types.REMOVE_CHILDREN, index)
    }
  },
  mutations: {
    [types.CLEAR_FORM](state, form) {
      state[form] = _.cloneDeep(initialState[form])
      state.modified[form] = false
      state.editing.application.validateAll = false
      state.editing.application.validations = {}
    },
    [types.UPDATE_FORM](state, { form, field, value }) {
      const oldValue = _.get(state[form], field)
      _.set(state[form], field, value)
      // Detecting state change with (strict) equality can produce false positives
      // This is accepted as the form user will be prompted to save the form when exiting.
      state.modified[form] = oldValue !== value
    },
    [types.LOAD_APPLICATION_FORM](state, { application }) {
      state.application = Object.assign(state.application, application)
      state.loadedApplication = _.cloneDeep(state.application)
    },
    [types.REVERT_APPLICATION_FORM](state) {
      state.application = _.cloneDeep(state.loadedApplication)
    },
    [types.NEW_APPLICATION](state, isNew) {
      state.editing.application.isNew = isNew
      localStorage.setItem('isNew', isNew)
    },
    [types.UPDATE_VALIDATION](state, validation) {
      state.editing.application.validations = Object.assign(
        {},
        state.editing.application.validations,
        validation
      )
    },
    [types.ADD_CHILD](state, newChild) {
      state.application.form.otherChildren.push(newChild)
    },
    [types.UPDATE_CHILD](state, params) {
      _.merge(state.application.form.otherChildren[params.index], params.value)
    },
    [types.REMOVE_CHILD](state, index) {
      state.application.form.otherChildren.splice(index, 1)
    },
    [types.REMOVE_CHILDREN](state) {
      state.application.form.otherChildren = []
    },
    [types.VALIDATE_ALL](state) {
      state.editing.application.validateAll = true
      state.editing.application.validations = _.mapValues(
        state.editing.application.validations,
        (validation) => {
          return _.merge({}, validation, { validator: { dirty: true } })
        }
      )
    },
    [types.REMOVE_VALIDATION](state, validationId) {
      state.editing.application.validations = _.omit(
        state.editing.application.validations,
        validationId
      )
    },
    [types.TOGGLE_SECTION](state, sectionId) {
      const activeSection = state.editing.application.activeSection
      state.editing.application.activeSection =
        sectionId === activeSection ? undefined : sectionId
    }
  }
}

export default module
