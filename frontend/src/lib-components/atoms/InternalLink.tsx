// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import { Link } from 'react-router-dom'
import { faLink } from 'lib-icons'
import { Bold } from '../typography'

interface Props {
  newTab: boolean
  to: string
  text: string | JSX.Element
}

export const InternalLink = React.memo(function InternalLink({
  newTab = false,
  text,
  to
}: Props) {
  return (
    <Link to={to} target={newTab ? '_blank' : undefined}>
      <Bold>
        {text} <FontAwesomeIcon icon={faLink} />
      </Bold>
    </Link>
  )
})
