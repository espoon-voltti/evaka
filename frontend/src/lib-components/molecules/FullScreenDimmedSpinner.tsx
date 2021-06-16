// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import FocusLock from 'react-focus-lock'
import { modalZIndex } from '../layout/z-helpers'
import { BackgroundOverlay, ModalWrapper } from './modals/FormModal'
import Spinner from '../atoms/state/Spinner'

export function FullScreenDimmedSpinner() {
  return (
    <FocusLock>
      <ModalWrapper>
        <BackgroundOverlay />
        <Spinner zIndex={modalZIndex} />
      </ModalWrapper>
    </FocusLock>
  )
}
