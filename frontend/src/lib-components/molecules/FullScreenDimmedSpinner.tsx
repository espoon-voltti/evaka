// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { modalZIndex } from '../layout/z-helpers'
import ModalBackground from './modals/ModalBackground'
import Spinner from '../atoms/state/Spinner'

export function FullScreenDimmedSpinner() {
  return (
    <ModalBackground zIndex={modalZIndex}>
      <Spinner zIndex={modalZIndex} />
    </ModalBackground>
  )
}
