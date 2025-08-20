// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useRef } from 'react'
import { Redirect } from 'wouter'

import { combine } from 'lib-common/api'
import { useQueryResult } from 'lib-common/query'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'
import { Notifications } from 'lib-components/Notifications'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Main from 'lib-components/atoms/Main'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import Footer from '../Footer'
import { renderResult } from '../async-rendering'
import { AuthContext } from '../auth/state'
import { useTranslation } from '../localization'
import useTitle from '../useTitle'

import LoginDetailsSection from './LoginDetailsSection'
import NotificationSettingsSection from './NotificationSettingsSection'
import PersonalDetailsSection from './PersonalDetailsSection'
import {
  emailVerificationStatusQuery,
  notificationSettingsQuery,
  passwordConstraintsQuery
} from './queries'

export default React.memo(function PersonalDetails() {
  const t = useTranslation()
  const { apiVersion } = useContext(AuthContext)
  useTitle(t, t.personalDetails.title)
  const { user, refreshAuthStatus } = useContext(AuthContext)
  const notificationSettings = useQueryResult(notificationSettingsQuery())
  const notificationSettingsSection = useRef<HTMLDivElement>(null)
  const emailVerificationStatus = useQueryResult(emailVerificationStatusQuery())
  const passwordConstraints = useQueryResult(passwordConstraintsQuery())

  useEffect(() => {
    if (
      window.location.hash === '#notifications' &&
      user.isSuccess &&
      notificationSettings.isSuccess
    ) {
      scrollRefIntoView(notificationSettingsSection)
    }
  }, [user.isSuccess, notificationSettings.isSuccess])

  return (
    <Main>
      <Container>
        <Notifications apiVersion={apiVersion} sticky topOffset={80} />
        <Gap size="L" />
        <ContentArea opaque paddingVertical="L">
          <H1 noMargin>{t.personalDetails.title}</H1>
          {t.personalDetails.description}
          <HorizontalLine />
          {renderResult(
            combine(user, emailVerificationStatus),
            ([user, emailVerificationStatus]) =>
              user ? (
                <>
                  <PersonalDetailsSection
                    user={user}
                    emailVerificationStatus={emailVerificationStatus}
                    reloadUser={refreshAuthStatus}
                  />
                </>
              ) : (
                <Redirect replace to="/" />
              )
          )}
          {renderResult(
            combine(user, emailVerificationStatus, passwordConstraints),
            ([user, emailVerificationStatus, passwordConstraints]) =>
              user ? (
                <>
                  <HorizontalLine />
                  <LoginDetailsSection
                    user={user}
                    passwordConstraints={passwordConstraints}
                    emailVerificationStatus={emailVerificationStatus}
                    reloadUser={refreshAuthStatus}
                  />
                </>
              ) : (
                <Redirect replace to="/" />
              )
          )}
          {renderResult(notificationSettings, (notificationSettings) => (
            <>
              <HorizontalLine />
              <NotificationSettingsSection
                initialData={notificationSettings}
                ref={notificationSettingsSection}
              />
            </>
          ))}
        </ContentArea>
      </Container>
      <Footer />
    </Main>
  )
})
