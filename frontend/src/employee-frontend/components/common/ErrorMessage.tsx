// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faExclamation } from 'lib-icons'
import React, { useContext } from 'react'
import { errorModalZIndex } from 'lib-components/layout/z-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { UIContext } from '../../state/ui'

function ErrorMessage() {
  const { errorMessage, clearErrorMessage } = useContext(UIContext)

  return errorMessage ? (
    <InfoModal
      title={errorMessage.title}
      icon={faExclamation}
      iconColor={errorMessage.type === 'error' ? 'red' : 'orange'}
      text={errorMessage.text}
      resolve={{
        action: clearErrorMessage,
        label: errorMessage.resolveLabel
      }}
      close={clearErrorMessage}
      zIndex={errorModalZIndex}
      data-qa="app-error-modal"
    />
  ) : null
}

export default ErrorMessage
