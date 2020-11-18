// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import _ from 'lodash'
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
  const application = applicationData.form
  if (typeof application !== 'object') {
    return
  }

  application.id = applicationData.id
  application.createdDate = applicationData.createdDate
  application.modifiedDate = applicationData.modifiedDate
  application.status = applicationData.status
  application.transferApplication = applicationData.transferApplication
  application.hasOtherVtjGuardian = applicationData.hasOtherVtjGuardian
  application.otherVtjGuardianHasSameAddress = applicationData.otherVtjGuardianHasSameAddress

  if (application.type === APPLICATION_TYPE.CLUB.value) {
    mapValueToConst(application, 'daycare.weeklyHours', WEEKLY_HOURS)
    mapValueToConst(application, 'apply.primaryCareType', DAYCARE_TYPES)
    mapValueToConst(application, 'apply.secondaryCareType', DAYCARE_TYPES)
    mapValueToConst(application, 'careDetails.daysPerWeek', CLUB_DAYS)
    mapValueToConst(application, 'careDetails.hoursPerDay', CLUB_HOURS)
    // FIXME use a proper datepicker component or do formatting at backend
    mapDateToUIFormat(application, 'child.childMovingDate')
    mapDateToUIFormat(application, 'guardian.guardianMovingDate')
    mapDateToUIFormat(application, 'preferredStartDate')
  }

  mapValueToConst(application, 'type', APPLICATION_TYPE)
  mapValueToConst(application, 'status', APPLICATION_STATUS)

  application.child.id = applicationData.childId
  application.guardian.id = applicationData.guardianId

  return application
}


const serializeApplication = (application) => {
  if (typeof application !== 'object') {
    return
  }

  if (application.type.value === APPLICATION_TYPE.CLUB.value) {
    mapConstToValue(application, 'daycare.weeklyHours')
    mapConstToValue(application, 'apply.primaryCareType')
    mapConstToValue(application, 'apply.secondaryCareType')
    mapConstToValue(application, 'careDetails.daysPerWeek')
    mapConstToValue(application, 'careDetails.hoursPerDay')
    mapDateToAPIFormat(application, 'child.childMovingDate')
    mapDateToAPIFormat(application, 'guardian.guardianMovingDate')
    // FIXME use a proper datepicker component or do formatting at backend
    mapDateToAPIFormat(application, 'preferredStartDate')
  }

  mapConstToValue(application, 'type')
  mapConstToValue(application, 'status')

  application.guardianId = application.guardian.id
  application.guardian.id = undefined
  application.childId = application.child.id
  application.child.id = undefined

  return application
}

const APPLICATION_API_V1 = rest.createClient(
  '/api/application',
  [serializeApplication],
  [deserializeApplication]
)

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
  getApplication: (id) => APPLICATION_API_V1.get(`/enduser/v2/applications/${id}`),
  createApplication: (type, childId) => client.post('/enduser/v2/applications', { type, childId }),
  updateApplication: (type, application) => APPLICATION_API_V1.put(`/enduser/v2/applications/${application.id}`, application),
  sendApplication: (applicationId) => client.post(`/enduser/v2/applications/${applicationId}/actions/send-application`),
  saveAttachment: (applicationId, file, onUploadProgress) => {
    const formData = new FormData()
    formData.append('file', file)
    return client.post(`/attachments/enduser/applications/${applicationId}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress
    })
  },
  deleteAttachment: (id) => client.delete(`/attachments/enduser/${id}`)
}
