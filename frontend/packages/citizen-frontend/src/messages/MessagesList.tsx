import React from 'react'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { H1 } from '@evaka/lib-components/src/typography'
import { Result } from '@evaka/lib-common/src/api'
import { ReceivedBulletin } from '~messages/types'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import { useTranslation } from '~localization'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import MessageListItem from '~messages/MessageListItem'

type Props = {
  bulletins: Result<ReceivedBulletin[]>
  onClickBulletin: (target: ReceivedBulletin) => void
}

function MessagesList({ bulletins, onClickBulletin }: Props) {
  const t = useTranslation()

  return (
    <Container>
      <HeaderContainer>
        <H1 noMargin>{t.messages.inboxTitle}</H1>
        {bulletins.isSuccess && bulletins.value.length === 0 && (
          <span>{t.messages.noMessages}</span>
        )}
      </HeaderContainer>

      {bulletins.isLoading && <SpinnerSegment />}
      {bulletins.isFailure && (
        <ErrorSegment title={t.common.errors.genericGetError} />
      )}
      {bulletins.isSuccess &&
        bulletins.value.map((bulletin) => (
          <MessageListItem
            key={bulletin.id}
            bulletin={bulletin}
            onClick={() => onClickBulletin(bulletin)}
          />
        ))}
    </Container>
  )
}

const Container = styled.div`
  width: 400px;
  min-width: 400px;
  min-height: 500px;
  background-color: ${colors.greyscale.white};
`

const HeaderContainer = styled.div`
  padding: ${defaultMargins.m};
`

export default React.memo(MessagesList)
