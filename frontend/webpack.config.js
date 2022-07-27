// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const fs = require('fs')
const path = require('path')

const SentryWebpackPlugin = require('@sentry/webpack-plugin')
const PreloadWebpackPlugin = require('@vue/preload-webpack-plugin')
const ForkTsCheckerWebpackPlugin = require('fork-ts-checker-webpack-plugin')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const TsConfigPaths = require('tsconfig-paths-webpack-plugin')
const webpack = require('webpack')
const WebpackPwaManifest = require('webpack-pwa-manifest')

const { resolveCustomizations, resolveIcons } =
  require('./build-tools/build-utils')(true)

const customizationsModule = resolveCustomizations()
const icons = resolveIcons()

function baseConfig(
  { isDevelopment, isDevServer },
  { name, publicPath, prefetchChunks }
) {
  const plugins = [
    new HtmlWebpackPlugin({
      template: path.resolve(__dirname, `src/${name}/index.html`),
      templateParameters: {
        appBody: fs.readFileSync(
          path.resolve(__dirname, 'src/body.html'),
          'utf-8'
        )
      },
      minify: {
        minifyJS: true,
        collapseBooleanAttributes: true,
        collapseWhitespace: true,
        minifyCSS: true,
        processConditionalComments: true
      }
    }),
    new webpack.NormalModuleReplacementPlugin(
      /@evaka\/customizations\/(.*)/,
      (resource) => {
        resource.request = resource.request.replace(
          /@evaka\/customizations/,
          customizationsModule
        )
      }
    ),
    new webpack.DefinePlugin({
      // This matches APP_COMMIT in apigw
      'process.env.APP_COMMIT': `'${process.env.APP_COMMIT || 'UNDEFINED'}'`
    }),
    new MiniCssExtractPlugin({
      filename: isDevelopment ? '[name].css' : '[name].[contenthash].css',
      chunkFilename: isDevelopment ? '[id].css' : '[id].[contenthash].css'
    })
  ]

  if (prefetchChunks) {
    plugins.push(
      new PreloadWebpackPlugin({
        rel: 'prefetch',
        include: {
          chunks: prefetchChunks
        },
        as(entry) {
          if (/\.css$/.test(entry)) return 'style'
          if (/\.(woff|woff2|otf|ttf|eot|)$/.test(entry)) return 'font'
          if (/\.(svg|png|gif|jpe?g|ico)$/.test(entry)) return 'image'
          return 'script'
        }
      })
    )
  }

  if (isDevServer) {
    plugins.push(new ForkTsCheckerWebpackPlugin())
  }

  // Only create a Sentry release when Sentry is enabled (i.e. production builds).
  // SentryWebpackPlugin automatically publishes source maps and creates a release.
  if (process.env.SENTRY_PUBLISH_ENABLED === 'true') {
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
                      displayName: isDevServer,
                      fileName: false,
                      pure: true,
                      ssr: true
                    }
                  ]
                ]
              }
            },
            {
              loader: 'ts-loader',
              options: {
                onlyCompileBundledFiles: true,
                projectReferences: true,
                transpileOnly: isDevServer
              }
            }
          ]
        },
        // All CSS
        {
          test: /\.css$/,
          use: [
            MiniCssExtractPlugin.loader,
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
        chunks: 'all'
      },
      usedExports: true
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
    publicPath: '/',
    prefetchChunks: ['LoginPage', 'CalendarPage', 'Applying-MapView']
  })
}

function employee(flags) {
  return baseConfig(flags, {
    name: 'employee-frontend',
    publicPath: '/employee/',
    prefetchChunks: ['LoginPage']
  })
}

function employeeMobile(flags) {
  const config = baseConfig(flags, {
    name: 'employee-mobile-frontend',
    publicPath: '/employee/mobile/',
    prefetchChunks: ['MobileLander']
  })
  config.plugins.push(
    new WebpackPwaManifest({
      fingerprints: !flags.isDevelopment,
      ios: true,
      name: 'eVaka',
      display: 'fullscreen',
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
  const isDevServer = !!(env && env['DEV_SERVER'])
  const flags = { isDevServer, isDevelopment }

  // TODO: remaining prefetches, prioritization?
  return [citizen(flags), employee(flags), employeeMobile(flags)]
}
