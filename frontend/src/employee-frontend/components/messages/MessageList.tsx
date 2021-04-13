// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import LocalDate from 'lib-common/local-date'
import { Result } from 'lib-common/api'
import useIntersectionObserver from 'lib-common/utils/useIntersectionObserver'
import colors from 'lib-components/colors'
import { defaultMargins } from 'lib-components/white-space'
import { H1, H2, H3 } from 'lib-components/typography'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { useTranslation } from '../../state/i18n'
import { formatDate } from '../../utils/date'
import { MessageBoxType } from './MessageBoxes'
import { Bulletin, IdAndName } from './types'

interface Props {
  bulletins: Bulletin[]
  unitName: string
  nextPage: Result<void>
  loadNextPage: () => void
  messageBoxType: MessageBoxType
  groups: IdAndName[]
  selectMessage: (msg: Bulletin) => void
}

export default React.memo(function MessageList({
  bulletins,
  unitName,
  nextPage,
  loadNextPage,
  messageBoxType,
  selectMessage
}: Props) {
  const { i18n } = useTranslation()

  const renderReceivers = (msg: Bulletin): string => {
    const visibleNamesCount = 3
    const parseFirstName = (firstNames: string) =>
      firstNames.trim().split(/\s+/)[0]
    const maybeVisible = [
      ...msg.receiverUnits
        .slice(0, visibleNamesCount)
        .map(({ unitName }) => unitName),
      ...msg.receiverGroups
        .slice(0, visibleNamesCount)
        .map(({ groupName }) => groupName),
      ...msg.receiverChildren
        .slice(0, visibleNamesCount)
        .map(
          ({ firstName, lastName }) =>
            `${parseFirstName(firstName)} ${lastName}`
        )
    ]
    const visibleNames = maybeVisible.slice(0, visibleNamesCount).join(', ')
    return maybeVisible.length > visibleNamesCount
      ? `${visibleNames}, ...`
      : visibleNames
  }

  return (
    <Container>
      <HeaderContainer>
        <H1 noMargin>{i18n.messages.messageList.titles[messageBoxType]}</H1>
        <H2>{unitName}</H2>
      </HeaderContainer>
      {bulletins.map((msg) => (
        <React.Fragment key={msg.id}>
          <MessageListItem onClick={() => selectMessage(msg)}>
            <MessageTopRow>
              <FixedSpaceRow
                justifyContent="space-between"
                alignItems="baseline"
              >
                <MessageReceivers>{renderReceivers(msg)}</MessageReceivers>

                {msg.sentAt ? (
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
                ) : (
                  <span>{i18n.messages.notSent}</span>
                )}
              </FixedSpaceRow>
            </MessageTopRow>
            <MessageSummary>
              <MessageTitle>
                {msg.title || `(${i18n.messages.noTitle})`}
              </MessageTitle>
              {' - '}
              {msg.content.substring(0, 200).replace('\n', ' ')}
            </MessageSummary>
          </MessageListItem>
          <StyledHr />
        </React.Fragment>
      ))}
      {nextPage.isSuccess && <OnEnterView onEnter={loadNextPage} />}
      {nextPage.isLoading && <SpinnerSegment />}
      {nextPage.isFailure && <ErrorSegment title={i18n.common.loadingFailed} />}
    </Container>
  )
})

const OnEnterView = React.memo(function IsInView({
  onEnter
}: {
  onEnter: () => void
}) {
  const ref = useIntersectionObserver<HTMLDivElement>(onEnter)
  return <div ref={ref} />
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
  cursor: pointer;
  &.selected-message {
    font-weight: 600;
    background: ${colors.brandEspoo.espooTurquoiseLight};
  }
`

const MessageTopRow = styled.div`
  width: 100%;
`

const MessageReceivers = styled(H3)`
  font-size: 16px;
  font-weight: 600;
  color: ${colors.greyscale.dark};
`

const MessageTitle = styled.span`
  font-size: 16px;
  font-weight: 600;
  color: ${colors.greyscale.darkest};
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
