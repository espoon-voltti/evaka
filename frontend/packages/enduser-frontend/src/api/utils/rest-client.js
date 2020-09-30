// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import _ from 'lodash'
import axios from 'axios'
import { handleRequestError } from '../error-interceptor'

const instrument = (o, fname, fn) => {
  const old = o[fname]
  o[fname] = function() {
    return old.apply(o, fn(arguments))
  }
}

const firstArgToString = (args) => {
  const [first, ...rest] = args
  return [first + '', ...rest]
}

const createClient = (url, serializers = [], deserializres = []) => {
  const client = axios.create({
    baseURL: url,
    transformRequest: [
      _.cloneDeep,
      ...serializers,
      ...axios.defaults.transformRequest
    ],
    transformResponse: [...axios.defaults.transformResponse, ...deserializres]
  })

  client.interceptors.response.use(_.property('data'), handleRequestError)

  instrument(client, 'get', firstArgToString)
  instrument(client, 'delete', firstArgToString)
  instrument(client, 'head', firstArgToString)
  instrument(client, 'options', firstArgToString)
  instrument(client, 'post', firstArgToString)
  instrument(client, 'put', firstArgToString)
  instrument(client, 'patch', firstArgToString)

  return client
}

export default {
  createClient
}
