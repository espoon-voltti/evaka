// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Request } from 'express'
import type {
  SerializedRequest,
  SerializedResponse
} from 'pino-std-serializers'

import type { EvakaSessionUser } from './auth/index.ts'

export interface PinoRequest
  extends Omit<
    SerializedRequest,
    'id' | 'headers' | 'method' | 'raw' | 'remoteAddress' | 'remotePort'
  > {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  raw?: any
  // Custom enriched properties
  path?: string
  queryString?: string
  userIdHash?: string
}

export interface PinoResponse
  extends Omit<SerializedResponse, 'raw' | 'statusCode'> {
  // Custom enriched properties
  contentLength?: number
}

export type PinoReqSerializer = (req: PinoRequest) => PinoRequest
export type PinoResSerializer = (res: PinoResponse) => PinoResponse

export interface UserPinoRequest extends PinoRequest {
  spanId?: string
  traceId?: string
  user?: EvakaSessionUser
}

export type LogLevel = 'error' | 'warn' | 'info' | 'debug'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type LogMeta = Record<string, any>

export type LogFn = (
  msg: string,
  req?: Request,
  meta?: LogMeta,
  err?: Error
) => void
