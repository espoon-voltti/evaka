// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Express, Request } from 'express'
import { merge } from 'lodash'
import pino from 'pino'
import pinoHttp from 'pino-http'
import pinoPretty from 'pino-pretty'
import * as queryString from 'query-string'
import {
  appBuild,
  appCommit,
  gatewayRole,
  hostIp,
  prettyLogs,
  volttiEnv
} from './config'
import { createSha256Hash } from './crypto'
import {
  LogFn,
  LogLevel,
  LogMeta,
  PinoReqSerializer,
  PinoRequest,
  PinoResponse,
  PinoResSerializer,
  UserPinoRequest
} from './types'

const BASE_LOGGER_OPTS: pino.LoggerOptions = {
  base: {
    appName: `evaka-${gatewayRole || 'dev'}-gw`,
    appBuild: appBuild,
    appCommit: appCommit,
    env: volttiEnv,
    hostIp: hostIp
  },
  messageKey: 'message',
  timestamp: () => `,"@timestamp":"${new Date().toISOString()}"`,
  useLevelLabels: true
}

const APP_LOGGER_OPTS: pino.LoggerOptions = {
  name: 'app',
  base: {
    type: 'app-misc',
    version: 1
  }
}

const PRETTY_OPTS = {
  prettyPrint: {
    levelFirst: true
  },
  prettifier: pinoPretty
}

const logger = pino(
  merge({}, BASE_LOGGER_OPTS, APP_LOGGER_OPTS, prettyLogs ? PRETTY_OPTS : {})
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
  const extraFields: { [key: string]: any } = {
    meta: { ...meta },
    spanId: req?.spanId,
    traceId: req?.traceId,
    userIdHash: req?.user?.id ? createSha256Hash(req.user.id) : ''
  }

  if (level === 'error') {
    extraFields.exception = err?.constructor?.name || 'Unknown Error'
    extraFields.stackTrace = err?.stack
  }

  logger[level](extraFields, msg)
}

const AUDIT_LOG_LEVEL = 45

const AUDIT_LOGGER_OPTS: pino.LoggerOptions = {
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
  merge({}, BASE_LOGGER_OPTS, AUDIT_LOGGER_OPTS, prettyLogs ? PRETTY_OPTS : {})
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

const middlewareLogger = pino(merge({}, BASE_LOGGER_OPTS, ACCESS_LOGGER_OPTS))

export default function setupMiddleware(app: Express) {
  app.use(
    pinoHttp({
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
  )
}

function tracingReqSerializer(req: UserPinoRequest): UserPinoRequest {
  return {
    spanId: req.raw.spanId,
    traceId: req.raw.traceId,
    ...req
  }
}

/**
 * A request serializer for pino-http which enriches the req object with the user id hash
 */
export function userIdHashReqSerializer(req: UserPinoRequest): UserPinoRequest {
  const userId = req.raw.user?.id || null
  req.userIdHash = userId != null ? createSha256Hash(userId) : ''
  return req
}

export const reqSerializer = (reqSerializers: PinoReqSerializer[]) => (
  req: PinoRequest
) => reqSerializers.reduce((acc, serializer) => serializer(acc), req)

export const resSerializer = (resSerializers: PinoResSerializer[]) => (
  res: PinoResponse
) => resSerializers.reduce((acc, serializer) => serializer(acc), res)

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
