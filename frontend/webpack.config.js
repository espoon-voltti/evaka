// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* eslint-disable no-console */

const fs = require('fs')
const path = require('path')

const sentryWebpackPlugin =
  require('@sentry/webpack-plugin').sentryWebpackPlugin
const HtmlWebpackPlugin = require('html-webpack-plugin')
const TsConfigPaths = require('tsconfig-paths-webpack-plugin')
const webpack = require('webpack')
const WebpackPwaManifest = require('webpack-pwa-manifest')

function resolveCustomizations() {
  const customizations = process.env.EVAKA_CUSTOMIZATIONS
  if (customizations) {
    const customizationsPath = path.resolve(
      __dirname,
      'src/lib-customizations',
      customizations
    )
    console.info(`Using customizations from ${customizationsPath}`)
    return customizations
  } else {
    return 'espoo'
  }
}

function resolveIcons() {
  switch (process.env.ICONS) {
    case 'pro':
      console.info('Using pro icons (forced)')
      return 'pro'
    case 'free':
      console.info('Using free icons (forced)')
      return 'free'
    case undefined:
      break
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

const customizationsModule = resolveCustomizations()
const icons = resolveIcons()

function baseConfig({ isDevelopment }, { name, publicPath, entry }) {
  const plugins = [
    new HtmlWebpackPlugin({
      template: path.resolve(__dirname, `src/${name}/index.html`),
      templateParameters: {
        appBody: fs.readFileSync(
          path.resolve(__dirname, 'src/body.html'),
          'utf-8'
        )
      }
    }),
    new webpack.NormalModuleReplacementPlugin(
      /@evaka\/customizations\/(.*)/,
      (resource) => {
        resource.request = resource.request.replace(
          /@evaka\/customizations/,
          `lib-customizations/${customizationsModule}`
        )
      }
    ),
    new webpack.DefinePlugin({
      // This matches APP_COMMIT in apigw
      'process.env.APP_COMMIT': `'${process.env.APP_COMMIT || 'UNDEFINED'}'`
    })
  ]

  // Only create a Sentry release when Sentry is enabled (i.e. production builds).
  // SentryWebpackPlugin automatically publishes source maps and creates a release.
  if (process.env.SENTRY_PUBLISH_ENABLED === 'true') {
    plugins.push(
      sentryWebpackPlugin({
        org: process.env.SENTRY_ORG || undefined,
        release: {
          name: process.env.APP_COMMIT,
          setCommits: {
            repo: 'espoon-voltti/evaka',
            commit: process.env.APP_COMMIT,
            auto: false
          }
        },
        project: `evaka-${name}`,
        sourcemaps: {
          assets: path.resolve(__dirname, `dist/bundle/${name}/**`)
        }
      })
    )
  }

  return {
    name,
    context: path.resolve(__dirname, `src/${name}`),
    mode: isDevelopment ? 'development' : 'production',
    devtool: isDevelopment ? 'cheap-module-source-map' : 'source-map',
    entry: entry ?? path.resolve(__dirname, `src/${name}/index.tsx`),
    output: {
      filename: isDevelopment ? '[name].js' : '[name].[contenthash].js',
      path: path.resolve(__dirname, `dist/bundle/${name}`),
      publicPath,
      assetModuleFilename: isDevelopment
        ? '[name][ext][query][fragment]'
        : '[name].[contenthash][ext][query][fragment]'
    },
    resolve: {
      extensions: ['.js', '.jsx', '.ts', '.tsx', '.json'],
      symlinks: false,
      alias: {
        Icons:
          icons === 'pro'
            ? path.resolve(__dirname, 'src/lib-icons/pro-icons')
            : path.resolve(__dirname, 'src/lib-icons/free-icons')
      },
      plugins: [
        new TsConfigPaths({
          configFile: path.resolve(__dirname, `src/${name}/tsconfig.json`)
        })
      ]
    },
    plugins,
    module: {
      rules: [
        // JS/TS/JSON
        {
          test: /\.(ts|tsx|json)$/,
          exclude: /[\\/]node_modules[\\/]/,
          use: [
            {
              loader: 'babel-loader',
              options: {
                presets: [
                  [
                    '@babel/preset-env',
                    {
                      corejs: '3.21',
                      useBuiltIns: 'entry'
                    }
                  ]
                ],
                plugins: [
                  [
                    'babel-plugin-styled-components',
                    {
                      displayName: false,
                      fileName: false,
                      pure: true
                    }
                  ]
                ]
              }
            },
            {
              loader: 'ts-loader',
              options: {
                projectReferences: true
              }
            }
          ]
        },
        // All CSS
        {
          test: /\.css$/,
          use: [
            'style-loader',
            {
              loader: 'css-loader',
              options: { importLoaders: 1 }
            },
            {
              loader: 'postcss-loader',
              options: {
                postcssOptions: {
                  config: path.resolve(__dirname, 'package.json')
                }
              }
            }
          ]
        },
        // Static files
        {
          test: /\.(woff|woff2|otf|ttf|eot|svg|png|gif|jpg|ico)$/,
          type: 'asset/resource'
        }
      ]
    },
    optimization: {
      splitChunks: {
        // Service workers must *not* use any kind of "chunk splitting" magic,
        // because a service worker is supposed to be contained in a single file
        chunks: (chunk) => chunk.name !== 'service-worker',
        cacheGroups: {
          defaultVendors: false,
          vendor: {
            test: /[\\/]node_modules[\\/]/,
            name: 'vendor',
            // See above
            chunks: (chunk) => chunk.name !== 'service-worker'
          }
        }
      }
    },
    stats: {
      children: false,
      colors: true,
      entrypoints: true,
      modules: false
    },
    performance: {
      hints: false
    }
  }
}

function citizen(flags) {
  return baseConfig(flags, {
    name: 'citizen-frontend',
    publicPath: '/'
  })
}

function employee(flags) {
  return baseConfig(flags, {
    name: 'employee-frontend',
    publicPath: '/employee/'
  })
}

function employeeMobile(flags) {
  const config = baseConfig(flags, {
    name: 'employee-mobile-frontend',
    publicPath: '/employee/mobile/',
    entry: {
      main: path.resolve(__dirname, 'src/employee-mobile-frontend/index.tsx'),
      'service-worker': {
        import: path.resolve(
          __dirname,
          'src/employee-mobile-frontend/service-worker.js'
        ),
        filename: '[name].js'
      }
    }
  })
  config.plugins.push(
    new WebpackPwaManifest({
      fingerprints: !flags.isDevelopment,
      ios: true,
      name: 'eVaka',
      display: 'standalone',
      start_url: '/employee/mobile',
      background_color: '#ffffff',
      theme_color: '#3273c9',
      icons: [
        {
          ios: true,
          src: path.resolve(
            __dirname,
            'src/employee-mobile-frontend/assets/evaka-180px.png'
          ),
          size: 180,
          type: 'image/png',
          purpose: 'maskable any'
        },
        {
          src: path.resolve(
            __dirname,
            'src/employee-mobile-frontend/assets/evaka-192px.png'
          ),
          size: 192,
          type: 'image/png',
          purpose: 'maskable any'
        },
        {
          src: path.resolve(
            __dirname,
            'src/employee-mobile-frontend/assets/evaka-512px.png'
          ),
          size: 512,
          type: 'image/png',
          purpose: 'maskable any'
        }
      ]
    })
  )
  return config
}

module.exports = (env, argv) => {
  const isDevelopment = !!(argv && argv['mode'] !== 'production')
  const flags = { isDevelopment }

  return [citizen(flags), employee(flags), employeeMobile(flags)]
}
