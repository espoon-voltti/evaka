// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const webpack = require('webpack')
const path = require('path')

const HtmlWebpackPlugin = require('html-webpack-plugin')
const SentryWebpackPlugin = require('@sentry/webpack-plugin')
const WebpackPwaManifest = require('webpack-pwa-manifest')
const TsConfigPaths = require('tsconfig-paths-webpack-plugin')
const ForkTsCheckerWebpackPlugin = require('fork-ts-checker-webpack-plugin')

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

const customizationsModule = resolveCustomizations()

function baseConfig({ isDevelopment, isDevServer }, { name, publicPath }) {
  const plugins = [
    new HtmlWebpackPlugin({
      template: path.resolve(__dirname, `src/${name}/index.html`)
    }),
    new webpack.NormalModuleReplacementPlugin(
      /@evaka\/customizations\/(.*)/,
      (resource) => {
        resource.request = resource.request.replace(
          /@evaka\/customizations/,
          `lib-customizations/${customizationsModule}`
        )
      }
    )
  ]

  if (isDevServer) {
    plugins.push(new ForkTsCheckerWebpackPlugin())
  }

  // Only create a Sentry release when Sentry is enabled (i.e. production builds).
  // SentryWebpackPlugin automatically publishes source maps and creates a release.
  if (process.env.SENTRY_PUBLISH_ENABLED) {
    plugins.push(
      new SentryWebpackPlugin({
        project: `evaka-${name}`,
        include: path.resolve(__dirname, `dist/bundle/${name}`),
        urlPrefix: `~${publicPath}`,
        setCommits: {
          repo: 'espoon-voltti/evaka',
          auto: true
        }
      })
    )
  }

  return {
    name,
    context: path.resolve(__dirname, `src/${name}`),
    mode: isDevelopment ? 'development' : 'production',
    devtool: isDevelopment ? 'cheap-module-source-map' : 'source-map',
    entry: path.resolve(__dirname, `src/${name}/index.tsx`),
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
          process.env.ICONS === 'pro'
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
          use: {
            loader: 'ts-loader',
            options: {
              onlyCompileBundledFiles: true,
              projectReferences: true,
              transpileOnly: isDevServer
            }
          }
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
          test: /\.(woff|woff2|otf|ttf|eot|svg|png|gif|jpg)$/,
          type: 'asset/resource'
        }
      ]
    },
    optimization: {
      splitChunks: {
        chunks: 'all',
        cacheGroups: {
          defaultVendors: false,
          vendor: {
            test: /[\\/]node_modules[\\/]/,
            name: 'vendor',
            chunks: 'all'
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
    },
    cache: isDevServer ? { type: 'filesystem' } : false
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
    publicPath: '/employee/mobile/'
  })
  config.plugins.push(
    new WebpackPwaManifest({
      fingerprints: !flags.isDevelopment,

      name: 'eVaka',
      display: 'fullscreen',
      start_url: '/employee/mobile',
      background_color: '#ffffff',
      theme_color: '#3273c9',
      icons: [
        {
          src: path.resolve(
            __dirname,
            'src/employee-mobile-frontend/evaka.png'
          ),
          sizes: '160x160',
          type: 'image/png',
          purpose: 'maskable'
        }
      ]
    })
  )
  return config
}

module.exports = (env, argv) => {
  const isDevelopment = !!(argv && argv['mode'] !== 'production')
  const isDevServer = !!(env && env['DEV_SERVER'])
  const flags = { isDevServer, isDevelopment }

  return [citizen(flags), employee(flags), employeeMobile(flags)]
}
