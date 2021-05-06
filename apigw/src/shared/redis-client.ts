// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import redis from 'redis'
import {
  redisHost,
  redisPort,
  redisDisableSecurity,
  redisTlsServerName,
  redisPassword
} from './config'
import { logError } from './logging'

/**
 * Create an instance of a Redis client targeting a shared Redis instance.
 * It is recommended to create only a single instance of this client per app.
 */
export function createRedisClient() {
  const redisClient = redis.createClient({
    host: redisHost,
    port: redisPort,
    ...(!redisDisableSecurity && {
      tls: { servername: redisTlsServerName },
      password: redisPassword
    })
  })

  redisClient.on('error', (err) =>
    logError('Redis error', undefined, undefined, err)
  )

  // don't prevent the app from exiting if a redis connection is alive
  redisClient.unref()
  return redisClient
}
