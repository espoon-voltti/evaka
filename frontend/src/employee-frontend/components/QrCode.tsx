// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import styled from 'styled-components'
import * as uqr from 'uqr'

export function qrCodeSvg(data: string): { size: number; path: string } {
  const qr = uqr.encode(data)
  const path = qr.data
    .flatMap((row, y) => row.flatMap((cell, x) => (cell ? [{ x, y }] : [])))
    .map(
      ({ x, y }) =>
        // M -> move to coordinates
        // h 1 v 1 h -1 z -> draw a square
        `M${x} ${y}h1v1h-1z`
    )
    .join('')

  return { size: qr.size, path }
}

function QrCode({
  url,
  className
}: {
  url: string
  className?: string | undefined
}) {
  const { size, path } = useMemo(() => qrCodeSvg(url), [url])
  return (
    <div className={className}>
      <svg
        viewBox={`0 0 ${size} ${size}`}
        shapeRendering="crispEdges"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path d={path} fill="black" />
      </svg>
    </div>
  )
}

export default styled(QrCode)`
  display: flex;
  justify-content: center;

  svg {
    width: 75%;
  }
`
