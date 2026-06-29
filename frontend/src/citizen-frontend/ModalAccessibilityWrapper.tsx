// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ReactNode } from 'react'
import { useContext } from 'react'
import React from 'react'
import ReactDOM from 'react-dom'

import { OverlayContext } from './overlay/state'

interface Props {
  children: ReactNode
}

function ModalAccessibilityWrapper(props: Props) {
  const { setModalOpen } = useContext(OverlayContext)

  React.useEffect(() => {
    setModalOpen(true)
    return () => {
      setModalOpen(false)
    }
  }, [setModalOpen])

  const container = document.getElementById('modal-container')

  if (!container) {
    return props.children
  }

  return ReactDOM.createPortal(props.children, container)
}

export default ModalAccessibilityWrapper
