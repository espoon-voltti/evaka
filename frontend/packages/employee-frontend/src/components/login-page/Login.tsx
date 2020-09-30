// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import {
  Container,
  ContentAreaLanding,
  LinkButton
} from '~components/shared/alpha'
import ErrorMessage from './login/ErrorMessage'
import { useTranslation } from '~state/i18n'

interface Props {
  loginUrl: string
  error?: string
}

function Login({ loginUrl, error }: Props) {
  const { i18n } = useTranslation()

  return (
    <Container>
      <ContentAreaLanding
        title={i18n.login.title}
        subtitle={i18n.login.subtitle}
      >
        <LinkButton dataQa="login-btn" primary href={loginUrl}>
          <span>{i18n.login.login}</span>
        </LinkButton>
        <ErrorMessage error={error} />
      </ContentAreaLanding>
    </Container>
  )
}

export default Login
