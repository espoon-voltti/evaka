// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const path = require('path')

const defaults = {
  autorestart: false
}

const useProIcons = process.env.ICONS === 'pro'

module.exports = {
  apps: [{
    name: 'apigw',
    script: 'yarn clean && yarn && yarn dev',
    cwd: path.resolve(__dirname, '../apigw'),
    ...defaults
  }, {
    name: 'citizen',
    script: useProIcons ? 'yarn install --mutex network && ICONS=pro yarn dev' : 'yarn install --ignore-optional --mutex network && yarn dev',
    cwd: path.resolve(__dirname, '../frontend/packages/citizen-frontend'),
    ...defaults
  }, {
    name: 'enduser',
    script: useProIcons ? 'yarn install --mutex network && ICONS=pro yarn dev' : 'yarn install --ignore-optional --mutex network && yarn dev',
    cwd: path.resolve(__dirname, '../frontend/packages/enduser-frontend'),
    ...defaults
  }, {
    name: 'employee',
    script: useProIcons ? 'yarn install --mutex network && ICONS=pro yarn dev' : 'yarn install --ignore-optional --mutex network && yarn dev',
    cwd: path.resolve(__dirname, '../frontend/packages/employee-frontend'),
    ...defaults
  }, {
    name: 'service',
    script: `${__dirname}/run-after-db.sh`,
    args: './gradlew --no-daemon bootRun',
    cwd: path.resolve(__dirname, '../service'),
    ...defaults
  },
    /*{
    name: 'message-srv',
    script: `${__dirname}/run-after-db.sh`,
    args: './gradlew --no-daemon bootRun',
    cwd: path.resolve(__dirname, '../message-service'),
    ...defaults
  }*/
  ],
}
