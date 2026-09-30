// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Redirect, useLocation, useSearchParams } from 'wouter'

import Main from 'lib-components/atoms/Main'
import { H1, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import Footer from '../Footer'
import { useUser } from '../auth/state'
import { useTranslation } from '../localization'
import { getStrongLoginUri } from '../navigation/const'
import useTitle from '../useTitle'

import {
  HelpLinkRow,
  LoginCard,
  LoginContainer,
  TopGap,
  WideLinkButton
} from './layout'
import { validatedNextPath } from './next-path'

export default React.memo(function StrongLoginPage() {
  const i18n = useTranslation()
  const t = i18n.loginPage.strongLogin
  useTitle(i18n, t.title)
  const user = useUser()
  const [, navigate] = useLocation()

  const [searchParams] = useSearchParams()
  const nextPath = validatedNextPath(searchParams.get('next'))

  if (user?.authLevel === 'STRONG') {
    return <Redirect to={nextPath} replace />
  }

  return (
    <Main>
      <TopGap />
      <LoginContainer>
        <LoginCard>
          <H1 $noMargin $hyphenate>
            {t.title}
          </H1>
          <Gap $size="s" />
          <P $noMargin>{t.info}</P>
          <Gap $size="L" />
          <WideLinkButton
            href={getStrongLoginUri(nextPath)}
            $style="primary"
            data-qa="strong-login"
          >
            {i18n.loginPage.applying.link}
          </WideLinkButton>
          <Gap $size="s" />
          <WideLinkButton
            href="/"
            onClick={(e) => {
              e.preventDefault()
              navigate('/')
            }}
            $style="secondary"
            data-qa="go-to-homepage"
          >
            {t.goToHomepage}
          </WideLinkButton>
        </LoginCard>
        <HelpLinkRow />
      </LoginContainer>
      <Footer narrow />
    </Main>
  )
})
