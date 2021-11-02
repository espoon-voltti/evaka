// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { modalZIndex } from '../layout/z-helpers'
import ModalBackground from './modals/ModalBackground'
import Spinner from '../atoms/state/Spinner'

const FullScreenCentered = styled.div`
  position: fixed;
  top: 0;
  bottom: 0;
  right: 0;
  left: 0;
  display: flex;
  justify-content: center;
  align-items: center;
`

export function FullScreenDimmedSpinner() {
  return (
    <ModalBackground zIndex={modalZIndex}>
      <FullScreenCentered>
        <Spinner />
      </FullScreenCentered>
    </ModalBackground>
  )
}
