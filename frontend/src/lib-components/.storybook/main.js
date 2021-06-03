// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const TsconfigPathsPlugin = require('tsconfig-paths-webpack-plugin')
const path = require('path')

function resolveIcons() {
  switch (process.env.ICONS) {
    case 'pro':
      console.info('Using pro icons (forced)')
      return 'pro';
    case 'free':
      console.info('Using free icons (forced)')
      return 'free';
    case undefined:
      break;
    default:
      throw new Error(`Invalid environment variable ICONS=${process.env.ICONS}`)
  }
  try {
    require('@fortawesome/pro-light-svg-icons')
    require('@fortawesome/pro-regular-svg-icons')
    require('@fortawesome/pro-solid-svg-icons')
    console.info('Using pro icons (auto-detected)')
    return 'pro'
  } catch (e) {
    console.info('Using free icons (fallback)')
    return 'free'
  }
}

const icons = resolveIcons()

module.exports = {
  stories: ['../**/*.stories.tsx'],
  addons: ['@storybook/addon-actions'],
  webpackFinal: async (config) => {
    config.resolve.plugins.push(new TsconfigPathsPlugin({
      configFile: path.resolve(__dirname, '../tsconfig.json')
    }))
    config.resolve.alias = {
      ...config.resolve.alias,
      'Icons': icons === 'pro'
        ? path.resolve(__dirname, '../../lib-icons/pro-icons')
        : path.resolve(__dirname, '../../lib-icons/free-icons')
    }
    return config
  }
}
