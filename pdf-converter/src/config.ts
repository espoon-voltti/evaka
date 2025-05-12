// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

type EnvVariables = typeof envVariables
const envVariables = {
  HTTP_PORT: 9091
}

type Parser<T> = (value: string) => T

const parseInteger: Parser<number> = (value) => {
  const result = Number.parseInt(value, 10)
  if (Number.isNaN(result)) throw new Error('Invalid integer')
  return result
}

function parseEnv<K extends keyof EnvVariables>(
  key: K,
  f: Parser<NonNullable<EnvVariables[K]>>
): NonNullable<EnvVariables[K]> {
  const value = process.env[key]?.trim()
  try {
    return value == null || value === '' ? envVariables[key] : f(value)
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    throw new Error(`${message}: ${key}=${value}`)
  }
}

function configFromEnv(): EnvVariables {
  return {
    HTTP_PORT: parseEnv('HTTP_PORT', parseInteger)
  }
}

export const config = configFromEnv() 