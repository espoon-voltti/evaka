// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { LegacyAsyncButton } from 'lib-components/atoms/buttons/LegacyAsyncButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faUserUnlock } from 'lib-icons'

import { pinLogout } from '../../auth/api'
import { topBarHeight } from '../../constants'
import { useTranslation } from '../i18n'

const UserMenuContainer = styled.div`
  position: fixed;
  right: 0;
  top: calc(${topBarHeight} + ${defaultMargins.xxs});
  width: 100%;
  padding: ${defaultMargins.s};

  display: flex;
  flex-direction: column;

  background-color: ${colors.grayscale.g0};
  color: ${colors.grayscale.g100};
  box-shadow: 0 4px 4px 0 ${colors.grayscale.g15};
`

interface Props {
  name: string
  onLogoutSuccess: () => void
}

export const UserMenu = React.memo(function UserMenu(props: Props) {
  const { i18n } = useTranslation()
  return (
    <UserMenuContainer>
      <Label>{i18n.pin.loggedIn}</Label>

      <Gap size="xxs" />

      <FixedSpaceRow justifyContent="flex-start">
        <FontAwesomeIcon icon={faUserUnlock} size="lg" color={colors.main.m1} />{' '}
        <span data-qa="full-name">{props.name}</span>
      </FixedSpaceRow>

      <Gap />

      <LegacyAsyncButton
        onClick={pinLogout}
        onSuccess={props.onLogoutSuccess}
        primary
        text={i18n.pin.logOut}
        data-qa="logout-btn"
      />
    </UserMenuContainer>
  )
})
