// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

interface Config {
  env: string | undefined
  adminUrl: string
  employeeUrl: string
  devApiGwUrl: string
  enduserUrl: string
  supervisorAad: string
  adminAad: string
  mobileUrl: string
}

const config: Config = {
  env: process.env.TEST_ENV,
  adminUrl: `${
    process.env.BASE_URL || 'http://localhost:9093'
  }/employee/applications`,
  employeeUrl: `${process.env.BASE_URL || 'http://localhost:9093'}/employee`,
  devApiGwUrl: `${
    process.env.BASE_URL || 'http://localhost:3020'
  }/api/internal/dev-api`,
  enduserUrl: process.env.BASE_URL || 'http://localhost:9091',
  supervisorAad: '123dc92c-278b-4cea-9e54-2cc7e41555f3',
  adminAad: 'c50be1c1-304d-4d5a-86a0-1fad225c76cb',
  mobileUrl: `${
    process.env.BASE_URL || 'http://localhost:9093'
  }/employee/mobile`
}

export default config
