// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import {
  Container,
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import Title from '@evaka/lib-components/src/atoms/Title'
import ErrorMessage from './login/ErrorMessage'
import { useTranslation } from '~state/i18n'
import { Gap } from '@evaka/lib-components/src/white-space'
import { getLoginUrl } from '~api/auth'
import { featureFlags } from '~config'

interface Props {
  error?: string
}

const LoginButton = styled.a`
  -webkit-font-smoothing: antialiased;
  text-size-adjust: 100%;
  box-sizing: inherit;
  height: 45px;
  padding: 0 27px;
  width: fit-content;
  min-width: 100px;
  display: block;
  text-align: center;
  overflow-x: hidden;
  border: 1px solid #3273c9;
  border-radius: 2px;
  outline: none;
  cursor: pointer;
  font-family: 'Open Sans', sans-serif;
  font-size: 14px;
  line-height: 16px;
  font-weight: 600;
  text-transform: uppercase;
  white-space: nowrap;
  letter-spacing: 0.2px;
  color: #ffffff;
  background: #3273c9;
  margin-right: 0;
  text-decoration: none;
  display: flex;
  justify-content: center;
  align-items: center;
`

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
      <Gap size={'L'} />
      <ContentArea opaque>
        <Title size={1} centered>
          {i18n.login.title}
        </Title>
        <Title size={2} centered>
          {i18n.login.subtitle}
        </Title>
        <Center>
          <LoginButton data-qa="login-btn" href={getLoginUrl('saml')}>
            <span>{i18n.login.loginAD}</span>
          </LoginButton>
          <Gap horizontal />
          {featureFlags.evakaLogin && (
            <LoginButton data-qa="login-btn" href={getLoginUrl('evaka')}>
              <span>{i18n.login.loginEvaka}</span>
            </LoginButton>
          )}
        </Center>
        <ErrorMessage error={error} />
      </ContentArea>
    </Container>
  )
}

export default Login
