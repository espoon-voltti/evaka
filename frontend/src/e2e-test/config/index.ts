// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '../dev-api/types'

interface Config {
  env: string | undefined
  adminUrl: string
  employeeUrl: string
  devApiGwUrl: string
  enduserUrl: string
  supervisorAad: UUID
  supervisorExternalId: string
  adminAad: UUID
  adminExternalId: string
  mobileBaseUrl: string
  mobileUrl: string
}

const supervisorAad = '123dc92c-278b-4cea-9e54-2cc7e41555f3'
const adminAad = 'c50be1c1-304d-4d5a-86a0-1fad225c76cb'

const config: Config = {
  env: process.env.TEST_ENV,
  adminUrl: `${
    process.env.BASE_URL || 'http://localhost:9099'
  }/employee/applications`,
  employeeUrl: `${process.env.BASE_URL || 'http://localhost:9099'}/employee`,
  devApiGwUrl: `${
    process.env.BASE_URL || 'http://localhost:3020'
  }/api/internal/dev-api`,
  enduserUrl: process.env.BASE_URL || 'http://localhost:9099',
  supervisorAad,
  supervisorExternalId: `espoo-ad:${supervisorAad}`,
  adminAad,
  adminExternalId: `espoo-ad:${adminAad}`,
  mobileBaseUrl: `${process.env.BASE_URL || 'http://localhost:9099'}`,
  mobileUrl: `${
    process.env.BASE_URL || 'http://localhost:9099'
  }/employee/mobile`
}

export default config
