// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const path = require('path')

const defaults = {
  autorestart: false
}

module.exports = {
  apps: [{
    name: 'apigw',
    script: 'yarn && yarn clean && yarn dev',
    cwd: path.resolve(__dirname, '../apigw'),
    ...defaults
  }, {
    name: 'frontend',
    script: 'yarn && yarn clean && yarn dev',
    cwd: path.resolve(__dirname, '../frontend'),
    env: {
      'ICONS': process.env.ICONS
    },
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
