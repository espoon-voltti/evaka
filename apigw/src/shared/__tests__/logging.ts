// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it, test } from '@jest/globals'

import { createSha256Hash } from '../crypto.js'
import {
  contentLengthResSerializer,
  queryStringReqSerializer,
  reqSerializer,
  resSerializer,
  userIdHashReqSerializer
} from '../logging.js'
import { PinoRequest, PinoResponse, UserPinoRequest } from '../types.js'

const path = '/api/grants/youth/v1/grant-applications'
const queryString =
  'associationId=3cfe8152-4e84-48ab-ae13-15bcfa9b69b7&periodId=a506303b-31e5-42c9-8ce0-7574d383c2a6'

const validPinoReq: PinoRequest = {
  url: `${path}?${queryString}`
}

const validPinoRes: PinoResponse = {
  headers: {
    'content-length': '123'
  }
}

describe('logging', () => {
  describe('userIdHashReqSerializer', () => {
    it('adds userIdHash to request when request has User', () => {
      const USER_ID = 'abc123'
      const MOCK_REQ: UserPinoRequest = {
        url: 'http://localhost',
        path: '/',
        raw: {
          user: {
            id: `${USER_ID}`,
            roles: []
          }
        }
      }

      const output = userIdHashReqSerializer(MOCK_REQ)

      expect(output).toHaveProperty('userIdHash')
      expect(output.userIdHash).toEqual(createSha256Hash(USER_ID))
    })
  })

  it('adds an empty string as userIdHash when request has no User', () => {
    const MOCK_REQ: UserPinoRequest = {
      url: 'http://localhost',
      path: '/',
      raw: {}
    }

    const output = userIdHashReqSerializer(MOCK_REQ)

    expect(output).toHaveProperty('userIdHash')
    expect(output.userIdHash).toEqual('')
  })

  it('adds an empty string as userIdHash when request User has null ID', () => {
    const MOCK_REQ: UserPinoRequest = {
      url: 'http://localhost',
      path: '/',
      raw: {
        user: {
          id: null,
          roles: []
        }
      }
    }

    const output = userIdHashReqSerializer(MOCK_REQ)

    expect(output).toHaveProperty('userIdHash')
    expect(output.userIdHash).toEqual('')
  })

  describe('queryStringReqSerializer', () => {
    describe('given a request with a query string', () => {
      test('returns a serialized request with a path and queryString fields', () => {
        const serializedReq = queryStringReqSerializer(validPinoReq)
        expect(serializedReq.path).toEqual(path)
        expect(serializedReq.queryString).toEqual(`?${queryString}`)
      })
    })

    describe('given a request without a query string', () => {
      test('returns a serialized request with a path field a an empty queryString field', () => {
        const reqWithoutQueryString: PinoRequest = {
          url: path
        }
        const serializedReq = queryStringReqSerializer(reqWithoutQueryString)
        expect(serializedReq.path).toEqual(path)
        expect(serializedReq.queryString).toEqual('')
      })
    })
  })

  describe('contentLengthResSerializer', () => {
    describe('given a response with a content length', () => {
      test('returns a serialized response with content length returned as is', () => {
        const serializedRes = contentLengthResSerializer(validPinoRes)
        expect(serializedRes.contentLength).toEqual(
          Number(validPinoRes.headers['content-length'])
        )
      })
    })

    describe('given a response without a content length', () => {
      test('returns a serialized response with content length set to -1', () => {
        const resWithoutContentLength: PinoResponse = {
          headers: {}
        }
        const serializedRes = contentLengthResSerializer(
          resWithoutContentLength
        )
        expect(serializedRes.contentLength).toEqual(-1)
      })
    })
  })

  describe('reqSerializer', () => {
    describe('given one serializer', () => {
      test('returns a correctly serialized request', () => {
        const serializedReq = reqSerializer([queryStringReqSerializer])(
          validPinoReq
        )
        expect(serializedReq.path).toEqual(path)
        expect(serializedReq.queryString).toEqual(`?${queryString}`)
      })
    })
  })

  describe('resSerializer', () => {
    describe('given one serializer', () => {
      test('returns a correctly serialized response', () => {
        const serializedRes = resSerializer([contentLengthResSerializer])(
          validPinoRes
        )
        expect(serializedRes.contentLength).toEqual(
          Number(validPinoRes.headers['content-length'])
        )
      })
    })
  })
})
