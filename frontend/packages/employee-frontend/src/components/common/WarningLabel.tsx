// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons'

import '~components/common/WarningLabel.scss'

interface Props {
  text: string
  dataQa?: string
}

function WarningLabel({ text, dataQa }: Props) {
  return (
    <div className={`warning-label`} data-qa={dataQa}>
      <FontAwesomeIcon
        icon={faExclamationTriangle}
        color={'red'}
        inverse
        className={'warning-icon'}
      />
      {text}
    </div>
  )
}

export default WarningLabel
