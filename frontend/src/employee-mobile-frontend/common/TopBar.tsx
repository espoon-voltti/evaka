// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { fasExclamation } from 'Icons'
import React, { useState } from 'react'
import styled from 'styled-components'

import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faArrowLeft, faTimes } from 'lib-icons'

import { topBarHeight, zIndex } from '../constants'

import { useTranslation } from './i18n'
import { LoggedInUser } from './top-bar/LoggedInUser'
import { TopBarIconContainer } from './top-bar/TopBarIconContainer'
import { currentSystemNotificationQuery } from './top-bar/queries'

const StickyTopBar = styled.section<{ invertedColors?: boolean }>`
  position: sticky;
  top: 0;
  width: 100%;
  height: ${topBarHeight};
  padding: 0 ${defaultMargins.s};
  z-index: ${zIndex.topBar};

  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;

  background: ${(p) =>
    p.invertedColors ? p.theme.colors.grayscale.g4 : p.theme.colors.main.m2};
  color: ${(p) =>
    p.invertedColors ? p.theme.colors.main.m1 : p.theme.colors.grayscale.g0};

  button {
    color: ${(p) =>
      p.invertedColors ? p.theme.colors.main.m1 : p.theme.colors.grayscale.g0};
  }
`

const Title = styled.div`
  flex-grow: 6; // occupy majority of the space, leaving rest for the user menu
  margin: 0 ${defaultMargins.s};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`

interface Props {
  unitId: UUID | undefined
  title: string
  onBack?: () => void
  onClose?: () => void
  closeDisabled?: boolean
  invertedColors?: boolean
}

export default React.memo(function TopBar({
  unitId,
  title,
  onBack,
  onClose,
  closeDisabled = false,
  invertedColors
}: Props) {
  const { i18n } = useTranslation()

  // only shown on main screen
  const hideSystemNotification = invertedColors

  const notificationResult = useQueryResult(currentSystemNotificationQuery(), {
    enabled: !hideSystemNotification
  })

  const [showNotificationModal, setShowNotificationModal] = useState(false)

  return (
    <>
      <StickyTopBar invertedColors={invertedColors}>
        {onBack && (
          <TopBarIconContainer>
            <IconOnlyButton
              icon={faArrowLeft}
              onClick={onBack}
              aria-label={i18n.common.back}
              data-qa="go-back"
            />
          </TopBarIconContainer>
        )}
        <Title>
          <Label
            data-qa="top-bar-title"
            primary={invertedColors}
            white={!invertedColors}
          >
            {title}
          </Label>
        </Title>
        <FixedSpaceRow alignItems="center">
          {!hideSystemNotification &&
            notificationResult.isSuccess &&
            notificationResult.value.notification && (
              <NotificationIconWrapper
                data-qa="system-notification-btn"
                onClick={() => setShowNotificationModal(true)}
              >
                <FontAwesomeIcon icon={fasExclamation} size="lg" inverse />
              </NotificationIconWrapper>
            )}
          {onClose ? (
            <TopBarIconContainer>
              <IconOnlyButton
                icon={faTimes}
                color="white"
                disabled={closeDisabled}
                onClick={onClose}
                aria-label={i18n.common.close}
              />
            </TopBarIconContainer>
          ) : (
            <LoggedInUser unitId={unitId} />
          )}
        </FixedSpaceRow>
      </StickyTopBar>
      {showNotificationModal &&
        !hideSystemNotification &&
        notificationResult.isSuccess &&
        notificationResult.value.notification && (
          <InfoModal
            data-qa="system-notification-modal"
            title={i18n.systemNotification.title}
            text={notificationResult.value.notification.text}
            close={() => setShowNotificationModal(false)}
            resolve={{
              action: () => setShowNotificationModal(false),
              label: i18n.common.close
            }}
            closeLabel={i18n.common.close}
          />
        )}
    </>
  )
})

const NotificationIconWrapper = styled.div`
  width: 36px;
  height: 36px;
  background-color: ${(p) => p.theme.colors.status.warning};
  border-radius: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
`
