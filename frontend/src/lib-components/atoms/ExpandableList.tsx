// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

interface Props {
  children: React.ReactNode[]
  rowsToOccupy: number
  i18n: { others: string }
}

export const ExpandableList = React.memo(function ExpandableList({
  children,
  rowsToOccupy,
  i18n
}: Props) {
  const [expanded, setExpanded] = useState(false)

  const needsExpansion = !expanded && children.length > rowsToOccupy
  const elementsToShow = needsExpansion
    ? children.slice(0, rowsToOccupy - 1)
    : children
  return (
    <div>
      {elementsToShow}
      {needsExpansion && (
        <div>
          <a
            onClick={(e) => {
              e.preventDefault()
              e.stopPropagation()
              setExpanded(true)
            }}
          >
            + {children.length - rowsToOccupy + 1} {i18n.others}
          </a>
        </div>
      )}
    </div>
  )
})
