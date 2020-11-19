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
import applicationApi from '@/api/applications'
import { getUUID } from '@/utils/uuid'

const currentState = {
  application: {
    status: '',
    type: APPLICATION_TYPE.CLUB,
    preferredStartDate: null,
    wasOnDaycare: false,
    wasOnClubCare: false,
    term: null,
    careDetails: {
      assistanceNeeded: false,
      assistanceDescription: ''
    },
    apply: {
      preferredUnits: [],
      siblingBasis: false,
      siblingName: ''
    },
    child: {
      firstName: '',
      lastName: '',
      socialSecurityNumber: '',
      address: {
        street: '',
        postalCode: '',
        city: ''
      },
      hasCorrectingAddress: false,
      childMovingDate: null,
      correctingAddress: {
        street: '',
        postalCode: '',
        city: ''
      },
      nationality: '',
      language: ''
    },
    guardian: {
      firstName: '',
      lastName: '',
      socialSecurityNumber: '',
      phoneNumber: null,
      email: null,
      address: {
        street: '',
        postalCode: '',
        city: ''
      },
      hasCorrectingAddress: false,
      guardianMovingDate: null,
      correctingAddress: {
        street: '',
        postalCode: '',
        city: ''
      }
    },
    guardian2: {
      firstName: '',
      lastName: '',
      socialSecurityNumber: '',
      phoneNumber: null,
      email: null,
      address: {
        street: '',
        postalCode: '',
        city: ''
      },
      correctingAddress: {
        street: '',
        postalCode: '',
        city: ''
      }
    },
    otherGuardianAgreementStatus: null,
    hasSecondGuardian: true,
    hasOtherVtjGuardian: false,
    otherVtjGuardianHasSameAddress: false,
    guardiansSeparated: false,
    additionalDetails: {
      otherInfo: ''
    },
    maxFeeAccepted: false
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
  },
  daycare: {},
  files: {
    urgent: []
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
    applicationId: (state) => state.application.id,
    daycareForm: (state) => state.daycare,
    fieldValue: (state) => (form, path) => _.get(state[form], path),
    applicationForm: (state) => state.application,
    isExtendedCare: (state, getters) =>
      getters.hasCarePlan &&
      (isBeforeHours(state.application.daycare.arrival, NORMAL_CARE_START) ||
        isAfterHours(state.application.daycare.departure, NORMAL_CARE_END)),
    isShiftCare: (state, getters) =>
      getters.hasCarePlan &&
      (state.application.daycare.roundTheClockCare ||
        state.application.daycare.weekendCare ||
        getters.isExtendedCare),
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
    hasCarePlan: (state) => {
      const application = state.application
      return (
        application.type === APPLICATION_TYPE.DAYCARE || application.withDaycare
      )
    },
    hasClubType: (state) => {
      return (
        state.application.type.value.toUpperCase() ===
        APPLICATION_TYPE.CLUB.value.toUpperCase()
      )
    },
    hasOtherChildren: (state) => !_.isEmpty(state.application.otherChildren),
    getOtherChildren: (state) => state.application.otherChildren,
    getChildBirthday: (state) => {
      const ssn = state.application.child.socialSecurityNumber
      return ssn ? ssn.slice(0, 6) : null
    },
    getChildAge: (state, getters) =>
      getAge(moment(getters.getChildBirthday, 'DDMMYY')),
    selectedTerm: (state) => state.application.term,
    urgentFiles: (state) => state.files.urgent
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
    },
    async addUrgentFile({ commit }, { file, applicationId }) {
      const key = getUUID()
      const fileObject = { key, file, name: file.name, contentType: file.type }
      commit(types.ADD_URGENT_FILE, { ...fileObject, progress: 0 })
      const onUploadProgress = (event) => {
        const progress = Math.round((event.loaded / event.total) * 100)
        commit(types.UPDATE_URGENT_FILE, { ...fileObject, progress })
      }
      const { data } = await applicationApi.saveAttachment(
        applicationId,
        file,
        onUploadProgress
      )
      commit(types.UPDATE_URGENT_FILE, {
        ...fileObject,
        id: data,
        progress: 100
      })
    },
    async deleteUrgentFile({ commit }, { id }) {
      await applicationApi.deleteAttachment(id)
      commit(types.DELETE_URGENT_FILE, id)
    }
  },
  mutations: {
    [types.CLEAR_FORM](state, form) {
      state[form] = _.cloneDeep(initialState[form])
      state.modified[form] = false
      state.editing.application.validateAll = false
      state.editing.application.validations = {}
    },
    [types.CLEAR_DAYCARE_FORM](state) {
      state.daycare = {}
    },
    [types.UPDATE_DAYCARE_FORM](state, form) {
      state.daycare = form
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
      state.files.urgent = [...application.attachments]
    },
    [types.LOAD_DAYCARE_APPLICATION_FORM](state, { application }) {
      state.daycare = Object.assign(state.daycare, application)
      state.files.urgent = [...application.attachments]
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
      state.application.otherChildren.push(newChild)
    },
    [types.UPDATE_CHILD](state, params) {
      _.merge(state.application.otherChildren[params.index], params.value)
    },
    [types.REMOVE_CHILD](state, index) {
      state.application.otherChildren.splice(index, 1)
    },
    [types.REMOVE_CHILDREN](state) {
      state.application.otherChildren = []
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
    },
    [types.ADD_URGENT_FILE](state, file) {
      state.files.urgent = [...state.files.urgent, file]
    },
    [types.UPDATE_URGENT_FILE](state, file) {
      state.files.urgent = state.files.urgent.map((f) =>
        f.key === file.key ? file : f
      )
    },
    [types.DELETE_URGENT_FILE](state, id) {
      state.files.urgent = state.files.urgent.filter((f) => f.id !== id)
    }
  }
}

export default module
