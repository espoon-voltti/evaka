import { faInbox } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { useTranslation } from '../localization'
import { H3 } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import React from 'react'
import styled from 'styled-components'

interface Props {
  inboxEmpty: boolean
}

export default React.memo(function EmptyThreadView({ inboxEmpty }: Props) {
  const i18n = useTranslation()
  return inboxEmpty ? (
    <EmptyThreadViewContainer>
      <FontAwesomeIcon
        icon={faInbox}
        size={'7x'}
        color={colors.greyscale.medium}
      />
      <H3>{i18n.messages.emptyInbox}</H3>
    </EmptyThreadViewContainer>
  ) : (
    <EmptyThreadViewContainer>
      <H3>{i18n.messages.noSelectedMessage}</H3>
    </EmptyThreadViewContainer>
  )
})

const EmptyThreadViewContainer = styled.div`
  text-align: center;
  width: 100%;
  background: white;
  padding-top: 10%;
`
