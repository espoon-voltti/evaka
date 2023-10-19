// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import ListGrid from 'lib-components/layout/ListGrid'
import { H2, Label } from 'lib-components/typography'

import { User } from '../auth/state'
import { useTranslation } from '../localization'

export interface Props {
  user: User
}

export default React.memo(function LoginDetailsSection({ user }: Props) {
  const t = useTranslation()
  return (
    <div data-qa="login-details-section">
      <ListGrid rowGap="s" columnGap="L" labelWidth="max-content">
        <H2 noMargin>{t.personalDetails.loginDetailsSection.title}</H2>
        <div />
        <Label>{t.personalDetails.loginDetailsSection.keycloakEmail}</Label>
        <div data-qa="keycloak-email">{user.keycloakEmail}</div>
      </ListGrid>
    </div>
  )
})
