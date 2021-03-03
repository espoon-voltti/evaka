// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import InfoModal from '@evaka/lib-components/molecules/modals/InfoModal'
import { faExclamation } from '@evaka/lib-icons'
import React, { useContext } from 'react'
import { UIContext } from '../../state/ui'
import { errorModalZIndex } from '@evaka/lib-components/layout/z-helpers'

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
          resolve={{
            action: () => clearErrorMessage(),
            label: errorMessage.resolveLabel
          }}
          size={'md'}
          zIndex={errorModalZIndex}
        />
      )}
    </div>
  )
}

export default ErrorMessage
