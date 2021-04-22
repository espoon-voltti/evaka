// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import colors from 'lib-components/colors'
import { defaultMargins } from 'lib-components/white-space'
import Button from 'lib-components/atoms/buttons/Button'
import { useTranslation } from '../../state/i18n'

export type MessageBoxType = 'SENT' | 'DRAFT'

interface Props {
  activeMessageBox: MessageBoxType
  selectMessageBox: (selected: MessageBoxType) => void
  messageCounts: Record<MessageBoxType, number>
  onCreateNew: () => void
  createNewDisabled: boolean
  showReceiverSelection: () => void
  receiverSelectionShown: boolean
}

export default React.memo(function MessageBoxes({
  activeMessageBox,
  selectMessageBox,
  messageCounts,
  onCreateNew,
  createNewDisabled,
  showReceiverSelection,
  receiverSelectionShown
}: Props) {
  const { i18n } = useTranslation()

  return (
    <Container>
      <MessageBoxList>
        {(['SENT', 'DRAFT'] as MessageBoxType[]).map((type) => (
          <MessageBox
            key={type}
            className={
              !receiverSelectionShown && activeMessageBox === type
                ? 'selected-message-box'
                : ''
            }
            onClick={() => selectMessageBox(type)}
          >
            {i18n.messages.messageBoxes.names[type]}
            {messageCounts[type] > 0 ? ` (${messageCounts[type]})` : ''}
          </MessageBox>
        ))}
        <MessageBox
          className={receiverSelectionShown ? 'selected-message-box' : ''}
          onClick={showReceiverSelection}
        >
          {i18n.messages.messageBoxes.receivers}
        </MessageBox>
      </MessageBoxList>

      <BottomWrapper>
        <Button
          text={i18n.messages.messageBoxes.newBulletin}
          primary
          onClick={onCreateNew}
          disabled={createNewDisabled}
          data-qa="new-bulletin-btn"
        />
      </BottomWrapper>
    </Container>
  )
})

const Container = styled.div`
  width: 20%;
  min-width: 20%;
  max-width: 20%;
  background-color: ${colors.greyscale.white};
  display: flex;
  flex-direction: column;
`

const MessageBoxList = styled.div`
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  margin: ${defaultMargins.m} 0;
`

const MessageBox = styled.button`
  background: white;
  border: none;
  padding: ${defaultMargins.xs} ${defaultMargins.m};
  margin: 0 0 ${defaultMargins.xxs};
  text-align: left;
  cursor: pointer;
  &.selected-message-box {
    font-weight: 600;
    background: ${colors.brandEspoo.espooTurquoiseLight};
  }
`

const BottomWrapper = styled.div`
  padding: ${defaultMargins.m};
`
