// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { faPen } from 'lib-icons'
import LocalDate from 'lib-common/local-date'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { tabletMin } from 'lib-components/breakpoints'
import colors from 'lib-components/colors'
import { H3 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { formatDate } from '../../utils/date'
import { useTranslation } from '../../state/i18n'
import { Bulletin } from './types'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

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
