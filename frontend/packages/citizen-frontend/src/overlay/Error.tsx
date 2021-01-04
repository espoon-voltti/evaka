// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faExclamation } from '@evaka/lib-icons'
import React, { useContext } from 'react'
import { OverlayContext } from './state'
import FormModal from '@evaka/lib-components/src/molecules/modals/FormModal'

function GlobalErrorDialog() {
  const { errorMessage, clearErrorMessage } = useContext(OverlayContext)
  return (
    <div>
      {errorMessage && (
        <FormModal
          title={errorMessage.title}
          icon={faExclamation}
          iconColour={errorMessage.type === 'error' ? 'red' : 'orange'}
          text={errorMessage.text}
          resolve={{
            label: errorMessage.resolveLabel ?? 'Ok',
            action: () => clearErrorMessage()
          }}
          size={'md'}
        />
      )}
    </div>
  )
}

export default GlobalErrorDialog
