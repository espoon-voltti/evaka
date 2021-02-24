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
import IconButton from '@evaka/lib-components/src/atoms/buttons/IconButton'
import { faArrowLeft } from '@evaka/lib-icons'
import { messagesBreakpoint } from '~messages/const'

type Props = {
  bulletins: Result<ReceivedBulletin[]>
  activeBulletin: ReceivedBulletin | null
  onClickBulletin: (target: ReceivedBulletin) => void
  onReturn: () => void
}

function MessagesList({
  bulletins,
  activeBulletin,
  onClickBulletin,
  onReturn
}: Props) {
  const t = useTranslation()

  return (
    <>
      {activeBulletin && (
        <MobileOnly>
          <Return
            icon={faArrowLeft}
            onClick={onReturn}
            altText={t.common.return}
          />
        </MobileOnly>
      )}
      <Container className={activeBulletin ? 'desktop-only' : undefined}>
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
              active={activeBulletin?.id === bulletin.id}
            />
          ))}
      </Container>
    </>
  )
}

const MobileOnly = styled.div`
  display: none;

  @media (max-width: ${messagesBreakpoint}) {
    display: block;
  }
`

const Return = styled(IconButton)`
  margin-left: ${defaultMargins.xs};
`

const Container = styled.div`
  width: 400px;
  max-width: 100%;
  min-height: 500px;
  background-color: ${colors.greyscale.white};

  &.desktop-only {
    @media (max-width: ${messagesBreakpoint}) {
      display: none;
    }
  }
`

const HeaderContainer = styled.div`
  padding: ${defaultMargins.m};
`

export default React.memo(MessagesList)
