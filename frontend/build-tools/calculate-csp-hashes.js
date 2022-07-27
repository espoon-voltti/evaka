// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const crypto = require('crypto')
const fs = require('fs/promises')
const { join } = require('path')

const cheerio = require('cheerio')

;(async () => {
  const bundleDir = join(__dirname, '../dist/bundle')
  const bundles = await fs.readdir(bundleDir)

  const hashes = (
    await Promise.all(
      bundles.map(async (bundle) => {
        const indexHtml = await fs.readFile(
          join(bundleDir, bundle, 'index.html')
        )
        const $ = cheerio.load(indexHtml)
        return $('script:not([src])')
          .map(
            (_, script) =>
              `'sha256-${crypto
                .createHash('sha256')
                .update($(script).html())
                .digest('base64')}'`
          )
          .toArray()
      })
    )
  ).flat()

  console.log([...new Set(hashes)].join(' '))
})()
