// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { inflateSync } from 'node:zlib'

export interface DecodedPng {
  width: number
  height: number
  channels: number
  pixels: Uint8Array
}

const paeth = (a: number, b: number, c: number) => {
  const p = a + b - c
  const pa = Math.abs(p - a)
  const pb = Math.abs(p - b)
  const pc = Math.abs(p - c)
  return pa <= pb && pa <= pc ? a : pb <= pc ? b : c
}

/** Decodes the 8-bit RGB(A) non-interlaced PNGs that simulator screenshots are */
export function decodePng(data: Buffer): DecodedPng {
  let offset = 8
  let width = 0
  let height = 0
  let channels = 0
  const idat: Buffer[] = []
  while (offset < data.length) {
    const length = data.readUInt32BE(offset)
    const type = data.toString('ascii', offset + 4, offset + 8)
    const chunk = data.subarray(offset + 8, offset + 8 + length)
    if (type === 'IHDR') {
      width = chunk.readUInt32BE(0)
      height = chunk.readUInt32BE(4)
      const bitDepth = chunk[8]
      const colorType = chunk[9]
      const interlace = chunk[12]
      if (bitDepth !== 8 || interlace !== 0 || ![2, 6].includes(colorType)) {
        throw new Error(
          `Unsupported PNG: bit depth ${bitDepth}, color type ${colorType}, interlace ${interlace}`
        )
      }
      channels = colorType === 6 ? 4 : 3
    } else if (type === 'IDAT') {
      idat.push(chunk)
    } else if (type === 'IEND') {
      break
    }
    offset += 12 + length
  }
  const raw = inflateSync(Buffer.concat(idat))
  const stride = width * channels
  const pixels = new Uint8Array(stride * height)
  for (let y = 0; y < height; y++) {
    const filter = raw[y * (stride + 1)]
    if (filter > 4) throw new Error(`Unsupported PNG filter ${filter}`)
    const line = raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1))
    const row = y * stride
    const prevRow = row - stride
    for (let x = 0; x < stride; x++) {
      const left = x >= channels ? pixels[row + x - channels] : 0
      const up = y > 0 ? pixels[prevRow + x] : 0
      const upLeft = y > 0 && x >= channels ? pixels[prevRow + x - channels] : 0
      const predictor =
        filter === 0
          ? 0
          : filter === 1
            ? left
            : filter === 2
              ? up
              : filter === 3
                ? (left + up) >> 1
                : paeth(left, up, upLeft)
      pixels[row + x] = (line[x] + predictor) & 0xff
    }
  }
  return { width, height, channels, pixels }
}
