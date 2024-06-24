// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Request } from 'express'
import _ from 'lodash'
import { pino } from 'pino'
import { pinoHttp } from 'pino-http'
import queryString from 'query-string'

import { appBuild, appCommit, hostIp, prettyLogs, volttiEnv } from './config.js'
import { createSha256Hash } from './crypto.js'
import {
  LogFn,
  LogLevel,
  LogMeta,
  PinoReqSerializer,
  PinoRequest,
  PinoResponse,
  PinoResSerializer,
  UserPinoRequest
} from './types.js'

const BASE_LOGGER_OPTS: pino.LoggerOptions = {
  base: {
    appName: `evaka-api-gw`,
    appBuild: appBuild,
    appCommit: appCommit,
    env: volttiEnv,
    hostIp: hostIp
  },
  messageKey: 'message',
  timestamp: () => `,"@timestamp":"${new Date().toISOString()}"`,
  formatters: {
    level: (label) => ({ level: label })
  }
}

const APP_LOGGER_OPTS: pino.LoggerOptions = {
  name: 'app',
  base: {
    type: 'app-misc',
    version: 1
  }
}

const PRETTY_OPTS: Partial<pino.LoggerOptions> = {
  transport: {
    target: 'pino-pretty',
    options: {
      levelFirst: true,
      timestampKey: '@timestamp',
      messageKey: 'message'
    }
  }
}

const logger = pino(
  _.merge({}, BASE_LOGGER_OPTS, APP_LOGGER_OPTS, prettyLogs ? PRETTY_OPTS : {})
)

export const logError: LogFn = (msg, req?, meta?, err?) =>
  log('error', msg, req, meta, err)

export const logWarn: LogFn = (msg, req?, meta?) => log('warn', msg, req, meta)

export const logInfo: LogFn = (msg, req?, meta?) => log('info', msg, req, meta)

export const logDebug: LogFn = (msg, req?, meta?) =>
  log('debug', msg, req, meta)

function log(
  level: LogLevel,
  msg: string,
  req?: Request,
  meta?: LogMeta,
  err?: Error
): void {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const extraFields: Record<string, any> = {
    meta: { ...meta },
    spanId: req?.spanId,
    traceId: req?.traceId,
    userIdHash: req?.user?.id ? createSha256Hash(req.user.id) : ''
  }

  if (level === 'error') {
    extraFields.exception =
      err?.name ?? err?.constructor?.name ?? 'Unknown Error'
    extraFields.stackTrace = err?.stack
  }

  logger[level](extraFields, msg)
}

const AUDIT_LOG_LEVEL = 45

const AUDIT_LOGGER_OPTS: pino.LoggerOptions<'audit'> = {
  name: 'audit',
  messageKey: 'description',
  customLevels: {
    audit: AUDIT_LOG_LEVEL
  },
  base: {
    type: 'app-audit-events',
    targetId: '', // not used but needed in payload
    securityLevel: 'low',
    securityEvent: true,
    version: 1
  }
}

export const auditLogger = pino(
  _.merge(
    {},
    BASE_LOGGER_OPTS,
    AUDIT_LOGGER_OPTS,
    prettyLogs ? PRETTY_OPTS : {}
  )
)

export function logAuditEvent(
  eventCode: string,
  req: Request,
  description?: string
): void {
  const userId = req.user?.id || null
  auditLogger.audit(
    {
      eventCode,
      spanId: req?.spanId,
      traceId: req?.traceId,
      userId: userId,
      userIdHash: userId && createSha256Hash(userId),
      userIp: req.headers['x-real-ip'] as string
    },
    description
  )
}

const ACCESS_LOGGER_OPTS: pino.LoggerOptions = {
  name: 'access',
  messageKey: 'message',
  base: { type: 'app-requests-received', version: 1 }
}

const middlewareLogger = pino(_.merge({}, BASE_LOGGER_OPTS, ACCESS_LOGGER_OPTS))

function tracingReqSerializer(req: UserPinoRequest): UserPinoRequest {
  return {
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-unsafe-assignment
    spanId: req.raw.spanId,
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-unsafe-assignment
    traceId: req.raw.traceId,
    ...req
  }
}

/**
 * A request serializer for pino-http which enriches the req object with the user id hash
 */
export function userIdHashReqSerializer(req: UserPinoRequest): UserPinoRequest {
  // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-unsafe-assignment
  const userId = req.raw.user?.id || null
  // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
  req.userIdHash = userId != null ? createSha256Hash(userId) : ''
  return req
}

export const reqSerializer =
  (reqSerializers: PinoReqSerializer[]) => (req: PinoRequest) =>
    reqSerializers.reduce((acc, serializer) => serializer(acc), req)

export const resSerializer =
  (resSerializers: PinoResSerializer[]) => (res: PinoResponse) =>
    resSerializers.reduce((acc, serializer) => serializer(acc), res)

/**
 * A request serializer for pino-http which enriches the req object with the path and the query string
 */
export function queryStringReqSerializer(req: PinoRequest): PinoRequest {
  req.path = queryString.parseUrl(req.url).url
  const extractedQueryString = queryString.extract(req.url)
  req.queryString = extractedQueryString ? `?${extractedQueryString}` : ''
  return req
}

/**
 * A response serializer for pino-http which enriches the res object with contentLength
 */
export function contentLengthResSerializer(res: PinoResponse): PinoResponse {
  res.contentLength = Number(res.headers['content-length']) || -1
  return res
}

export const loggingMiddleware = pinoHttp({
  logger: middlewareLogger,
  serializers: {
    req: reqSerializer([
      userIdHashReqSerializer,
      queryStringReqSerializer,
      tracingReqSerializer
    ]),
    res: resSerializer([contentLengthResSerializer])
  }
})
