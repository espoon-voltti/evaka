// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SerializedRequest, SerializedResponse } from 'pino-std-serializers'
import split from 'split2'
import * as through from 'through2'

import { ipv6ToIpv4 } from './utils.js'

export interface BaseLog {
  '@timestamp': string
  appBuild: string
  appCommit: string
  appName: string
  env: string
  hostIp: string
  parentSpanId?: string
  spanId?: string
  traceId?: string
}

export interface AccessLog extends BaseLog {
  clientIp: string
  contentLength: number
  httpMethod: string
  path: string
  queryString: string
  responseTime: number
  statusCode: string
  type: 'app-requests-received'
  userIdHash: string
  version: number
}

export interface AuditLog extends BaseLog {
  description: string
  eventCode: string
  objectId?: string
  securityEvent: boolean
  securityLevel: string
  targetId: string | string[]
  type: 'app-audit-events'
  userId: string
  userIdHash: string
  userIp: string
  version: number
}

export interface MiscLog extends BaseLog {
  exception?: string
  logLevel: string
  message: string
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  meta?: Record<string, any>
  stackTrace?: string
  type: 'app-misc'
  userIdHash: string
  version: number
}

export interface PinoBaseLog {
  '@timestamp': string
  appBuild: string
  appCommit: string
  appName: string
  env: string
  hostIp: string
  level: string
  message?: string
  parentSpanId?: string
  spanId?: string
  traceId?: string
}

export interface PinoAccessLog extends PinoBaseLog {
  req: Omit<SerializedRequest, 'raw'> & {
    parentSpanId?: string
    path: string
    queryString: string
    spanId?: string
    traceId?: string
    userIdHash: string
  }
  res: Omit<SerializedResponse, 'raw'> & {
    contentLength: number
  }
  responseTime: number
}

export interface PinoAppAuditLog extends PinoBaseLog {
  description: string
  eventCode: string
  objectId?: string
  securityEvent: boolean
  securityLevel: string
  targetId: string | string[]
  type: 'app-audit-events'
  userId: string
  userIdHash: string
  userIp: string
  version: number
}

export interface PinoMiscLog extends PinoBaseLog {
  exception?: string
  message: string
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  meta?: Record<string, any>
  stackTrace?: string
  type: 'app-misc'
  userIdHash: string
  version: number
}

// TODO: the exceptions should be logged in the same format as other exception logs
interface ErrorLog {
  exception: string
  message: string
  stackTrace: string
}

const isHealthCheckRequest = (obj: PinoAccessLog): boolean =>
  obj.req.url === '/health'

// It would be possible to populate some of these fields through environment variables but that requires that all users
// of this library follow the same naming conventions for these environment variables
const mapPinoBaseLogToBaseLog = (obj: PinoBaseLog): BaseLog => ({
  '@timestamp': obj['@timestamp'],
  appBuild: obj.appBuild || '',
  appCommit: obj.appCommit || '',
  appName: obj.appName,
  env: obj.env,
  hostIp: obj.hostIp || '',
  parentSpanId: obj.parentSpanId || '',
  spanId: obj.spanId || '',
  traceId: obj.traceId || ''
})

const mapPinoLogToAccessLog = (obj: PinoAccessLog): AccessLog => ({
  ...mapPinoBaseLogToBaseLog(obj),
  // req.remoteAddress contains an IPv4-mapped IPv6 address, i.e., any IPv4 addresses are prefixed with ::ffff: to
  // transform them to IPv6 addresses. Thus, we can safely strip ::ffff: from the address. See
  // https://en.wikipedia.org/wiki/IPv6#IPv4-mapped_IPv6_addresses
  clientIp: (obj.req.remoteAddress && ipv6ToIpv4(obj.req.remoteAddress)) || '',
  contentLength: obj.res.contentLength || -1,
  httpMethod: obj.req.method || '',
  path: obj.req.path || '',
  queryString: obj.req.queryString || '',
  responseTime: obj.responseTime || -1,
  statusCode: (obj.res.statusCode && `${obj.res.statusCode}`) || '',
  parentSpanId: obj.req.parentSpanId || '',
  spanId: obj.req.spanId || '',
  traceId: obj.req.traceId || '',
  type: 'app-requests-received',
  userIdHash: obj.req.userIdHash || '',
  version: 1 // access log version is managed here as it's very much pino related
})

const mapPinoLogToAuditLog = (obj: PinoAppAuditLog): AuditLog => ({
  ...mapPinoBaseLogToBaseLog(obj),
  description: obj.description || '',
  eventCode: obj.eventCode || '',
  objectId: obj.objectId,
  securityEvent: obj.securityEvent || false,
  securityLevel: obj.securityLevel || '',
  targetId: obj.targetId || '',
  type: 'app-audit-events',
  userId: obj.userId || '',
  userIdHash: obj.userIdHash || '',
  userIp: obj.userIp || '',
  version: obj.version
})

const mapPinoLogToMiscLog = (obj: PinoMiscLog): MiscLog => ({
  ...mapPinoBaseLogToBaseLog(obj),
  exception: obj.exception,
  logLevel: obj.level || '',
  message: obj.message || '',
  meta: obj.meta,
  stackTrace: obj.stackTrace,
  type: 'app-misc',
  userIdHash: obj.userIdHash || '',
  version: obj.version
})

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const isPinoAccessLog = (obj: any): boolean =>
  obj.req &&
  obj.res &&
  (obj.message === 'request completed' || obj.message === 'request errored')

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const isPinoAppAuditLog = (obj: any): boolean =>
  obj.type && obj.type === 'app-audit-events'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const isPinoMiscLog = (obj: any): boolean => obj.type && obj.type === 'app-misc'

const stdoutStream = process.stdout

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const writeObjToStdoutAsJson = (obj: any): boolean =>
  stdoutStream.write(JSON.stringify(obj) + '\n')

const errorPayload = (e: unknown, message?: string): ErrorLog => {
  if (e instanceof Error) {
    return {
      exception: e.constructor.name,
      message: e.message || message || 'An error occurred',
      stackTrace: e.stack || ''
    }
  } else {
    return {
      exception: '',
      message: String(e),
      stackTrace: ''
    }
  }
}

const parser = (input: string) => {
  try {
    return JSON.parse(input)
  } catch (e) {
    if (e instanceof SyntaxError) {
      return
    }
    writeObjToStdoutAsJson(errorPayload(e, 'Parser failed unexpectedly'))
    throw e
  }
}

export const parserStream = split(parser)

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const transport = (obj: any, _: string, cb: through.TransformCallback) => {
  try {
    if (
      !isPinoAccessLog(obj) &&
      !isPinoAppAuditLog(obj) &&
      !isPinoMiscLog(obj)
    ) {
      writeObjToStdoutAsJson(obj)
      return cb()
    }

    if (isPinoAccessLog(obj) && isHealthCheckRequest(obj)) {
      return cb()
    }

    if (isPinoAccessLog(obj)) {
      writeObjToStdoutAsJson(mapPinoLogToAccessLog(obj))
    }

    if (isPinoAppAuditLog(obj)) {
      writeObjToStdoutAsJson(mapPinoLogToAuditLog(obj))
    }

    if (isPinoMiscLog(obj)) {
      writeObjToStdoutAsJson(mapPinoLogToMiscLog(obj))
    }
    cb()
  } catch (e) {
    writeObjToStdoutAsJson(errorPayload(e, 'Transport failed unexpectedly'))
    throw e
  }
}

export const transportStream = through.obj(transport)
