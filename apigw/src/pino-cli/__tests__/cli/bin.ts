// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { spawnSync } from 'child_process'
import * as path from 'node:path'
import { fileURLToPath } from 'node:url'

import {
  expected,
  validPinoAccessLogMessage,
  validPinoAppAuditLogMessage,
  validPinoMiscLogMessage,
  validPinoMiscLogMessageWithError
} from '../../test-utils/fixtures/log-messages'
import { deepCopyObj } from '../../test-utils/utils'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const cliPath = path.join(
  __dirname,
  '..',
  '..',
  '..',
  '..',
  'dist',
  'pino-cli',
  'cli',
  'bin.js'
)

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const objToLogMessage = (obj: any): string => {
  return JSON.stringify(obj) + '\n'
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const logMessageToObj = (data: Buffer): any => {
  return JSON.parse(data.toString())
}

describe('transport', () => {
  describe('given non-json input', () => {
    test('filters the non-json input', () => {
      const invalidLogLine = 'not a valid log line\n'
      const cli = spawnSync(process.argv[0], [cliPath], {
        input: invalidLogLine
      })

      expect(cli.stdout.toString()).toEqual('')
      expect(cli.status).toEqual(0)
    })
  })

  describe('given json input that is not a pino access log nor a pino app audit log', () => {
    test('returns the original json input as is', () => {
      const obj1 = { name: 'foo:bar', version: 5, message: 'foo bar' }
      const cli1 = spawnSync(process.argv[0], [cliPath], {
        input: objToLogMessage(obj1)
      })

      expect(logMessageToObj(cli1.stdout)).toEqual(obj1)
      expect(cli1.status).toEqual(0)

      const obj2 = {
        name: 'foo:bar',
        version: 5,
        message: 'foo bar',
        req: { url: '/path' }
      }
      const cli2 = spawnSync(process.argv[0], [cliPath], {
        input: objToLogMessage(obj2)
      })

      expect(logMessageToObj(cli2.stdout)).toEqual(obj2)
      expect(cli2.status).toEqual(0)

      const obj3 = {
        name: 'foo:bar',
        version: 5,
        message: 'foo bar',
        req: { url: '/path' },
        res: { statusCode: 200 }
      }
      const cli3 = spawnSync(process.argv[0], [cliPath], {
        input: objToLogMessage(obj3)
      })

      expect(logMessageToObj(cli3.stdout)).toEqual(obj3)
      expect(cli3.status).toEqual(0)
    })
  })

  describe('given a valid pino access log for the health check url', () => {
    test('filters the log message from the output', () => {
      const pinoAccessLogMessage = deepCopyObj(validPinoAccessLogMessage)
      pinoAccessLogMessage.req.url = '/health'
      pinoAccessLogMessage.req.path = '/health'

      const cli = spawnSync(process.argv[0], [cliPath], {
        input: objToLogMessage(pinoAccessLogMessage)
      })

      expect(cli.stdout.toString()).toEqual('')
      expect(cli.status).toEqual(0)
    })
  })

  describe('pino access log', () => {
    describe('given a valid log message', () => {
      test('returns a valid access log message in proper format', () => {
        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(validPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(
          expected.validAccessLogMessage
        )
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing @timestamp field', () => {
      test('returns a log message in proper format without the @timestamp field', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage['@timestamp']
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        delete logMessage['@timestamp']

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing appBuild field', () => {
      test('returns a valid log message in proper format with the appBuild field set to an empty string', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.appBuild
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        logMessage.appBuild = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing appCommit field', () => {
      test('returns a valid log message in proper format with the appCommit field set to an empty string', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.appCommit
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        logMessage.appCommit = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing appName field', () => {
      test('returns a log message in proper format without the appName field', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.appName
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        delete logMessage.appName

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing remoteAddress field', () => {
      test('returns a valid log message in proper format with the clientIp field set to an empty string', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.req.remoteAddress
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        logMessage.clientIp = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing contentLength field', () => {
      test('returns a valid log message in proper format with the contentLength field set to -1', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.res.contentLength
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        logMessage.contentLength = -1

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing env field', () => {
      test('returns a log message in proper format without the env field', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.env
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        delete logMessage.env

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing hostIp field', () => {
      test('returns a valid log message in proper format with the hostIp field set to an empty string', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.hostIp
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        logMessage.hostIp = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing httpMethod field', () => {
      test('returns a valid log message in proper format with the httpMethod field set to an empty string', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.req.method
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        logMessage.httpMethod = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing path field', () => {
      test('returns a valid log message in proper format with the path field set to an empty string', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.req.path
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        logMessage.path = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing queryString field', () => {
      test('returns a valid log message in proper format with the queryString field set to an empty string', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.req.queryString
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        logMessage.queryString = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing responseTime field', () => {
      test('returns a valid log message in proper format with the responseTime field set to -1', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.responseTime
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        logMessage.responseTime = -1

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing statusCode field', () => {
      test('returns a valid log message in proper format with the statusCode field set to an empty string', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.res.statusCode
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        logMessage.statusCode = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing userIdHash field', () => {
      test('returns a valid log message in proper format with the userIdHash field set to an empty string', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.req.userIdHash
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        logMessage.userIdHash = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing parentSpanId field', () => {
      test('returns a valid log message in proper format with the parentSpanId field set to an empty string', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.req.parentSpanId
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        logMessage.parentSpanId = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing spanId field', () => {
      test('returns a valid log message in proper format with the spanId field set to an empty string', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.req.spanId
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        logMessage.spanId = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing traceId field', () => {
      test('returns a valid log message in proper format with the traceId field set to an empty string', () => {
        const invalidPinoAccessLogMessage = deepCopyObj(
          validPinoAccessLogMessage
        )
        delete invalidPinoAccessLogMessage.req.traceId
        const logMessage = deepCopyObj(expected.validAccessLogMessage)
        logMessage.traceId = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAccessLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })
  })

  describe('pino app audit log', () => {
    describe('given a valid log message', () => {
      test('returns a valid audit log message in proper format', () => {
        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(validPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(
          expected.validAuditLogMessage
        )
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing @timestamp field', () => {
      test('returns a log message in proper format without the @timestamp field', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage['@timestamp']
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        delete logMessage['@timestamp']

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing appBuild field', () => {
      test('returns a valid log message in proper format with the appBuild field set to an empty string', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.appBuild
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        logMessage.appBuild = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing appCommit field', () => {
      test('returns a valid log message in proper format with the appCommit field set to an empty string', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.appCommit
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        logMessage.appCommit = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing appName field', () => {
      test('returns a log message in proper format without the appName field', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.appName
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        delete logMessage.appName

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing description field', () => {
      test('returns a valid log message in proper format with the description field set to an empty string', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.description
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        logMessage.description = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing env field', () => {
      test('returns a log message in proper format without the env field', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.env
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        delete logMessage.env

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing eventCode field', () => {
      test('returns a valid log message in proper format with the eventCode field set to an empty string', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.eventCode
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        logMessage.eventCode = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing hostIp field', () => {
      test('returns a valid log message in proper format with the hostIp field set to an empty string', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.hostIp
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        logMessage.hostIp = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing objectId field', () => {
      test('returns a log message in proper format without the objectId field', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.objectId
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        delete logMessage.objectId

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing securityEvent field', () => {
      test('returns a valid log message in proper format with the securityEvent field set to false', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.securityEvent
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        logMessage.securityEvent = false

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing securityLevel field', () => {
      test('returns a valid log message in proper format with the securityLevel field set to an empty string', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.securityLevel
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        logMessage.securityLevel = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing targetId field', () => {
      test('returns a valid log message in proper format with the targetId field set to an empty string', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.targetId
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        logMessage.targetId = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing userId field', () => {
      test('returns a valid log message in proper format with the userId field set to an empty string', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.userId
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        logMessage.userId = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing userIdHash field', () => {
      test('returns a valid log message in proper format with the userIdHash field set to an empty string', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.userIdHash
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        logMessage.userIdHash = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing userIp field', () => {
      test('returns a valid log message in proper format with the userIp field set to an empty string', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.userIp
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        logMessage.userIp = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing version field', () => {
      test('returns a log message in proper format without the version field', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.version
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        delete logMessage.version

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing parentSpanId field', () => {
      test('returns a valid log message in proper format without the parentSpanId field set to an empty string', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.parentSpanId
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        logMessage.parentSpanId = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing spanId field', () => {
      test('returns a valid log message in proper format without the spanId field set to an empty string', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.spanId
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        logMessage.spanId = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing traceId field', () => {
      test('returns a valid log message in proper format without the traceId field set to an empty string', () => {
        const invalidPinoAppAuditLogMessage = deepCopyObj(
          validPinoAppAuditLogMessage
        )
        delete invalidPinoAppAuditLogMessage.traceId
        const logMessage = deepCopyObj(expected.validAuditLogMessage)
        logMessage.traceId = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoAppAuditLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })
  })

  describe('pino misc log', () => {
    describe('given a valid log message', () => {
      test('returns a valid misc log message in proper format', () => {
        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(validPinoMiscLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(
          expected.validMiscLogMessage
        )
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a valid log message with an error', () => {
      test('returns a valid misc log message in proper format, including exception and stackTrace', () => {
        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(validPinoMiscLogMessageWithError)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(
          expected.validMiscLogMessageWithError
        )
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing @timestamp field', () => {
      test('returns a log message in proper format without the @timestamp field', () => {
        const invalidPinoMiscLogMessage = deepCopyObj(validPinoMiscLogMessage)
        delete invalidPinoMiscLogMessage['@timestamp']
        const logMessage = deepCopyObj(expected.validMiscLogMessage)
        delete logMessage['@timestamp']

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing appBuild field', () => {
      test('returns a valid log message in proper format with the appBuild field set to an empty string', () => {
        const invalidPinoMiscLogMessage = deepCopyObj(validPinoMiscLogMessage)
        delete invalidPinoMiscLogMessage.appBuild
        const logMessage = deepCopyObj(expected.validMiscLogMessage)
        logMessage.appBuild = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing appCommit field', () => {
      test('returns a valid log message in proper format with the appCommit field set to an empty string', () => {
        const invalidPinoMiscLogMessage = deepCopyObj(validPinoMiscLogMessage)
        delete invalidPinoMiscLogMessage.appCommit
        const logMessage = deepCopyObj(expected.validMiscLogMessage)
        logMessage.appCommit = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing appName field', () => {
      test('returns a log message in proper format without the appName field', () => {
        const invalidPinoMiscLogMessage = deepCopyObj(validPinoMiscLogMessage)
        delete invalidPinoMiscLogMessage.appName
        const logMessage = deepCopyObj(expected.validMiscLogMessage)
        delete logMessage.appName

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing exception field', () => {
      test('returns a valid log message in proper format without the exception field', () => {
        const invalidPinoMiscLogMessageWithError = deepCopyObj(
          validPinoMiscLogMessageWithError
        )
        delete invalidPinoMiscLogMessageWithError.exception
        const logMessage = deepCopyObj(expected.validMiscLogMessageWithError)
        delete logMessage.exception

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessageWithError)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing level field', () => {
      test('returns a log message in proper format  with the logLevel field set to an empty string', () => {
        const invalidPinoMiscLogMessage = deepCopyObj(validPinoMiscLogMessage)
        delete invalidPinoMiscLogMessage.level
        const logMessage = deepCopyObj(expected.validMiscLogMessage)
        logMessage.logLevel = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing message field', () => {
      test('returns a valid log message in proper format with the message field set to an empty string', () => {
        const invalidPinoMiscLogMessage = deepCopyObj(validPinoMiscLogMessage)
        delete invalidPinoMiscLogMessage.message
        const logMessage = deepCopyObj(expected.validMiscLogMessage)
        logMessage.message = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing meta field', () => {
      test('returns a valid log message in proper format without the meta field', () => {
        const invalidPinoMiscLogMessage = deepCopyObj(validPinoMiscLogMessage)
        delete invalidPinoMiscLogMessage.meta
        const logMessage = deepCopyObj(expected.validMiscLogMessage)
        delete logMessage.meta

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing env field', () => {
      test('returns a log message in proper format without the env field', () => {
        const invalidPinoMiscLogMessage = deepCopyObj(validPinoMiscLogMessage)
        delete invalidPinoMiscLogMessage.env
        const logMessage = deepCopyObj(expected.validMiscLogMessage)
        delete logMessage.env

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing hostIp field', () => {
      test('returns a valid log message in proper format with the hostIp field set to an empty string', () => {
        const invalidPinoMiscLogMessage = deepCopyObj(validPinoMiscLogMessage)
        delete invalidPinoMiscLogMessage.hostIp
        const logMessage = deepCopyObj(expected.validMiscLogMessage)
        logMessage.hostIp = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing stackTrace field', () => {
      test('returns a valid log message in proper format without the stackTrace field', () => {
        const invalidPinoMiscLogMessageWithError = deepCopyObj(
          validPinoMiscLogMessageWithError
        )
        delete invalidPinoMiscLogMessageWithError.stackTrace
        const logMessage = deepCopyObj(expected.validMiscLogMessageWithError)
        delete logMessage.stackTrace

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessageWithError)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing userIdHash field', () => {
      test('returns a valid log message in proper format with the userIdHash field set to an empty string', () => {
        const invalidPinoMiscLogMessage = deepCopyObj(validPinoMiscLogMessage)
        delete invalidPinoMiscLogMessage.userIdHash
        const logMessage = deepCopyObj(expected.validMiscLogMessage)
        logMessage.userIdHash = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing version field', () => {
      test('returns a log message in proper format without the version field', () => {
        const invalidPinoMiscLogMessage = deepCopyObj(validPinoMiscLogMessage)
        delete invalidPinoMiscLogMessage.version
        const logMessage = deepCopyObj(expected.validMiscLogMessage)
        delete logMessage.version

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing parentSpanId field', () => {
      test('returns a valid log message in proper format with the parentSpanId field set to an empty string', () => {
        const invalidPinoMiscLogMessage = deepCopyObj(validPinoMiscLogMessage)
        delete invalidPinoMiscLogMessage.parentSpanId
        const logMessage = deepCopyObj(expected.validMiscLogMessage)
        logMessage.parentSpanId = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing spanId field', () => {
      test('returns a valid log message in proper format with the spanId field set to an empty string', () => {
        const invalidPinoMiscLogMessage = deepCopyObj(validPinoMiscLogMessage)
        delete invalidPinoMiscLogMessage.spanId
        const logMessage = deepCopyObj(expected.validMiscLogMessage)
        logMessage.spanId = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })

    describe('given a log message with a missing traceId field', () => {
      test('returns a valid log message in proper format with the traceId field set to an empty string', () => {
        const invalidPinoMiscLogMessage = deepCopyObj(validPinoMiscLogMessage)
        delete invalidPinoMiscLogMessage.traceId
        const logMessage = deepCopyObj(expected.validMiscLogMessage)
        logMessage.traceId = ''

        const cli = spawnSync(process.argv[0], [cliPath], {
          input: objToLogMessage(invalidPinoMiscLogMessage)
        })

        expect(logMessageToObj(cli.stdout)).toEqual(logMessage)
        expect(cli.status).toEqual(0)
      })
    })
  })
})
