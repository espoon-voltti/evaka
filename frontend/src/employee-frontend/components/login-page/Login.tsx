// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import Title from 'lib-components/atoms/Title'
import LinkButton from 'lib-components/atoms/buttons/LinkButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'

import { getLoginUrl } from '../../api/auth'
import { useTranslation } from '../../state/i18n'

import ErrorMessage from './login/ErrorMessage'

interface Props {
  error?: string
}

const Center = styled.div`
  display: flex;
  justify-content: center;
  margin-top: 80px;
  margin-bottom: 80px;
`

function Login({ error }: Props) {
  const { i18n } = useTranslation()

  return (
    <Container>
      <ContentArea opaque>
        <Title size={1} centered>
          {i18n.login.title}
        </Title>
        <Title size={2} centered>
          {i18n.login.subtitle}
        </Title>
        <Center>
          <LinkButton data-qa="login-btn" href={getLoginUrl('ad')}>
            <span>{i18n.login.loginAD}</span>
          </LinkButton>
          <Gap horizontal />
          <LinkButton
            data-qa="login-btn"
            href={
              featureFlags.employeeSfiLogin
                ? getLoginUrl('sfi')
                : getLoginUrl('keycloak')
            }
          >
            <span>{i18n.login.loginEvaka}</span>
          </LinkButton>
        </Center>
        <ErrorMessage error={error} />
      </ContentArea>
    </Container>
  )
}

export default Login
