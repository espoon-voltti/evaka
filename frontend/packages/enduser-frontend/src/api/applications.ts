// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import _ from 'lodash'
import { config } from '@evaka/enduser-frontend/src/config'
import {
  APPLICATION_STATUS,
  APPLICATION_TYPE,
  CLUB_DAYS,
  CLUB_HOURS,
  DAYCARE_TYPES,
  WEEKLY_HOURS
} from '@/constants'
import rest from '@/api/utils/rest-client'

import axios from 'axios'
import moment from 'moment'

const mapConstToValue = (o, path) => _.update(o, path, _.property('value'))
const mapValueToConst = (o, path, constant) =>
  _.update(o, path, (value) => _.find(_.values(constant), ['value', value]))

const mapDateToUIFormat = (o, path) =>
  _.update(o, path, (date) =>
    date ? moment(date, 'YYYY-MM-DD').format('DD.MM.YYYY') : null
  )
const mapDateToAPIFormat = (o, path) =>
  _.update(o, path, (date) =>
    date ? moment(date, 'DD.MM.YYYY').format('YYYY-MM-DD') : null
  )

const deserializeApplication = (applicationData) => {
  const application = applicationData
  if (typeof application !== 'object') {
    return
  }

  application.type = application.type.toLowerCase()
  application.form.preferences.preferredUnits = application.form.preferences.preferredUnits.map(unit => unit.id)

  mapDateToUIFormat(application, 'form.preferences.preferredStartDate')
  if (application.form.child.futureAddress){
    mapDateToUIFormat(application, 'form.child.futureAddress.movingDate')
  }
  if (application.form.guardian.futureAddress){
    mapDateToUIFormat(application, 'form.guardian.futureAddress.movingDate')
  }
  if(application.form.secondGuardian && application.form.secondGuardian.futureAddress){
    mapDateToUIFormat(application, 'form.secondGuardian.futureAddress.movingDate')
  }

  return application
}

const serializeApplicationForm = (form) => {
  mapDateToAPIFormat(form, 'preferences.preferredStartDate')
  if (form.child.futureAddress){
    mapDateToAPIFormat(form, 'child.futureAddress.movingDate')
  }
  if (form.guardian.futureAddress){
    mapDateToAPIFormat(form, 'guardian.futureAddress.movingDate')
  }
  if(form.secondGuardian && form.secondGuardian.futureAddress){
    mapDateToAPIFormat(form, 'secondGuardian.futureAddress.movingDate')
  }
  form.preferences.preferredUnits = form.preferences.preferredUnits.map(id => ({ id, name: '' }))

  return form
}

const client = axios.create({
  baseURL: '/api/application'
})

export default {
  getApplications: () => {
    return client
      .get('/enduser/v2/applications')
      .then((response) => response.data)
      .then((applications) => _.map(applications, deserializeApplication))
  },
  removeApplication: (id) => client.delete(`/enduser/v2/applications/${id}`),
  getApplication: (id) => client.get(`/enduser/v2/applications/${id}`).then(res => deserializeApplication(res.data)),
  createApplication: (type, childId) => client.post('/enduser/v2/applications', { type, childId }),
  updateApplication: (type, application) => client
    .put(`/enduser/v2/applications/${application.id}`, serializeApplicationForm(application.form))
    .then(res => deserializeApplication(res.data)),
  sendApplication: (applicationId) => client.post(`/enduser/v2/applications/${applicationId}/actions/send-application`)
}
