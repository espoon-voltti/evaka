// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

export function formatParagraphs(text?: string) {
  return (
    text &&
    text.split(/\n/).map((line, index) => {
      if (line.trim().length === 0) return <br key={index} />
      else return <p key={index}>{line}</p>
    })
  )
}
