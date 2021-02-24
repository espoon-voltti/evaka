import React from 'react'

import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { ReceivedBulletin } from '~messages/types'
import { H3, P } from '@evaka/lib-components/src/typography'
import { messagesBreakpoint } from '~messages/const'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'
import {formatDate} from "~util";

type Props = {
  bulletin: ReceivedBulletin
}
function MessageReadView({ bulletin }: Props) {
  return (
    <Container>
      <Header>
        <Title>{bulletin.title}</Title>
        <FixedSpaceRow spacing='s'>
          <span>{bulletin.sender}</span>
          <span>{formatDate(bulletin.sentAt)}</span>
        </FixedSpaceRow>
      </Header>
      {bulletin.content.split('\n').map((pText, i) => (
        <P key={i}>{pText}</P>
      ))}
    </Container>
  )
}

const Container = styled.div`
  flex-grow: 1;
  box-sizing: border-box;
  max-width: 100%;
  min-height: 500px;
  background-color: ${colors.greyscale.white};
  padding: ${defaultMargins.m};

  @media (max-width: ${messagesBreakpoint}) {
    padding: ${defaultMargins.s};
  }
`

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
`

const Title = styled(H3)`
  font-weight: 600;
`

export default React.memo(MessageReadView)
