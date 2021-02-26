// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import { defaultMargins } from '@evaka/lib-components/src/white-space'

export type MessageBoxType = 'SENT' | 'DRAFT'

interface Props {
  activeMessageBox: MessageBoxType | null
  selectMessageBox: (selected: MessageBoxType) => void
  messageCounts: Record<MessageBoxType, number>
}

export default React.memo(function MessageBoxes({
  activeMessageBox,
  selectMessageBox,
  messageCounts
}: Props) {
  return (
    <Container>
      <MessageBox
        className={activeMessageBox === 'SENT' ? 'selected-message-box' : ''}
        onClick={() => selectMessageBox('SENT')}
      >
        Lähetetyt{' '}
        {messageCounts['SENT'] > 0 ? `(${messageCounts['SENT']})` : ''}
      </MessageBox>
      <MessageBox
        className={activeMessageBox === 'DRAFT' ? 'selected-message-box' : ''}
        onClick={() => selectMessageBox('DRAFT')}
      >
        Luonnokset{' '}
        {messageCounts['DRAFT'] > 0 ? `(${messageCounts['DRAFT']})` : ''}
      </MessageBox>
    </Container>
  )
})

const Container = styled.div`
  min-width: 35%;
  max-width: 400px;
  min-height: 500px;
  background-color: ${colors.greyscale.white};
  display: flex;
  flex-direction: column;
`

const MessageBox = styled.button`
  background: white;
  border: none;
  padding: ${defaultMargins.s} ${defaultMargins.m};
  margin: 0;
  text-align: left;
  &.selected-message-box {
    font-weight: 600;
    background: ${colors.brandEspoo.espooTurquoiseLight};
  }
`
