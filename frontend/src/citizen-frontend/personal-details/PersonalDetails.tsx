// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import type { RefObject } from 'react'
import React, { useContext, useEffect, useRef } from 'react'
import styled from 'styled-components'
import { Redirect } from 'wouter'

import { combine } from 'lib-common/api'
import { useQueryResult } from 'lib-common/query'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'
import Main from 'lib-components/atoms/Main'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { desktopMin } from 'lib-components/breakpoints'
import { ContentArea, NarrowContainer } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H1 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faChevronRight, fasInfo } from 'lib-icons'

import Footer from '../Footer'
import { renderResult } from '../async-rendering'
import { passkeysSupported } from '../auth/passkeys'
import { AuthContext } from '../auth/state'
import { useTranslation } from '../localization'
import useTitle from '../useTitle'

import ContactDetailsSection from './ContactDetailsSection'
import FamilySizeSection from './FamilySizeSection'
import LoginDetailsSection from './LoginDetailsSection'
import NotificationSettingsSection from './NotificationSettingsSection'
import PasskeysSection from './PasskeysSection'
import PersonalDetailsSection from './PersonalDetailsSection'
import {
  emailVerificationStatusQuery,
  familyQuery,
  notificationSettingsQuery,
  passwordConstraintsQuery
} from './queries'
import type { PersonalDetailsTaskSection } from './tasks'
import { personalDetailsTaskConfig, usePersonalDetailsTasks } from './tasks'

const DesktopTopGap = styled.div`
  display: none;

  @media (min-width: ${desktopMin}) {
    display: block;
    height: ${defaultMargins.L};
  }
`

const TaskBox = styled.button`
  display: flex;
  align-items: flex-start;
  gap: ${defaultMargins.s};
  width: 100%;
  padding: ${defaultMargins.s};
  background: ${(p) => p.theme.colors.main.m4};
  border: none;
  border-radius: 4px;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  font-size: 1rem;
  color: inherit;
`

const TaskTexts = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.xxs};
`

const TaskTitle = styled.div`
  color: ${(p) => p.theme.colors.main.m2};
  font-weight: 600;
`

const TaskDescription = styled.div`
  color: ${(p) => p.theme.colors.grayscale.g70};
  font-size: 14px;
  font-weight: 600;
`

const TaskChevron = styled(FontAwesomeIcon)`
  align-self: center;
  color: ${(p) => p.theme.colors.main.m2};
`

const Task = React.memo(function Task({
  title,
  description,
  onClick,
  dataQa
}: {
  title: string
  description: string
  onClick: () => void
  dataQa: string
}) {
  return (
    <TaskBox type="button" onClick={onClick} data-qa={dataQa}>
      <RoundIcon content={fasInfo} color={colors.main.m2} size="m" />
      <TaskTexts>
        <TaskTitle>{title}</TaskTitle>
        <TaskDescription>{description}</TaskDescription>
      </TaskTexts>
      <TaskChevron icon={faChevronRight} />
    </TaskBox>
  )
})

export default React.memo(function PersonalDetails() {
  const t = useTranslation()
  useTitle(t, t.personalDetails.title)
  const { user, refreshAuthStatus } = useContext(AuthContext)
  const notificationSettings = useQueryResult(notificationSettingsQuery())
  const notificationSettingsSection = useRef<HTMLDivElement>(null)
  const contactDetailsSection = useRef<HTMLDivElement>(null)
  const loginDetailsSection = useRef<HTMLDivElement>(null)
  const passkeysSection = useRef<HTMLDivElement>(null)
  const emailVerificationStatus = useQueryResult(emailVerificationStatusQuery())
  const passwordConstraints = useQueryResult(passwordConstraintsQuery())
  const family = useQueryResult(familyQuery())
  const showFamilySizeSection = family
    .map(({ children }) => children.length > 0)
    .getOrElse(false)

  const tasks = usePersonalDetailsTasks()
  const taskSectionRefs: Record<
    PersonalDetailsTaskSection,
    RefObject<HTMLDivElement | null>
  > = {
    contact: contactDetailsSection,
    login: loginDetailsSection,
    passkeys: passkeysSection,
    notifications: notificationSettingsSection
  }

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
      <NarrowContainer>
        <DesktopTopGap />
        <ContentArea $opaque $paddingVertical="m">
          <H1 $noMargin>{t.personalDetails.title}</H1>
          {tasks.length > 0 && (
            <>
              <Gap $size="s" />
              <FixedSpaceColumn $spacing="xs">
                {tasks.map((task) => {
                  const { dataQa, section } = personalDetailsTaskConfig[task]
                  return (
                    <Task
                      key={task}
                      dataQa={dataQa}
                      title={t.personalDetails.tasks[task].title}
                      description={t.personalDetails.tasks[task].description}
                      onClick={() =>
                        scrollRefIntoView(taskSectionRefs[section])
                      }
                    />
                  )
                })}
              </FixedSpaceColumn>
            </>
          )}
        </ContentArea>

        <Gap $size="s" />

        <ContentArea $opaque $paddingVertical="m">
          {renderResult(user, (user) =>
            user ? (
              <PersonalDetailsSection
                user={user}
                reloadUser={refreshAuthStatus}
              />
            ) : (
              <Redirect replace to="/" />
            )
          )}
        </ContentArea>

        <Gap $size="s" />

        <ContentArea $opaque $paddingVertical="m" ref={contactDetailsSection}>
          {renderResult(
            combine(user, emailVerificationStatus),
            ([user, emailVerificationStatus]) =>
              user ? (
                <ContactDetailsSection
                  user={user}
                  emailVerificationStatus={emailVerificationStatus}
                  reloadUser={refreshAuthStatus}
                />
              ) : (
                <Redirect replace to="/" />
              )
          )}
        </ContentArea>

        {showFamilySizeSection && (
          <>
            <Gap $size="s" />
            <ContentArea $opaque $paddingVertical="m">
              {renderResult(combine(user, family), ([user, family]) =>
                user ? (
                  <FamilySizeSection user={user} family={family} />
                ) : (
                  <Redirect replace to="/" />
                )
              )}
            </ContentArea>
          </>
        )}

        {passkeysSupported() && (
          <>
            <Gap $size="s" />
            <ContentArea $opaque $paddingVertical="m" ref={passkeysSection}>
              {renderResult(user, (user) =>
                user ? (
                  <PasskeysSection user={user} />
                ) : (
                  <Redirect replace to="/" />
                )
              )}
            </ContentArea>
          </>
        )}

        <Gap $size="s" />

        <ContentArea $opaque $paddingVertical="m" ref={loginDetailsSection}>
          {renderResult(
            combine(user, emailVerificationStatus, passwordConstraints),
            ([user, emailVerificationStatus, passwordConstraints]) =>
              user ? (
                <LoginDetailsSection
                  user={user}
                  passwordConstraints={passwordConstraints}
                  emailVerificationStatus={emailVerificationStatus}
                  reloadUser={refreshAuthStatus}
                />
              ) : (
                <Redirect replace to="/" />
              )
          )}
        </ContentArea>

        <Gap $size="s" />

        <ContentArea
          $opaque
          $paddingVertical="m"
          ref={notificationSettingsSection}
        >
          {renderResult(notificationSettings, (notificationSettings) => (
            <NotificationSettingsSection
              initialData={notificationSettings}
              ref={notificationSettingsSection}
            />
          ))}
        </ContentArea>
      </NarrowContainer>
      <Footer />
    </Main>
  )
})
