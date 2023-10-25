// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useRef } from 'react'
import { Navigate } from 'react-router-dom'

import { useQueryResult } from 'lib-common/query'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Main from 'lib-components/atoms/Main'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import Footer from '../Footer'
import { renderResult } from '../async-rendering'
import { AuthContext } from '../auth/state'
import { useTranslation } from '../localization'

import NotificationSettingsSection from './NotificationSettingsSection'
import PersonalDetailsSection from './PersonalDetailsSection'
import { notificationSettingsQuery } from './queries'

export default React.memo(function PersonalDetails() {
  const t = useTranslation()
  const { user, refreshAuthStatus } = useContext(AuthContext)
  const notificationSettings = useQueryResult(notificationSettingsQuery())
  const notificationSettingsSection = useRef<HTMLDivElement>(null)

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
        <Gap size="L" />
        <ContentArea opaque paddingVertical="L">
          <H1 noMargin>{t.personalDetails.title}</H1>
          {t.personalDetails.description}
          <HorizontalLine />
          {renderResult(user, (user) =>
            user ? (
              <PersonalDetailsSection
                user={user}
                reloadUser={refreshAuthStatus}
              />
            ) : (
              <Navigate replace to="/" />
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
