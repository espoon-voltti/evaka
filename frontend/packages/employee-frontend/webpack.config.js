// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const path = require('path')

const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const OptimizeCSSAssetsPlugin = require('optimize-css-assets-webpack-plugin')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const TsconfigPathsPlugin = require('tsconfig-paths-webpack-plugin')
const SentryWebpackPlugin = require('@sentry/webpack-plugin')

module.exports = function (env, argv) {
  const isDevelopment = argv && argv['mode'] !== 'production'

  const plugins = [
    new MiniCssExtractPlugin({
      filename: isDevelopment ? '[name].css' : '[name].[contenthash].css'
    }),
    new HtmlWebpackPlugin({
      template: 'src/index.html'
    })
  ]

  if (!isDevelopment) {
    plugins.push(new OptimizeCSSAssetsPlugin())
  }

  // Only create a Sentry release when Sentry is enabled (i.e. production builds).
  // SentryWebpackPlugin automatically published source maps and creates a release.
  if (process.env.SENTRY_PUBLISH_ENABLED) {
    plugins.push(
      new SentryWebpackPlugin({
        include: './dist',
        urlPrefix: '~/employee/',
        setCommits: {
          repo: 'espoon-voltti/evaka',
          auto: true
        }
      })
    )
  }

  return {
    mode: isDevelopment ? 'development' : 'production',
    devtool: isDevelopment ? 'cheap-module-source-map' : 'source-map',
    entry: path.resolve(__dirname, 'src/index.tsx'),
    output: {
      filename: isDevelopment ? '[name].js' : '[name].[contenthash].js',
      path: path.resolve(__dirname, 'dist'),
      publicPath: '/employee/'
    },
    resolve: {
      extensions: ['.js', '.jsx', '.ts', '.tsx', '.json'],
      plugins: [new TsconfigPathsPlugin()]
    },
    plugins,
    module: {
      rules: [
        // JS/TS/JSON
        {
          test: /\.(js|jsx|ts|tsx|json)$/,
          exclude: /node_modules/,
          use: {
            loader: 'ts-loader',
            options: {
              onlyCompileBundledFiles: true,
              compilerOptions: { noEmit: false }
            }
          }
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
                config: {
                  path: path.resolve(__dirname, 'package.json')
                }
              }
            }
          ]
        },
        // All SASS
        {
          test: /\.scss$/,
          use: [
            MiniCssExtractPlugin.loader,
            {
              loader: 'css-loader',
              options: { importLoaders: 2 }
            },
            {
              loader: 'postcss-loader',
              options: {
                config: {
                  path: path.resolve(__dirname, 'package.json')
                }
              }
            },
            'sass-loader'
          ]
        },
        // Static files
        {
          test: /\.(woff|woff2|otf|ttf|eot|svg|png|gif|jpg)$/,
          loader: 'file-loader',
          options: {
            name: isDevelopment ? '[name].[ext]' : '[name].[contenthash].[ext]'
          }
        }
      ]
    },
    optimization: {
      usedExports: true,
      splitChunks: {
        cacheGroups: {
          deps: {
            test: /\/node_modules\//,
            name: 'vendor',
            chunks: 'initial'
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
    devServer: {
      port: 9093,
      historyApiFallback: {
        index: '/employee/index.html'
      },
      proxy: {
        '/api/internal': {
          target: process.env.API_PROXY_URL || 'http://localhost:3020'
        }
      }
    }
  }
}
