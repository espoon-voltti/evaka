// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const path = require('path')
const TsconfigPathsPlugin = require('tsconfig-paths-webpack-plugin')
const DefinePlugin = require('webpack').DefinePlugin

module.exports = ({ config }) => {
  config.module.rules.push({
    test: /\.(ts|tsx)$/,
    loader: require.resolve('babel-loader'),
    options: {
      presets: [['react-app', { flow: false, typescript: true }]],
    },
  })
  config.module.rules.push({
    test: /\.scss$/,
    loaders: ['style-loader', 'css-loader', 'sass-loader'],
  })
  config.resolve.extensions.push('.ts', '.tsx')
  config.resolve.plugins = [new TsconfigPathsPlugin()]
  config.resolve.alias = {
    'Icons': process.env.ICONS === 'pro'
      ? path.resolve(__dirname, '../../lib-icons/pro-icons')
      : path.resolve(__dirname, '../../lib-icons/free-icons')
  }
  return config
}
