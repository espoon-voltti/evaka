// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const booleans: Record<string, boolean> = {
  1: true,
  0: false,
  true: true,
  false: false
}

function parseBoolean(value: string): boolean {
  if (value in booleans) return booleans[value]
  throw new Error('Invalid boolean')
}

function parseEnum<T extends string>(
  variants: readonly T[]
): (value: string) => T {
  return (value) => {
    for (const variant of variants) {
      if (value === variant) return variant
    }
    throw new Error(`Invalid enum (expected one of ${variants.toString()})`)
  }
}

function env<T>(key: string, parser: (value: string) => T): T | undefined {
  const value = process.env[key]
  if (value === undefined || value === '') return undefined
  try {
    return parser(value)
  } catch (err) {
    if (err instanceof Error) {
      throw new Error(`${err.message}: ${key}=${value}`)
    } else {
      throw new Error(`${String(err)}: ${key}=${value}`)
    }
  }
}

const frontendPort =
  env('EVAKA_FRONTEND_PORT', (value) => parseInt(value, 10)) ?? 9099
const idpPort = env('EVAKA_IDP_PORT', (value) => parseInt(value, 10)) ?? 9090

const baseUrl =
  env('BASE_URL', (url) => url) ?? `http://localhost:${frontendPort}`
const devApiGwUrl = env('DEV_API_URL', (url) => url) ?? `${baseUrl}/api/dev-api`
export const dummyIdpUrl =
  env('DUMMY_IDP_URL', (url) => url) ?? `http://localhost:${idpPort}`

const supervisorAad = '123dc92c-278b-4cea-9e54-2cc7e41555f3'
const adminAad = 'c50be1c1-304d-4d5a-86a0-1fad225c76cb'
const serviceWorkerAad = '00000000-0000-0000-0001-000000000000'
const financeAdminAad = '00000000-0000-0000-0002-000000000000'
const directorAad = '00000000-0000-0000-0003-000000000000'
const unitSupervisorAad = '00000000-0000-0000-0004-000000000000'
const staffAad = '00000000-0000-0000-0005-000000000000'
const specialEducationTeacher = '00000000-0000-0000-0006-000000000000'
const reportViewerAad = '00000000-0000-0000-0007-000000000000'

const config = {
  playwright: {
    ci: env('CI', parseBoolean) ?? false,
    headless: env('HEADLESS', parseBoolean) ?? false,
    browser:
      env('BROWSER', parseEnum(['chromium', 'firefox', 'webkit'] as const)) ??
      'chromium'
  },
  apiUrl: `${baseUrl}/api`,
  adminUrl: `${baseUrl}/employee/applications`,
  employeeUrl: `${baseUrl}/employee`,
  employeeLoginUrl: `${baseUrl}/employee/login`,
  devApiGwUrl,
  enduserUrl: baseUrl,
  mobileBaseUrl: baseUrl,
  mobileUrl: `${baseUrl}/employee/mobile`,
  enduserMessagesUrl: `${baseUrl}/messages`,
  enduserLoginUrl: `${baseUrl}/login`,

  // TODO: Remove these
  supervisorAad,
  supervisorExternalId: `espoo-ad:${supervisorAad}`,
  adminAad,
  adminExternalId: `espoo-ad:${adminAad}`,
  serviceWorkerAad,
  financeAdminAad,
  directorAad,
  reportViewerAad,
  unitSupervisorAad,
  staffAad,
  specialEducationTeacher
}

export default config
