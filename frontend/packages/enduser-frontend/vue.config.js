// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

'use strict'

const path = require('path')

const SentryWebpackPlugin = require('@sentry/webpack-plugin')

const webpackPlugins = []

// Only create a Sentry release when Sentry is enabled (i.e. production builds).
// SentryWebpackPlugin automatically published source maps and creates a release.
if (process.env.SENTRY_PUBLISH_ENABLED) {
  webpackPlugins.push(new SentryWebpackPlugin({
    include: './dist',
    setCommits: {
      repo: 'espoon-voltti/evaka',
      auto: true
    }
  }))
}

module.exports = {
  devServer: {
    port: 9091,
    proxy: {
      '^/api/': {
        target: process.env.API_PROXY_URL || 'http://localhost:3010',
        ws: true
      }
    },
    headers: {
      'Strict-Transport-Security': 'max-age=31536000; includeSubdomains; preload',
      'X-Frame-Options': 'deny',
      'X-Content-Type-Options': 'nosniff',
      'X-XSS-Protection': '1',
      'Content-Security-Policy': "form-action 'self'; frame-ancestors 'none'; default-src 'self'; script-src 'self' 'unsafe-eval' maps.googleapis.com; font-src 'self' fonts.gstatic.com; style-src 'self' 'unsafe-inline' fonts.googleapis.com; img-src 'self' data: maps.gstatic.com maps.googleapis.com; connect-src *"
    },
    watchOptions: {
      poll: 1000,
      ignored: /node_modules/
    }
  },
  publicPath: '/',
  css: {
    loaderOptions: {
      postcss: {
        plugins: [
          require('autoprefixer')()
        ]
      },
      sass: {
        prependData: `
          @import "@/assets/scss/_variables.scss";
          @import "@/assets/scss/transitions.scss";
        `
      }
    }
  },
  configureWebpack: {
    plugins: webpackPlugins
  },
  chainWebpack: config => {
    config.module
      .rule('images')
      .use('url-loader')
      .loader('url-loader')
      .tap(options => {
        return {
          ...options,
          limit: 100 // Only base64 encode & inline the very smallest of images (icons etc.)
        }
      })
  },
  parallel: false
}
