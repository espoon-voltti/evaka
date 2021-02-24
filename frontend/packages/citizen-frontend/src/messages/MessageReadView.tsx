import React from 'react'

import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { ReceivedBulletin } from '~messages/types'
import { H3, P } from '@evaka/lib-components/src/typography'
import { messagesBreakpoint } from '~messages/const'

type Props = {
  bulletin: ReceivedBulletin
}
function MessageReadView({ bulletin }: Props) {
  return (
    <Container>
      <H3>{bulletin.title}</H3>
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

export default React.memo(MessageReadView)
