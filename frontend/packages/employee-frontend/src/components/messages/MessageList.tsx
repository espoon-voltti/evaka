// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import LocalDate from '@evaka/lib-common/src/local-date'
import { Result } from '@evaka/lib-common/src/api'
import colors from '@evaka/lib-components/src/colors'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { H1, H3 } from '@evaka/lib-components/src/typography'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import HorizontalLine from '@evaka/lib-components/src/atoms/HorizontalLine'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'
import { useTranslation } from '~state/i18n'
import { formatDate } from '~utils/date'
import { MessageBoxType } from './MessageBoxes'
import { Bulletin, IdAndName } from './types'

interface Props {
  messageBoxType: MessageBoxType
  messages: Result<Bulletin[]>
  groups: IdAndName[]
  selectMessage: (msg: Bulletin) => void
}

export default React.memo(function MessageList({
  messageBoxType,
  messages,
  selectMessage,
  groups
}: Props) {
  const { i18n } = useTranslation()
  return (
    <Container>
      <HeaderContainer>
        <H1 noMargin>{i18n.messages.messageList.titles[messageBoxType]}</H1>
      </HeaderContainer>
      {messages.isLoading && <SpinnerSegment />}
      {messages.isFailure && <ErrorSegment title={i18n.common.loadingFailed} />}
      {messages.isSuccess && (
        <>
          {messages.value.map((msg) => (
            <React.Fragment key={msg.id}>
              <MessageListItem onClick={() => selectMessage(msg)}>
                <MessageTopRow>
                  <MessageTitle noMargin>
                    {msg.title || `(${i18n.messages.noTitle})`}
                  </MessageTitle>
                  {msg.sentAt ? (
                    <FixedSpaceRow>
                      <span>
                        {groups.find((g) => g.id === msg.groupId)?.name}
                      </span>
                      <span>
                        {formatDate(
                          msg.sentAt,
                          LocalDate.fromSystemTzDate(msg.sentAt).isEqual(
                            LocalDate.today()
                          )
                            ? 'HH:mm'
                            : 'd.M.'
                        )}
                      </span>
                    </FixedSpaceRow>
                  ) : (
                    <span>{i18n.messages.notSent}</span>
                  )}
                </MessageTopRow>
                <MessageSummary>
                  {msg.content.substring(0, 200).replace('\n', ' ')}
                </MessageSummary>
              </MessageListItem>
              <StyledHr />
            </React.Fragment>
          ))}
        </>
      )}
    </Container>
  )
})

const Container = styled.div`
  align-self: flex-start;
  flex-grow: 1;
  min-height: 500px;
  background-color: ${colors.greyscale.white};
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
  overflow-y: auto;
  max-height: 100%;
`

const HeaderContainer = styled.div`
  padding: ${defaultMargins.m};
`

const MessageListItem = styled.button`
  background: white;
  border: none;
  padding: ${defaultMargins.s} ${defaultMargins.m};
  margin: 0 0 ${defaultMargins.xxs};
  text-align: left;
  &.selected-message {
    font-weight: 600;
    background: ${colors.brandEspoo.espooTurquoiseLight};
  }
`

const MessageTopRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  width: 100%;
`

const MessageTitle = styled(H3)`
  font-size: 16px;
  font-weight: 600;
  color: ${colors.greyscale.dark};
`

const MessageSummary = styled.div`
  width: 80%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`

const StyledHr = styled(HorizontalLine)`
  margin-block-start: 0;
  margin-block-end: 0;
  width: calc(100% - ${defaultMargins.m});
`
