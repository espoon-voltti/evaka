// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faPen } from 'Icons'
import React from 'react'

import { boolean } from 'lib-common/form/fields'
import { object } from 'lib-common/form/form'
import { useBoolean, useForm, useFormFields } from 'lib-common/form/hooks'
import { EmailNotificationSettings } from 'lib-common/generated/api-types/pis'
import { Button } from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import MutateButton from 'lib-components/atoms/buttons/MutateButton'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { H2, P, Strong } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../localization'

import { EditButtonRow } from './components'
import { updateNotificationSettingsMutation } from './queries'

const notificationSettingsForm = object({
  message: boolean(),
  bulletin: boolean(),
  outdatedIncome: boolean(),
  calendarEvent: boolean(),
  decision: boolean(),
  document: boolean(),
  informalDocument: boolean(),
  missingAttendanceReservation: boolean()
})

export interface Props {
  initialData: EmailNotificationSettings
}

export default React.memo(
  React.forwardRef(function NotificationSettingsSection(
    { initialData }: Props,
    ref: React.Ref<HTMLDivElement>
  ) {
    const t = useTranslation()
    const [editing, useEditing] = useBoolean(false)
    const form = useForm(
      notificationSettingsForm,
      () => initialData,
      t.validationErrors
    )
    const {
      message,
      bulletin,
      outdatedIncome,
      calendarEvent,
      decision,
      document,
      informalDocument,
      missingAttendanceReservation
    } = useFormFields(form)

    return (
      <div data-qa="notification-settings-section" ref={ref}>
        <EditButtonRow>
          <InlineButton
            text={t.common.edit}
            icon={faPen}
            onClick={useEditing.on}
            disabled={editing}
            data-qa="start-editing"
          />
        </EditButtonRow>
        <H2 noMargin>{t.personalDetails.notificationsSection.title}</H2>
        <P>{t.personalDetails.notificationsSection.info}</P>
        <P>
          <Strong>{t.personalDetails.notificationsSection.subtitle}</Strong>
        </P>
        <CheckboxF
          bind={message}
          label={t.personalDetails.notificationsSection.message}
          disabled={!editing}
          data-qa="message"
        />
        <Gap size="s" />
        <CheckboxF
          bind={bulletin}
          label={t.personalDetails.notificationsSection.bulletin}
          disabled={!editing}
          data-qa="bulletin"
        />
        <Gap size="s" />
        <ExpandingInfo
          info={t.personalDetails.notificationsSection.outdatedIncomeInfo}
        >
          <CheckboxF
            bind={outdatedIncome}
            label={t.personalDetails.notificationsSection.outdatedIncome}
            disabled={!editing}
            data-qa="outdated-income"
          />
        </ExpandingInfo>
        {outdatedIncome.state === false ? (
          <>
            <Gap size="s" />
            <AlertBox
              noMargin
              message={
                t.personalDetails.notificationsSection.outdatedIncomeWarning
              }
            />
          </>
        ) : null}
        <Gap size="s" />
        <CheckboxF
          bind={calendarEvent}
          label={t.personalDetails.notificationsSection.calendarEvent}
          disabled={!editing}
          data-qa="calendar-event"
        />
        <Gap size="s" />
        <CheckboxF
          bind={decision}
          label={t.personalDetails.notificationsSection.decision}
          disabled={!editing}
          data-qa="decision"
        />
        <Gap size="s" />
        <ExpandingInfo
          info={t.personalDetails.notificationsSection.documentInfo}
        >
          <CheckboxF
            bind={document}
            label={t.personalDetails.notificationsSection.document}
            disabled={!editing}
            data-qa="document"
          />
        </ExpandingInfo>
        <Gap size="s" />
        <ExpandingInfo
          info={t.personalDetails.notificationsSection.informalDocumentInfo}
        >
          <CheckboxF
            bind={informalDocument}
            label={t.personalDetails.notificationsSection.informalDocument}
            disabled={!editing}
            data-qa="informal-document"
          />
        </ExpandingInfo>
        <Gap size="s" />
        <ExpandingInfo
          info={
            t.personalDetails.notificationsSection
              .missingAttendanceReservationInfo
          }
        >
          <CheckboxF
            bind={missingAttendanceReservation}
            label={
              t.personalDetails.notificationsSection
                .missingAttendanceReservation
            }
            disabled={!editing}
            data-qa="missing-attendance-reservation"
          />
        </ExpandingInfo>
        <Gap size="s" />
        {editing ? (
          <FixedSpaceRow justifyContent="flex-end">
            <Button
              onClick={() => {
                form.set(initialData)
                useEditing.off()
              }}
              text={t.common.cancel}
              data-qa="cancel"
            />
            <MutateButton
              mutation={updateNotificationSettingsMutation}
              onClick={() => ({
                body: form.value()
              })}
              onSuccess={useEditing.off}
              text={t.common.save}
              primary
              data-qa="save"
            />
          </FixedSpaceRow>
        ) : null}
      </div>
    )
  })
)
