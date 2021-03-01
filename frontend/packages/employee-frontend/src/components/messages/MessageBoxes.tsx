// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import { useTranslation } from '~state/i18n'

export type MessageBoxType = 'SENT' | 'DRAFT'

interface Props {
  activeMessageBox: MessageBoxType | null
  selectMessageBox: (selected: MessageBoxType) => void
  messageCounts: Record<MessageBoxType, number>
  onCreateNew: () => void
  createNewDisabled: boolean
}

export default React.memo(function MessageBoxes({
  activeMessageBox,
  selectMessageBox,
  messageCounts,
  onCreateNew,
  createNewDisabled
}: Props) {
  const { i18n } = useTranslation()

  return (
    <Container>
      <MessageBoxList>
        {(['SENT', 'DRAFT'] as MessageBoxType[]).map((type) => (
          <MessageBox
            key={type}
            className={activeMessageBox === type ? 'selected-message-box' : ''}
            onClick={() => selectMessageBox(type)}
          >
            {i18n.messages.messageBoxes.names[type]}
            {messageCounts[type] > 0 ? ` (${messageCounts[type]})` : ''}
          </MessageBox>
        ))}
      </MessageBoxList>

      <BottomWrapper>
        <Button
          text={i18n.messages.messageBoxes.newBulletin}
          primary
          onClick={onCreateNew}
          disabled={createNewDisabled}
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
