import React from 'react'

import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { ReceivedBulletin } from '~messages/types'
import { formatDate } from '~util'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import LocalDate from '@evaka/lib-common/src/local-date'

type Props = {
  bulletin: ReceivedBulletin
  active: boolean
  onClick: () => void
}
function MessageListItem({ bulletin, active, onClick }: Props) {
  return (
    <Container
      isRead={bulletin.isRead}
      active={active}
      onClick={onClick}
      tabIndex={0}
      onKeyPress={(e) => e.key === 'Enter' && onClick()}
    >
      <FixedSpaceColumn>
        <Header>
          <span>{bulletin.sender}</span>
          <span>
            {formatDate(
              bulletin.sentAt,
              LocalDate.fromSystemTzDate(bulletin.sentAt).isEqual(
                LocalDate.today()
              )
                ? 'HH:mm'
                : 'd.M.'
            )}
          </span>
        </Header>
        <Title>{bulletin.title}</Title>
        <ContentSummary>{bulletin.content.split('\n')[0]}</ContentSummary>
      </FixedSpaceColumn>
    </Container>
  )
}

const Container = styled.div<{ isRead: boolean; active: boolean }>`
  background-color: ${colors.greyscale.white};
  padding: ${defaultMargins.s} ${defaultMargins.m};
  cursor: pointer;

  border: 1px solid ${colors.greyscale.lighter};

  ${(p) =>
    !p.isRead
      ? `
    border-left-color: ${colors.brandEspoo.espooTurquoise};
    border-left-width: 6px;
    padding-left: calc(${defaultMargins.m} - 6px);
  `
      : ''}

  ${(p) => (p.active ? `background-color: #E9F5FF;` : '')}
`

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  font-weight: bold;
  font-size: 16px;
`

const Title = styled.div`
  font-weight: 600;
`

const ContentSummary = styled.div`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`

export default React.memo(MessageListItem)
