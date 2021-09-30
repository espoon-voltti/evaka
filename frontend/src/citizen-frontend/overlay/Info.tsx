// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { OverlayContext } from './state'
import InfoModal from 'lib-components/molecules/modals/InfoModal'

function GlobalInfoDialog() {
  const { infoMessage, clearInfoMessage } = useContext(OverlayContext)
  return infoMessage ? (
    <InfoModal
      title={infoMessage.title}
      icon={infoMessage.icon}
      iconColour={infoMessage.iconColour}
      text={infoMessage.text}
      resolve={infoMessage.resolve}
      reject={infoMessage.reject}
      close={clearInfoMessage}
      data-qa={infoMessage['data-qa']}
    />
  ) : null
}

export default GlobalInfoDialog
