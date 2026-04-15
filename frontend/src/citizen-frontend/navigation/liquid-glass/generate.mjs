// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* eslint-disable no-console */

import { writeFileSync } from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

import { chromium } from 'playwright'

// Regeneration script for the liquid-glass displacement + specular maps.
// Not invoked at build time. Run manually from the `frontend/` directory:
//
//   node src/citizen-frontend/navigation/liquid-glass/generate.mjs
//
// Requires playwright to be installed (it already is, for e2e tests).

const here = path.dirname(fileURLToPath(import.meta.url))

const config = {
  width: 360,
  height: 72,
  radius: 36,
  bezelWidth: 28,
  thickness: 140,
  n: 1.5,
  light: { x: -0.707, y: -0.707 },
  specularPower: 2.0
}

const displacementOut = path.join(here, 'displacement.png')
const specularOut = path.join(here, 'specular.png')
const metaOut = path.join(here, 'meta.json')

const browser = await chromium.launch()
const page = await browser.newPage()

const result = await page.evaluate(async (cfg) => {
  const {
    width,
    height,
    radius,
    bezelWidth,
    thickness,
    n,
    light,
    specularPower
  } = cfg

  const samples = 256
  const magLut = new Array(samples)
  const sinThetaLut = new Array(samples)
  let maxMag = 0
  for (let i = 0; i < samples; i++) {
    const u = i / (samples - 1)
    const h = thickness * Math.sqrt(Math.max(0, 2 * u - u * u))
    const denom = Math.sqrt(Math.max(1e-10, 2 * u - u * u))
    const slope = (1 - u) / denom
    const theta1 = Math.atan(slope)
    const sinTheta2 = Math.min(1, Math.sin(theta1) / n)
    const theta2 = Math.asin(sinTheta2)
    const mag = h * Math.tan(theta1 - theta2)
    magLut[i] = mag
    sinThetaLut[i] = Math.sin(theta1)
    if (mag > maxMag) maxMag = mag
  }

  const sdf = (px, py) => {
    const qx = Math.abs(px - width / 2) - (width / 2 - radius)
    const qy = Math.abs(py - height / 2) - (height / 2 - radius)
    const ux = Math.max(qx, 0)
    const uy = Math.max(qy, 0)
    return Math.sqrt(ux * ux + uy * uy) + Math.min(Math.max(qx, qy), 0) - radius
  }
  const sdfGrad = (px, py) => {
    const e = 0.5
    const dx = sdf(px + e, py) - sdf(px - e, py)
    const dy = sdf(px, py + e) - sdf(px, py - e)
    const len = Math.hypot(dx, dy)
    if (len < 1e-6) return [0, 0]
    return [dx / len, dy / len]
  }

  const mkCanvas = () => {
    // eslint-disable-next-line no-undef
    const c = document.createElement('canvas')
    c.width = width
    c.height = height
    return c
  }

  const dispCanvas = mkCanvas()
  const specCanvas = mkCanvas()
  const dispCtx = dispCanvas.getContext('2d')
  const specCtx = specCanvas.getContext('2d')
  const dispImg = dispCtx.createImageData(width, height)
  const specImg = specCtx.createImageData(width, height)

  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const px = x + 0.5
      const py = y + 0.5
      const d = sdf(px, py)
      let r = 128
      let g = 128
      let specA = 0
      if (d < 0 && -d < bezelWidth) {
        const u = -d / bezelWidth
        const idxF = u * (samples - 1)
        const i0 = Math.floor(idxF)
        const i1 = Math.min(samples - 1, i0 + 1)
        const t = idxF - i0
        const mag = magLut[i0] * (1 - t) + magLut[i1] * t
        const sinTheta = sinThetaLut[i0] * (1 - t) + sinThetaLut[i1] * t
        const [gx, gy] = sdfGrad(px, py)
        const inX = -gx
        const inY = -gy
        const dispX = inX * mag
        const dispY = inY * mag
        const nx = dispX / maxMag
        const ny = dispY / maxMag
        r = Math.max(0, Math.min(255, Math.round(128 + nx * 127)))
        g = Math.max(0, Math.min(255, Math.round(128 + ny * 127)))

        const dot = inX * light.x + inY * light.y
        const rim = Math.max(0, dot)
        const intensity = Math.pow(rim * sinTheta, specularPower)
        specA = Math.max(0, Math.min(255, Math.round(intensity * 255)))
      }
      const idx = (y * width + x) * 4
      dispImg.data[idx] = r
      dispImg.data[idx + 1] = g
      dispImg.data[idx + 2] = 128
      dispImg.data[idx + 3] = 255
      specImg.data[idx] = 255
      specImg.data[idx + 1] = 255
      specImg.data[idx + 2] = 255
      specImg.data[idx + 3] = specA
    }
  }
  dispCtx.putImageData(dispImg, 0, 0)
  specCtx.putImageData(specImg, 0, 0)
  return {
    dispDataUrl: dispCanvas.toDataURL('image/png'),
    specDataUrl: specCanvas.toDataURL('image/png'),
    maxMag
  }
}, config)

const writePng = (outPath, dataUrl) => {
  const base64 = dataUrl.split(',')[1]
  const buf = Buffer.from(base64, 'base64')
  writeFileSync(outPath, buf)
  return buf.length
}
const dispBytes = writePng(displacementOut, result.dispDataUrl)
const specBytes = writePng(specularOut, result.specDataUrl)

writeFileSync(
  metaOut,
  JSON.stringify({ ...config, maxDisplacement: result.maxMag }, null, 2)
)

console.log(`wrote ${displacementOut} (${dispBytes} bytes)`)
console.log(`wrote ${specularOut} (${specBytes} bytes)`)
console.log(
  `maxDisplacement = ${result.maxMag.toFixed(2)} px  (use as <feDisplacementMap scale>)`
)

await browser.close()
