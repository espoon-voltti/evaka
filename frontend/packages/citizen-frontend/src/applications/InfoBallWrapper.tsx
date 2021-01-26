import React, { ReactNode } from 'react'
import styled from 'styled-components'
import { faInfo } from '@evaka/lib-icons'
import colors from '@evaka/lib-components/src/colors'
import { P } from '@evaka/lib-components/src/typography'
import { Gap } from '@evaka/lib-components/src/white-space'
import Tooltip from '@evaka/lib-components/src/atoms/Tooltip'
import RoundIcon from '@evaka/lib-components/src/atoms/RoundIcon'

type Props = {
  infoText: string
  children: ReactNode
}

export default React.memo(function InfoBallWrapper({
  infoText,
  children
}: Props) {
  return (
    <Container>
      {children}
      <Gap size="xs" horizontal />
      <Tooltip tooltip={<P>{infoText}</P>}>
        <RoundIcon
          content={faInfo}
          color={colors.brandEspoo.espooTurquoise}
          size="s"
        />
      </Tooltip>
    </Container>
  )
})

const Container = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;
`
