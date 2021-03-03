// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const path = require('path')

module.exports = {
  stories: ['../src/**/*.stories.tsx'],
  addons: ['@storybook/addon-actions'],
  webpackFinal: async (config) => {
    config.resolve.alias = {
      ...config.resolve.alias,
      'Icons': process.env.ICONS === 'pro'
        ? path.resolve(__dirname, '../../lib-icons/pro-icons')
        : path.resolve(__dirname, '../../lib-icons/free-icons')
    }
    return config
  }
}
