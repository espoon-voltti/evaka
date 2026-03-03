// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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

export interface DisabledEndpoint {
  method: string
  pathPattern: string
}

export function parseDisabledEndpoint(
  entry: string
): DisabledEndpoint | null {
  const spaceIndex = entry.indexOf(' ')
  if (spaceIndex <= 0) return null
  const method = entry.substring(0, spaceIndex)
  const pathPattern = entry.substring(spaceIndex + 1)
  if (!pathPattern) return null
  return { method, pathPattern }
}

export function isEndpointDisabled(
  entries: string[],
  method: string,
  path: string
): boolean {
  for (const entry of entries) {
    const parsed = parseDisabledEndpoint(entry)
    if (!parsed) continue
    if (parsed.method !== '*' && parsed.method !== method) continue
    if (matchPath(parsed.pathPattern, path)) return true
  }
  return false
}
