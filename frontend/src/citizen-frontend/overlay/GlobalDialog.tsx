// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'

import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { faExclamation } from 'lib-icons'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'

import { OverlayContext } from './state'

export default React.memo(function GlobalDialog() {
  const { errorMessage, clearErrorMessage, infoMessage, clearInfoMessage } =
    useContext(OverlayContext)

  const modal = errorMessage ? (
    <InfoModal
      title={errorMessage.title}
      icon={faExclamation}
      type={errorMessage.type === 'error' ? 'danger' : 'warning'}
      text={errorMessage.text}
      resolve={{
        label: errorMessage.resolveLabel ?? 'Ok',
        action: clearErrorMessage
      }}
      close={clearErrorMessage}
    />
  ) : infoMessage ? (
    <InfoModal
      title={infoMessage.title}
      icon={infoMessage.icon}
      type={infoMessage.type}
      text={infoMessage.text}
      resolve={infoMessage.resolve}
      reject={infoMessage.reject}
      close={clearInfoMessage}
      data-qa={infoMessage['data-qa']}
    />
  ) : null

  return modal && <ModalAccessibilityWrapper>{modal}</ModalAccessibilityWrapper>
})
