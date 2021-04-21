// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNodeArray, useState } from 'react'

interface Props {
  children: ReactNodeArray
  show: number
  i18n: { others: string }
}

export const ExpandableList = React.memo(function ExpandableList({
  children,
  show,
  i18n
}: Props) {
  const [expanded, setExpanded] = useState(false)

  const needsExpansion = !expanded && children.length > show
  const elementsToShow = needsExpansion ? children.slice(0, show) : children
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
            + {children.length - show} {i18n.others}
          </a>
        </div>
      )}
    </div>
  )
})
