// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ReactNode } from 'react'
import React from 'react'
import ReactDOM from 'react-dom'

import { OverlayContext } from './overlay/state'

interface Props {
  children: ReactNode
}

export default class ModalAccessibilityWrapper extends React.Component<Props> {
  static contextType = OverlayContext
  declare context: React.ContextType<typeof OverlayContext>

  container: HTMLElement | null

  constructor(props: Props) {
    super(props)
    this.container = document.getElementById('modal-container')
  }

  componentDidMount() {
    this.context.setModalOpen(true)
  }

  componentWillUnmount() {
    this.context.setModalOpen(false)
  }

  render() {
    if (!this.container) {
      return this.props.children
    }

    return ReactDOM.createPortal(this.props.children, this.container)
  }
}
