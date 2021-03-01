// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { faPen } from '@evaka/lib-icons'
import LocalDate from '@evaka/lib-common/src/local-date'
import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'
import { tabletMin } from '@evaka/lib-components/src/breakpoints'
import colors from '@evaka/lib-components/src/colors'
import { H3 } from '@evaka/lib-components/src/typography'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { formatDate } from '~utils/date'
import { useTranslation } from '~state/i18n'
import { Bulletin } from './types'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'

interface Props {
  message: Bulletin
  onEdit: () => void
}

export default React.memo(function MessageReadView({ message, onEdit }: Props) {
  const { i18n } = useTranslation()

  return (
    <Container>
      <Header>
        <Title noMargin>{message.title || i18n.messages.noTitle}</Title>
        {message.sentAt ? (
          <FixedSpaceRow>
            <span>{message.groupName}</span>
            <span>
              {formatDate(
                message.sentAt,
                LocalDate.fromSystemTzDate(message.sentAt).isEqual(
                  LocalDate.today()
                )
                  ? 'HH:mm'
                  : 'd.M.'
              )}
            </span>
          </FixedSpaceRow>
        ) : (
          <InlineButton
            icon={faPen}
            onClick={onEdit}
            text={i18n.messages.editDraft}
          />
        )}
      </Header>
      <Info>{message.createdByEmployeeName}</Info>

      <div>
        {message.content.split('\n').map((text, i) => (
          <React.Fragment key={i}>
            <span>{text}</span>
            <br />
          </React.Fragment>
        ))}
      </div>
    </Container>
  )
})

const Container = styled.div`
  flex-grow: 1;
  min-height: 500px;
  overflow-y: auto;
  padding: ${defaultMargins.m};
  background-color: ${colors.greyscale.white};
  display: flex;
  flex-direction: column;
`

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  font-weight: 600;
  font-size: 16px;
  flex-wrap: wrap;
  margin-bottom: ${defaultMargins.xs};

  @media (max-width: ${tabletMin}) {
    flex-direction: column;
    flex-wrap: nowrap;
    justify-content: flex-start;
    align-items: flex-start;
  }
`

const Title = styled(H3)`
  font-weight: 600;
  margin-right: ${defaultMargins.XXL};
`

const Info = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: ${colors.greyscale.medium};
  margin-bottom: ${defaultMargins.L};
`
