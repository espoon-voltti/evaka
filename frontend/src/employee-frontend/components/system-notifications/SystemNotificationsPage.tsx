// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faPlus, faPen, faTrash } from 'Icons'
import React, { useContext, useState } from 'react'

import { useTranslation } from 'employee-frontend/state/i18n'
import { localDate, localTime } from 'lib-common/form/fields'
import {
  object,
  required,
  transformed,
  validated,
  value
} from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { ValidationSuccess } from 'lib-common/form/types'
import { nonBlank } from 'lib-common/form/validators'
import {
  SystemNotification,
  SystemNotificationTargetGroup
} from 'lib-common/generated/api-types/systemnotifications'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import MutateButton, {
  InlineMutateButton
} from 'lib-components/atoms/buttons/MutateButton'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import { Container, ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { H1, H2, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'

import {
  allSystemNotificationsQuery,
  deleteSystemNotificationMutation,
  putSystemNotificationMutation
} from './queries'

const notificationForm = transformed(
  object({
    targetGroup: required(value<SystemNotificationTargetGroup>()),
    text: validated(required(value<string>()), nonBlank),
    validToDate: required(localDate()),
    validToTime: required(localTime())
  }),
  ({ targetGroup, text, validToDate, validToTime }) => {
    const notification: SystemNotification = {
      targetGroup,
      text,
      validTo: HelsinkiDateTime.fromLocal(validToDate, validToTime)
    }
    return ValidationSuccess.of(notification)
  }
)

const NotificationEditor = React.memo(function NotificationEditor({
  notification,
  onClose
}: {
  notification: SystemNotification
  onClose: () => void
}) {
  const { i18n, lang } = useTranslation()
  const form = useForm(
    notificationForm,
    () => ({
      targetGroup: notification.targetGroup,
      text: notification.text,
      validToDate: localDate.fromDate(notification.validTo.toLocalDate()),
      validToTime: notification.validTo.toLocalTime().format()
    }),
    i18n.validationErrors
  )
  const { text, validToDate, validToTime } = useFormFields(form)

  return (
    <FixedSpaceColumn>
      <Label>{i18n.systemNotifications.text}</Label>
      <InputFieldF bind={text} data-qa="text-input" />
      <FixedSpaceRow>
        <Label>{i18n.systemNotifications.validTo}</Label>
        <DatePickerF bind={validToDate} locale={lang} data-qa="date-input" />
        <TimeInputF bind={validToTime} data-qa="time-input" />
      </FixedSpaceRow>
      <FixedSpaceRow>
        <LegacyButton text={i18n.common.cancel} onClick={onClose} />
        <MutateButton
          primary
          text={i18n.common.save}
          disabled={!form.isValid()}
          mutation={putSystemNotificationMutation}
          onClick={() => ({ body: form.value() })}
          onSuccess={onClose}
          data-qa="save-btn"
        />
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
})

const SystemNotificationsPageInner = React.memo(
  function SystemNotificationsPageInner({
    notifications
  }: {
    notifications: SystemNotification[]
  }) {
    const { i18n } = useTranslation()
    const { user } = useContext(UserContext)
    const editAllowed =
      user?.permittedGlobalActions?.includes('UPDATE_SYSTEM_NOTIFICATION') ??
      false

    const [editedNotification, setEditedNotification] =
      useState<SystemNotificationTargetGroup | null>(null)

    const targetGroups: SystemNotificationTargetGroup[] = [
      'CITIZENS',
      'EMPLOYEES'
    ]

    return (
      <FixedSpaceColumn spacing="XL">
        {targetGroups.map((targetGroup) => {
          const notification =
            notifications.find((n) => n.targetGroup === targetGroup) ?? null

          return (
            <FixedSpaceColumn
              key={targetGroup}
              data-qa={`notification-${targetGroup}`}
            >
              <H2 noMargin>{i18n.systemNotifications.title[targetGroup]}</H2>
              {editedNotification === targetGroup ? (
                <NotificationEditor
                  notification={
                    notification ?? {
                      targetGroup: targetGroup,
                      text: '',
                      validTo: HelsinkiDateTime.now().addHours(24)
                    }
                  }
                  onClose={() => setEditedNotification(null)}
                />
              ) : notification ? (
                <FixedSpaceColumn>
                  <P noMargin>{notification.text}</P>
                  <div>
                    <Label>{i18n.systemNotifications.validTo}: </Label>
                    <span>{notification.validTo.format()}</span>
                  </div>
                  {editAllowed && (
                    <FixedSpaceRow>
                      <Button
                        appearance="inline"
                        onClick={() => setEditedNotification(targetGroup)}
                        text={i18n.common.edit}
                        icon={faPen}
                        disabled={editedNotification !== null}
                      />
                      <InlineMutateButton
                        mutation={deleteSystemNotificationMutation}
                        onClick={() => ({ targetGroup })}
                        text={i18n.common.remove}
                        icon={faTrash}
                        disabled={editedNotification !== null}
                      />
                    </FixedSpaceRow>
                  )}
                </FixedSpaceColumn>
              ) : (
                <FixedSpaceColumn>
                  <div>{i18n.systemNotifications.noNotification}</div>
                  {editAllowed && (
                    <Button
                      appearance="inline"
                      onClick={() => setEditedNotification(targetGroup)}
                      text={i18n.systemNotifications.setNotification}
                      icon={faPlus}
                      disabled={editedNotification !== null}
                      data-qa="create-btn"
                    />
                  )}
                </FixedSpaceColumn>
              )}
            </FixedSpaceColumn>
          )
        })}
      </FixedSpaceColumn>
    )
  }
)

export default React.memo(function SystemNotificationsPage() {
  const { i18n } = useTranslation()

  const result = useQueryResult(allSystemNotificationsQuery())

  return (
    <Container>
      <ContentArea opaque>
        <H1>{i18n.titles.systemNotifications}</H1>
        <Gap />
        {renderResult(result, (notifications) => (
          <SystemNotificationsPageInner notifications={notifications} />
        ))}
      </ContentArea>
    </Container>
  )
})
