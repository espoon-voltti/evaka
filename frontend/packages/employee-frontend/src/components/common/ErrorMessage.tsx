// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import InfoModal from '~components/common/InfoModal'
import { faExclamation } from '@evaka/lib-icons'
import React, { useContext } from 'react'
import { UIContext } from '~state/ui'

import '~components/common/ErrorMessage.scss'
import { errorModalZIndex } from '@evaka/lib-components/src/layout/z-helpers'

function ErrorMessage() {
  const { errorMessage, clearErrorMessage } = useContext(UIContext)

  return (
    <div>
      {errorMessage && (
        <InfoModal
          title={errorMessage.title}
          icon={faExclamation}
          iconColour={errorMessage.type === 'error' ? 'red' : 'orange'}
          text={errorMessage.text}
          resolveLabel={errorMessage.resolveLabel}
          rejectLabel={errorMessage.resolveLabel}
          resolve={() => clearErrorMessage()}
          size={'md'}
          zIndex={errorModalZIndex}
        />
      )}
    </div>
  )
}

export default ErrorMessage
