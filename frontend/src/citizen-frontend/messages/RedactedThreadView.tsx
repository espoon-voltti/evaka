import React from 'react'
import styled from 'styled-components'

import { CitizenMessageThread } from 'lib-common/generated/api-types/messaging'
import { FixedSpaceFlexWrap } from 'lib-components/layout/flex-helpers'
import { MessageCharacteristics } from 'lib-components/messages/MessageCharacteristics'
import { ThreadContainer } from 'lib-components/messages/ThreadListItem'
import { ScreenReaderButton } from 'lib-components/molecules/ScreenReaderButton'
import { H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../localization'
import { getStrongLoginUri } from '../navigation/const'

import { ThreadTitleRow } from './ThreadView'

const RedactedThreadInfo = styled.div`
  background-color: ${colors.grayscale.g0};
  margin: ${defaultMargins.xxs};
  padding: ${defaultMargins.L};
`
interface Props {
  thread: CitizenMessageThread.Redacted
  closeThread: () => void
}
export default React.memo(function ThreadView({
  thread: { urgent },
  closeThread
}: Props) {
  const i18n = useTranslation()

  const returnUrl = `${location.pathname}${location.search}${location.hash}`

  return (
    <ThreadContainer data-qa="thread-reader">
      <ThreadTitleRow>
        <FixedSpaceFlexWrap>
          <MessageCharacteristics
            type="MESSAGE"
            urgent={urgent}
            sensitive={true}
          />
        </FixedSpaceFlexWrap>
        <H2 noMargin data-qa="thread-reader-title">
          {i18n.messages.sensitive}
        </H2>
      </ThreadTitleRow>
      <Gap size="s" />
      <RedactedThreadInfo>
        <p>
          {i18n.messages.strongAuthRequiredThread}{' '}
          <a href={getStrongLoginUri(returnUrl)}>
            {i18n.messages.strongAuthLink}
          </a>
        </p>
      </RedactedThreadInfo>
      <ScreenReaderButton
        onClick={closeThread}
        text={i18n.messages.thread.close}
      />
    </ThreadContainer>
  )
})
