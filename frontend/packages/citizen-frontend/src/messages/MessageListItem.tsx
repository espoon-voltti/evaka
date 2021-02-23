import React from 'react'

import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { ReceivedBulletin } from '~messages/types'
import { formatDate } from '~util'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'

type Props = {
  bulletin: ReceivedBulletin
  onClick: () => void
}
function MessageListItem({ bulletin, onClick }: Props) {
  return (
    <Container onClick={onClick} isRead={bulletin.isRead}>
      <FixedSpaceColumn>
        <Header>
          <span>{bulletin.sender}</span>
          <span>{formatDate(bulletin.sentAt, 'dd.MM')}</span>
        </Header>
        <Title>{bulletin.title}</Title>
        <ContentSummary>{bulletin.content.split('\n')[0]}</ContentSummary>
      </FixedSpaceColumn>
    </Container>
  )
}

const Container = styled.div<{ isRead: boolean }>`
  background-color: ${colors.greyscale.white};
  padding: ${defaultMargins.m};

  border: 1px solid ${colors.greyscale.lighter};

  ${(p) =>
    !p.isRead
      ? `
    border-left-color: ${colors.brandEspoo.espooTurquoise};
    border-left-width: 6px;
  `
      : ''}
`

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  font-weight: 600;
`

const Title = styled.div``

const ContentSummary = styled.div`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`

export default React.memo(MessageListItem)
