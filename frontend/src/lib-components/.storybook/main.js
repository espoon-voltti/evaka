// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const TsconfigPathsPlugin = require('tsconfig-paths-webpack-plugin')
const path = require('path')

module.exports = {
  stories: ['../**/*.stories.tsx'],
  addons: ['@storybook/addon-actions'],
  webpackFinal: async (config) => {
    config.resolve.plugins.push(new TsconfigPathsPlugin({
      configFile: path.resolve(__dirname, '../tsconfig.json')
    }))
    config.resolve.alias = {
      ...config.resolve.alias,
      'Icons': process.env.ICONS === 'pro'
        ? path.resolve(__dirname, '../../lib-icons/pro-icons')
        : path.resolve(__dirname, '../../lib-icons/free-icons')
    }
    return config
  }
}
