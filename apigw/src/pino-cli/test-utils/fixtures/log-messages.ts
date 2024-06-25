// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AccessLog,
  PinoAccessLog,
  PinoAppAuditLog,
  AuditLog,
  PinoMiscLog,
  MiscLog
} from '../../cli/index.js'
import { ipv6ToIpv4 } from '../../cli/utils.js'

export const validPinoAccessLogMessage: PinoAccessLog = {
  level: 'info',
  '@timestamp': '2019-01-30T11:24:43.901Z',
  message: 'request completed',
  appName: 'nuora-api-gateway',
  appBuild: '123',
  appCommit: '1c82a1d18f210f0e4cfd091c377e4f0e9cf592cb',
  env: 'local',
  hostIp: '10.228.94.18',
  req: {
    id: '6',
    method: 'GET',
    url: '/api/grants/v1/grant-applications?associationId=43a8a9eb-9fff-4a6f-9aa1-6792a3024d74&periodId=a506303b-31e5-42c9-8ce0-7574d383c2a6',
    params: {},
    query: {},
    headers: {
      te: 'trailers',
      cookie:
        'XSRF-TOKEN=s2o2wXGc-xzCTMGMIGRgsD4LT4wM-T7VRPXw; nuora.local.sid=s%3AdtmQbMKjkuf2SAllngYu5_esQ9kr7WE9.%2Bx9t0JU5wGadEPEyfm7r0FqiuWOcIrYOvwIjIabuGN8',
      'x-xsrf-token': 's2o2wXGc-xzCTMGMIGRgsD4LT4wM-T7VRPXw',
      referer: 'https://localhost:8888/virkailija/fi/grant-applications',
      'accept-encoding': 'gzip, deflate, br',
      'accept-language': 'en-US,en;q=0.5',
      accept: 'application/json, text/plain, */*',
      'user-agent':
        'Mozilla/5.0 (X11; Linux x86_64; rv:66.0) Gecko/20100101 Firefox/66.0',
      host: 'localhost:8888',
      connection: 'close'
    },
    remoteAddress: '::ffff:172.19.0.1',
    remotePort: 43768,
    parentSpanId: '6692ad9c54ff94f9',
    spanId: '8e26717c418d7b8b',
    traceId: '181b7fe0c99486593ad9c545794f97f3',
    userIdHash:
      'af8dcd13ce05e32f4aeb2f18f10231ff8dfaf055066155fd985a3e46a484f855',
    path: '/api/grants/v1/grant-applications',
    queryString:
      '?associationId=43a8a9eb-9fff-4a6f-9aa1-6792a3024d74&periodId=a506303b-31e5-42c9-8ce0-7574d383c2a6'
  },
  res: {
    statusCode: 200,
    headers: {
      'x-dns-prefetch-control': 'off',
      'x-frame-options': 'SAMEORIGIN',
      'strict-transport-security': 'max-age=15552000; includeSubDomains',
      'x-download-options': 'noopen',
      'x-content-type-options': 'nosniff',
      'x-xss-protection': '1; mode=block',
      'x-content-length': '19924',
      'set-cookie':
        'nuora.local.sid=s%3AdtmQbMKjkuf2SAllngYu5_esQ9kr7WE9.%2Bx9t0JU5wGadEPEyfm7r0FqiuWOcIrYOvwIjIabuGN8; Path=/; Expires=Wed, 30 Jan 2019 11:56:43 GMT; HttpOnly'
    },
    contentLength: 19924
  },
  responseTime: 192
}

export const validPinoAppAuditLogMessage: PinoAppAuditLog = {
  level: 'info',
  '@timestamp': '2019-01-30T11:24:43.901Z',
  appName: 'nuora-api-gateway',
  appBuild: '123',
  appCommit: '1c82a1d18f210f0e4cfd091c377e4f0e9cf592cb',
  env: 'local',
  hostIp: '10.228.94.18',
  description: 'User signed in',
  eventCode: 'nuora.espooad.sign_in',
  targetId: '6bf04a89-a634-4ad8-96f6-5b0c7e7c10c2',
  objectId: 'ee681df8-38bc-48a6-97d8-06a225abfd4c',
  securityLevel: 'low',
  securityEvent: true,
  type: 'app-audit-events',
  parentSpanId: '6692ad9c54ff94f9',
  spanId: '8e26717c418d7b8b',
  traceId: '181b7fe0c99486593ad9c545794f97f3',
  userId: '26e43b70-d0b4-4549-b14e-7532c6c47c06',
  userIdHash:
    'af8dcd13ce05e32f4aeb2f18f10231ff8dfaf055066155fd985a3e46a484f855',
  userIp: '172.18.0.1',
  version: 1
}

export const validPinoMiscLogMessage: PinoMiscLog = {
  '@timestamp': '2019-01-30T11:24:43.901Z',
  appBuild: '123',
  appCommit: '1c82a1d18f210f0e4cfd091c377e4f0e9cf592cb',
  appName: 'nuora-api-gateway',
  env: 'local',
  hostIp: '10.228.94.18',
  level: 'info',
  meta: {
    customField1: 'customValue1',
    customField2: 'customValue2'
  },
  message: 'User signed in',
  type: 'app-misc',
  parentSpanId: '6692ad9c54ff94f9',
  spanId: '8e26717c418d7b8b',
  traceId: '181b7fe0c99486593ad9c545794f97f3',
  userIdHash:
    'af8dcd13ce05e32f4aeb2f18f10231ff8dfaf055066155fd985a3e46a484f855',
  version: 1
}

