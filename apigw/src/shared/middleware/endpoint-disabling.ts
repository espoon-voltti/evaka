// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type express from 'express'

import { logWarn } from '../logging.ts'
import type { RedisClient } from '../redis-client.ts'

export function matchPath(pattern: string, path: string): boolean {
  const patternSegments = pattern.split('/')
  const pathSegments = path.split('/')
  return matchSegments(patternSegments, pathSegments, 0, 0)
}

function matchSegments(
  patternSegments: string[],
  pathSegments: string[],
  patternIndex: number,
  pathIndex: number
): boolean {
  while (patternIndex < patternSegments.length) {
    if (patternSegments[patternIndex] === '**') {
      for (let skip = 0; skip <= pathSegments.length - pathIndex; skip++) {
        if (
          matchSegments(
            patternSegments,
            pathSegments,
            patternIndex + 1,
            pathIndex + skip
          )
        )
          return true
      }
      return false
    }
    if (pathIndex >= pathSegments.length) return false
    if (patternSegments[patternIndex] === '*') {
      if (pathSegments[pathIndex] === '') return false
    } else if (patternSegments[patternIndex] !== pathSegments[pathIndex]) {
      return false
    }
    patternIndex++
    pathIndex++
  }
  return pathIndex === pathSegments.length
}

interface ParsedEntry {
  original: string
  method: string
  patternSegments: string[]
}

function parseEntries(rawEntries: string[]): ParsedEntry[] {
  const result: ParsedEntry[] = []
  for (const entry of rawEntries) {
    const spaceIndex = entry.indexOf(' ')
    if (spaceIndex <= 0) {
      logWarn(`Ignoring invalid disabled-endpoints entry: ${entry}`)
      continue
    }
    const method = entry.substring(0, spaceIndex).toUpperCase()
    const pathPattern = entry.substring(spaceIndex + 1)
    if (!pathPattern) {
      logWarn(`Ignoring invalid disabled-endpoints entry: ${entry}`)
      continue
    }
    result.push({
      original: entry,
      method,
      patternSegments: pathPattern.split('/')
    })
  }
  return result
}

const DISABLED_ENDPOINTS_KEY = 'disabled-endpoints'
const REFRESH_INTERVAL_MS = 10_000

export function createEndpointDisablingMiddleware(redisClient: RedisClient) {
  let parsedEntries: ParsedEntry[] = []

  async function refreshCache() {
    try {
      const raw = await redisClient.sMembers(DISABLED_ENDPOINTS_KEY)
      parsedEntries = parseEntries(raw)
    } catch (error) {
      logWarn(
        `Failed to refresh disabled endpoints cache: ${error instanceof Error ? error.message : String(error)}`
      )
    }
  }

  const interval = setInterval(() => void refreshCache(), REFRESH_INTERVAL_MS)
  interval.unref()

  // Load initial cache (non-blocking)
  void refreshCache()

  const middleware: express.RequestHandler = (req, res, next) => {
    const match = findMatch(parsedEntries, req.method, req.path)
    if (match) {
      res.status(503).json({
        errorCode: 'ENDPOINT_DISABLED',
        matchedPattern: match
      })
      return
    }
    next()
  }

  return {
    middleware,
    refreshCache,
    cleanup: () => clearInterval(interval)
  }
}

function findMatch(
  entries: ParsedEntry[],
  method: string,
  path: string
): string | null {
  const pathSegments = path.split('/')
  for (const entry of entries) {
    if (entry.method !== '*' && entry.method !== method) continue
    if (matchSegments(entry.patternSegments, pathSegments, 0, 0))
      return entry.original
  }
  return null
}
