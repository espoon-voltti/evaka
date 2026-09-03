// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled, { useTheme } from 'styled-components'

import type { DeviceClass } from 'lib-common/generated/api-types/user'
import type { CitizenPushDevice } from 'lib-common/generated/api-types/webpush'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import IconChip from 'lib-components/atoms/IconChip'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AlertBox, InfoBox } from 'lib-components/molecules/MessageBoxes'
import { InformationText, LabelLike, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'
import {
  faBell,
  faCheckCircle,
  faExclamation,
  faLaptop,
  faMobileButton,
  faTabletButton,
  faTrash
} from 'lib-icons'

import { renderResult } from '../async-rendering'
import { useTranslation } from '../localization'
import { platform } from '../pwa/platform'
import {
  usePushAvailability,
  useSubscribeToPush,
  useThisPushDevice,
  unsubscribeLocally
} from '../pwa/pushNotifications'
import {
  deletePushDeviceMutation,
  pushSettingsQuery,
  sendTestPushNotificationMutation
} from '../pwa/queries'

import { SectionTitle } from './components'

export default React.memo(
  React.forwardRef(function PushNotificationsSection(
    _props: unknown,
    ref: React.Ref<HTMLDivElement>
  ) {
    const i18n = useTranslation()
    const t = i18n.pwa.pushSection
    const { colors } = useTheme()
    const settings = useQueryResult(pushSettingsQuery())
    const availability = usePushAvailability()
    const thisDevice = useThisPushDevice()
    const subscribe = useSubscribeToPush()
    const { mutateAsync: deleteDevice } = useMutationResult(
      deletePushDeviceMutation
    )

    if (!featureFlags.citizenPwa) return null

    return renderResult(settings, ({ applicationServerKey, devices }) => {
      // Web push is not configured in this environment
      if (applicationServerKey === null) return null

      // Even if browser doesn't support push notifications, we still show the section if there
      // are devices, so that the user can revoke them.
      if (availability.kind === 'unavailable' && devices.length === 0)
        return null

      const currentDevice = devices.find((d) => d.id === thisDevice) ?? null

      const revoke = async (device: CitizenPushDevice) => {
        const deleted = await deleteDevice({ id: device.id })
        if (deleted.isSuccess && device.id === currentDevice?.id) {
          await unsubscribeLocally()
        }
      }

      return (
        <>
          <Gap $size="s" />
          <ContentArea
            $opaque
            $paddingVertical="m"
            data-qa="push-notifications-section"
            ref={ref}
          >
            <TitleRow>
              <SectionTitle $noMargin>{t.title}</SectionTitle>
              {devices.length > 0 && (
                <IconChip
                  label={t.enabled}
                  icon={faCheckCircle}
                  textColor={colors.accents.a1greenDark}
                  backgroundColor={colors.accents.a7mint}
                  iconColor={colors.status.success}
                  iconBackgroundColor="transparent"
                  data-qa="push-account-status"
                />
              )}
            </TitleRow>
            <P>{t.description}</P>

            {currentDevice ? (
              <ThisDeviceStrip data-qa="push-this-device-state">
                <InformationText>
                  {currentDevice.lastSentAt
                    ? t.lastSent(currentDevice.lastSentAt.format())
                    : t.neverSent}
                </InformationText>
                <MutateButton
                  appearance="inline"
                  text={t.sendTest}
                  mutation={sendTestPushNotificationMutation}
                  onClick={() => ({ body: { deviceId: currentDevice.id } })}
                  data-qa="send-test-push-notification"
                />
              </ThisDeviceStrip>
            ) : availability.kind === 'blocked' ? (
              <AlertBox
                noMargin
                title={t.blockedOnThisDevice}
                message={t.blockedInstructions[platform()]}
                data-qa="push-this-device-state"
              />
            ) : (
              <InfoBox
                noMargin
                darkBackground
                message={
                  <FixedSpaceColumn $spacing="s" $alignItems="flex-start">
                    <span>{t.notEnabledOnThisDevice}</span>
                    {availability.kind === 'subscribable' && (
                      <Button
                        appearance="inline"
                        text={t.enable}
                        onClick={() => void subscribe()}
                        data-qa="enable-push-notifications"
                      />
                    )}
                  </FixedSpaceColumn>
                }
                data-qa="push-this-device-state"
              />
            )}

            <Gap $size="s" />
            <FixedSpaceColumn $spacing="xs">
              {devices.map((device) => (
                <DeviceCard
                  key={device.id}
                  $current={device.id === currentDevice?.id}
                  data-qa="push-device"
                >
                  <CardIcon>
                    <FontAwesomeIcon icon={deviceIcon(device.deviceClass)} />
                  </CardIcon>
                  <CardContent>
                    <LabelLike data-qa="push-device-name">
                      {deviceName(
                        device.installed ? t.installedApp : t.browser,
                        device.operatingSystemName
                      )}
                    </LabelLike>
                    <Chips>
                      {device.id === currentDevice?.id && (
                        <IconChip
                          label={t.thisDevice}
                          icon={faBell}
                          textColor={colors.grayscale.g100}
                          backgroundColor={colors.main.m4}
                          iconColor={colors.grayscale.g0}
                          iconBackgroundColor={colors.main.m2}
                          data-qa="push-device-current"
                        />
                      )}
                      {device.id === currentDevice?.id &&
                        availability.kind === 'blocked' && (
                          <IconChip
                            label={t.blocked}
                            icon={faExclamation}
                            textColor={colors.accents.a2orangeDark}
                            backgroundColor={colors.status.warningBackground}
                            iconColor={colors.grayscale.g0}
                            iconBackgroundColor={colors.status.warning}
                          />
                        )}
                    </Chips>
                    <InformationText>
                      {t.inUseSince} {device.createdAt.toLocalDate().format()}
                    </InformationText>
                  </CardContent>
                  <IconOnlyButton
                    icon={faTrash}
                    aria-label={t.revoke}
                    onClick={() => void revoke(device)}
                    data-qa="revoke-push-device"
                  />
                </DeviceCard>
              ))}
            </FixedSpaceColumn>
          </ContentArea>
        </>
      )
    })
  })
)

const deviceName = (kind: string, operatingSystemName: string) =>
  operatingSystemName === '' ? kind : `${kind} (${operatingSystemName})`

const deviceIcon = (deviceClass: DeviceClass) => {
  switch (deviceClass) {
    case 'PHONE':
      return faMobileButton
    case 'TABLET':
      return faTabletButton
    case 'DESKTOP':
    case 'UNKNOWN':
      return faLaptop
  }
}

const TitleRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${defaultMargins.s};
`

const ThisDeviceStrip = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g4};
  border-radius: 4px;
  padding: ${defaultMargins.s};
`

const DeviceCard = styled.div<{ $current: boolean }>`
  display: flex;
  align-items: flex-start;
  gap: ${defaultMargins.s};
  border: 1px solid
    ${(p) => (p.$current ? p.theme.colors.main.m2 : p.theme.colors.grayscale.g15)};
  border-radius: 4px;
  padding: ${defaultMargins.s};
`

const CardIcon = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  font-size: 20px;
  color: ${(p) => p.theme.colors.grayscale.g100};
`

const CardContent = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.xxs};
  flex: 1 0 0;
  min-width: 0;
`

const Chips = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${defaultMargins.xxs};
`