export const validPinoMiscLogMessageWithError: PinoMiscLog = {
  '@timestamp': '2019-01-30T11:24:43.901Z',
  appBuild: '123',
  appCommit: '1c82a1d18f210f0e4cfd091c377e4f0e9cf592cb',
  appName: 'nuora-api-gateway',
  env: 'local',
  hostIp: '10.228.94.18',
  level: 'info',
  meta: {
    customField1: 'customValue1',
    customField2: 'customValue2'
  },
  message: 'User signed in',
  type: 'app-misc',
  parentSpanId: '6692ad9c54ff94f9',
  spanId: '8e26717c418d7b8b',
  traceId: '181b7fe0c99486593ad9c545794f97f3',
  exception: 'ValidTestException',
  stackTrace: `Valid test exception
multiline
stack trace`,
  userIdHash:
    'af8dcd13ce05e32f4aeb2f18f10231ff8dfaf055066155fd985a3e46a484f855',
  version: 1
}

interface Expected {
  validAccessLogMessage: AccessLog
  validAuditLogMessage: AuditLog
  validMiscLogMessage: MiscLog
  validMiscLogMessageWithError: MiscLog
}

export const expected: Expected = {
  validAccessLogMessage: {
    '@timestamp': validPinoAccessLogMessage['@timestamp'],
    appBuild: validPinoAccessLogMessage.appBuild,
    appCommit: validPinoAccessLogMessage.appCommit,
    appName: validPinoAccessLogMessage.appName,
    clientIp: ipv6ToIpv4(validPinoAccessLogMessage.req.remoteAddress),
    contentLength: validPinoAccessLogMessage.res.contentLength,
    env: validPinoAccessLogMessage.env,
    hostIp: validPinoAccessLogMessage.hostIp,
    httpMethod: validPinoAccessLogMessage.req.method,
    path: validPinoAccessLogMessage.req.path,
    queryString: validPinoAccessLogMessage.req.queryString,
    responseTime: validPinoAccessLogMessage.responseTime,
    parentSpanId: validPinoAccessLogMessage.req.parentSpanId,
    spanId: validPinoAccessLogMessage.req.spanId,
    statusCode: `${validPinoAccessLogMessage.res.statusCode}`,
    traceId: validPinoAccessLogMessage.req.traceId,
    type: 'app-requests-received',
    userIdHash: validPinoAccessLogMessage.req.userIdHash,
    version: 1
  },
  validAuditLogMessage: {
    '@timestamp': validPinoAppAuditLogMessage['@timestamp'],
    appBuild: validPinoAppAuditLogMessage.appBuild,
    appCommit: validPinoAppAuditLogMessage.appCommit,
    appName: validPinoAppAuditLogMessage.appName,
    description: validPinoAppAuditLogMessage.description,
    env: validPinoAppAuditLogMessage.env,
    eventCode: validPinoAppAuditLogMessage.eventCode,
    hostIp: validPinoAppAuditLogMessage.hostIp,
    securityEvent: validPinoAppAuditLogMessage.securityEvent,
    securityLevel: validPinoAppAuditLogMessage.securityLevel,
    targetId: validPinoAppAuditLogMessage.targetId,
    objectId: validPinoAppAuditLogMessage.objectId,
    type: validPinoAppAuditLogMessage.type,
    parentSpanId: validPinoAppAuditLogMessage.parentSpanId,
    spanId: validPinoAppAuditLogMessage.spanId,
    traceId: validPinoAppAuditLogMessage.traceId,
    userId: validPinoAppAuditLogMessage.userId,
    userIdHash: validPinoAppAuditLogMessage.userIdHash,
    userIp: validPinoAppAuditLogMessage.userIp,
    version: validPinoAppAuditLogMessage.version
  },
  validMiscLogMessage: {
    '@timestamp': validPinoMiscLogMessage['@timestamp'],
    appBuild: validPinoMiscLogMessage.appBuild,
    appCommit: validPinoMiscLogMessage.appCommit,
    appName: validPinoMiscLogMessage.appName,
    env: validPinoMiscLogMessage.env,
    hostIp: validPinoMiscLogMessage.hostIp,
    logLevel: validPinoMiscLogMessage.level,
    message: validPinoMiscLogMessage.message,
    meta: {
      // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
      customField1: validPinoMiscLogMessage.meta!.customField1,
      // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
      customField2: validPinoMiscLogMessage.meta!.customField2
    },
    type: validPinoMiscLogMessage.type,
    parentSpanId: validPinoAppAuditLogMessage.parentSpanId,
    spanId: validPinoAppAuditLogMessage.spanId,
    traceId: validPinoAppAuditLogMessage.traceId,
    userIdHash: validPinoMiscLogMessage.userIdHash,
    version: validPinoMiscLogMessage.version
  },
  validMiscLogMessageWithError: {
    '@timestamp': validPinoMiscLogMessage['@timestamp'],
    appBuild: validPinoMiscLogMessage.appBuild,
    appCommit: validPinoMiscLogMessage.appCommit,
    appName: validPinoMiscLogMessage.appName,
    env: validPinoMiscLogMessage.env,
    exception: validPinoMiscLogMessageWithError.exception,
    hostIp: validPinoMiscLogMessage.hostIp,
    logLevel: validPinoMiscLogMessage.level,
    message: validPinoMiscLogMessageWithError.message,
    meta: {
      // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
      customField1: validPinoMiscLogMessage.meta!.customField1,
      // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
      customField2: validPinoMiscLogMessage.meta!.customField2
    },
    stackTrace: validPinoMiscLogMessageWithError.stackTrace,
    type: validPinoMiscLogMessage.type,
    parentSpanId: validPinoAppAuditLogMessage.parentSpanId,
    spanId: validPinoAppAuditLogMessage.spanId,
    traceId: validPinoAppAuditLogMessage.traceId,
    userIdHash: validPinoMiscLogMessage.userIdHash,
    version: validPinoMiscLogMessage.version
  }
}
