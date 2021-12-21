// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { Label } from 'lib-components/typography'
import { faUserUnlock } from 'lib-icons'
import React from 'react'
import styled from 'styled-components'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { pinLogout } from '../../../api/auth'
import { useTranslation } from '../../../state/i18n'
import { topBarHeight } from '../../constants'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

const UserMenuContainer = styled.div`
  position: fixed;
  right: 0;
  top: calc(${topBarHeight} + ${defaultMargins.xxs});
  width: 100%;
  padding: ${defaultMargins.s};

  display: flex;
  flex-direction: column;

  background-color: ${colors.greyscale.white};
  color: ${colors.greyscale.darkest};
  box-shadow: 0 4px 4px 0 ${colors.greyscale.lighter};
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
        <FontAwesomeIcon
          icon={faUserUnlock}
          size="lg"
          color={colors.main.dark}
        />{' '}
        <span data-qa="full-name">{props.name}</span>
      </FixedSpaceRow>

      <Gap />

      <AsyncButton
        onClick={pinLogout}
        onSuccess={props.onLogoutSuccess}
        primary
        text={i18n.pin.logOut}
        data-qa="logout-btn"
      />
    </UserMenuContainer>
  )
})
