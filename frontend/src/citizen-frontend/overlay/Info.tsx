// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { OverlayContext } from './state'
import FormModal from '@evaka/lib-components/molecules/modals/FormModal'

function GlobalInfoDialog() {
  const { infoMessage } = useContext(OverlayContext)
  return (
    <div>
      {infoMessage && (
        <FormModal
          title={infoMessage.title}
          icon={infoMessage.icon}
          iconColour={infoMessage.iconColour}
          text={infoMessage.text}
          resolve={infoMessage.resolve}
          reject={infoMessage.reject}
          size={'md'}
          data-qa={infoMessage['data-qa']}
        />
      )}
    </div>
  )
}

export default GlobalInfoDialog
