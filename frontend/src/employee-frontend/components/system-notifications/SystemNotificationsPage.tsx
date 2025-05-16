// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useState } from 'react'

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
  SystemNotificationCitizens,
  SystemNotificationEmployees,
  SystemNotificationTargetGroup
} from 'lib-common/generated/api-types/systemnotifications'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { TextAreaF } from 'lib-components/atoms/form/TextArea'
import { TimeInputF } from 'lib-components/atoms/form/TimeInput'
import { Container, ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { H1, H2, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPen, faPlus, faTrash } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'

import {
  allSystemNotificationsQuery,
  deleteSystemNotificationMutation,
  putSystemNotificationCitizensMutation,
  putSystemNotificationEmployeesMutation
} from './queries'

const citizenNotificationForm = transformed(
  object({
    text: validated(required(value<string>()), nonBlank),
    textSv: validated(required(value<string>()), nonBlank),
    textEn: validated(required(value<string>()), nonBlank),
    validToDate: required(localDate()),
    validToTime: required(localTime())
  }),
  ({ text, textSv, textEn, validToDate, validToTime }) => {
    const notification: SystemNotificationCitizens = {
      text,
      textSv,
      textEn,
      validTo: HelsinkiDateTime.fromLocal(validToDate, validToTime)
    }
    return ValidationSuccess.of(notification)
  }
)

const employeeNotificationForm = transformed(
  object({
    text: validated(required(value<string>()), nonBlank),
    validToDate: required(localDate()),
    validToTime: required(localTime())
  }),
  ({ text, validToDate, validToTime }) => {
    const notification: SystemNotificationEmployees = {
      text,
      validTo: HelsinkiDateTime.fromLocal(validToDate, validToTime)
    }
    return ValidationSuccess.of(notification)
  }
)

const CitizenNotificationEditor = React.memo(function NotificationEditor({
  notification,
  onClose
}: {
  notification: SystemNotificationCitizens
  onClose: () => void
}) {
  const { i18n, lang } = useTranslation()
  const form = useForm(
    citizenNotificationForm,
    () => ({
      text: notification.text,
      textSv: notification.textSv,
      textEn: notification.textEn,
      validToDate: localDate.fromDate(notification.validTo.toLocalDate()),
      validToTime: notification.validTo.toLocalTime().format()
    }),
    i18n.validationErrors
  )
  const { text, textSv, textEn, validToDate, validToTime } = useFormFields(form)

  return (
    <FixedSpaceColumn spacing="L">
      <FixedSpaceColumn spacing="zero">
        <Label>{i18n.systemNotifications.textFi}</Label>
        <TextAreaF bind={text} data-qa="text-input" />
      </FixedSpaceColumn>
      <FixedSpaceColumn spacing="zero>">
        <Label>{i18n.systemNotifications.textSv}</Label>
        <TextAreaF bind={textSv} data-qa="text-input-sv" />
      </FixedSpaceColumn>
      <FixedSpaceColumn spacing="zero">
        <Label>{i18n.systemNotifications.textEn}</Label>
        <TextAreaF bind={textEn} data-qa="text-input-en" />
      </FixedSpaceColumn>
      <FixedSpaceRow>
        <Label>{i18n.systemNotifications.validTo}</Label>
        <DatePickerF bind={validToDate} locale={lang} data-qa="date-input" />
        <TimeInputF bind={validToTime} data-qa="time-input" />
      </FixedSpaceRow>
      <FixedSpaceRow>
        <Button text={i18n.common.cancel} onClick={onClose} />
        <MutateButton
          primary
          text={i18n.common.save}
          disabled={!form.isValid()}
          mutation={putSystemNotificationCitizensMutation}
          onClick={() => ({ body: form.value() })}
          onSuccess={onClose}
          data-qa="save-btn"
        />
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
})

const EmployeeNotificationEditor = React.memo(function NotificationEditor({
  notification,
  onClose
}: {
  notification: SystemNotificationEmployees
  onClose: () => void
}) {
  const { i18n, lang } = useTranslation()
  const form = useForm(
    employeeNotificationForm,
    () => ({
      text: notification.text,
      validToDate: localDate.fromDate(notification.validTo.toLocalDate()),
      validToTime: notification.validTo.toLocalTime().format()
    }),
    i18n.validationErrors
  )
  const { text, validToDate, validToTime } = useFormFields(form)

  return (
    <FixedSpaceColumn spacing="L">
      <FixedSpaceColumn spacing="zero">
        <Label>{i18n.systemNotifications.text}</Label>
        <InputFieldF bind={text} data-qa="text-input" />
      </FixedSpaceColumn>
      <FixedSpaceRow>
        <Label>{i18n.systemNotifications.validTo}</Label>
        <DatePickerF bind={validToDate} locale={lang} data-qa="date-input" />
        <TimeInputF bind={validToTime} data-qa="time-input" />
      </FixedSpaceRow>
      <FixedSpaceRow>
        <Button text={i18n.common.cancel} onClick={onClose} />
        <MutateButton
          primary
          text={i18n.common.save}
          disabled={!form.isValid()}
          mutation={putSystemNotificationEmployeesMutation}
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
    citizenNotification,
    employeeNotification
  }: {
    citizenNotification: SystemNotificationCitizens | null
    employeeNotification: SystemNotificationEmployees | null
  }) {
    const { i18n } = useTranslation()
    const { user } = useContext(UserContext)
    const editAllowed =
      user?.permittedGlobalActions?.includes('UPDATE_SYSTEM_NOTIFICATION') ??
      false

    const [editedNotification, setEditedNotification] =
      useState<SystemNotificationTargetGroup | null>(null)

    return (
      <FixedSpaceColumn spacing="XL">
        <FixedSpaceColumn data-qa="notification-CITIZENS">
          <H2 noMargin>{i18n.systemNotifications.title.CITIZENS}</H2>
          {editedNotification === 'CITIZENS' ? (
            <CitizenNotificationEditor
              notification={
                citizenNotification ?? {
                  text: '',
                  textSv: '',
                  textEn: '',
                  validTo: HelsinkiDateTime.now().addHours(24)
                }
              }
              onClose={() => setEditedNotification(null)}
            />
          ) : citizenNotification ? (
            <FixedSpaceColumn>
              <FixedSpaceColumn spacing="xs">
                <Label>{i18n.systemNotifications.textFi}</Label>
                <P noMargin>
                  {citizenNotification.text.split('\n').map((s, i) => (
                    <Fragment key={i}>
                      {s}
                      <br />
                    </Fragment>
                  ))}
                </P>
              </FixedSpaceColumn>
              <FixedSpaceColumn spacing="xs">
                <Label>{i18n.systemNotifications.textSv}</Label>
                <P noMargin>
                  {citizenNotification.textSv.split('\n').map((s, i) => (
                    <Fragment key={i}>
                      {s}
                      <br />
                    </Fragment>
                  ))}
                </P>
              </FixedSpaceColumn>
              <FixedSpaceColumn spacing="xs">
                <Label>{i18n.systemNotifications.textEn}</Label>
                <P noMargin>
                  {citizenNotification.textEn.split('\n').map((s, i) => (
                    <Fragment key={i}>
                      {s}
                      <br />
                    </Fragment>
                  ))}
                </P>
              </FixedSpaceColumn>
              <div>
                <Label>{i18n.systemNotifications.validTo}: </Label>
                <span>{citizenNotification.validTo.format()}</span>
              </div>
              {editAllowed && (
                <FixedSpaceRow>
                  <Button
                    appearance="inline"
                    onClick={() => setEditedNotification('CITIZENS')}
                    text={i18n.common.edit}
                    icon={faPen}
                    disabled={editedNotification !== null}
                  />
                  <MutateButton
                    appearance="inline"
                    mutation={deleteSystemNotificationMutation}
                    onClick={() => ({ targetGroup: 'CITIZENS' as const })}
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
                  onClick={() => setEditedNotification('CITIZENS')}
                  text={i18n.systemNotifications.setNotification}
                  icon={faPlus}
                  disabled={editedNotification !== null}
                  data-qa="create-btn"
                />
              )}
            </FixedSpaceColumn>
          )}
        </FixedSpaceColumn>

        <FixedSpaceColumn data-qa="notification-EMPLOYEES">
          <H2 noMargin>{i18n.systemNotifications.title.EMPLOYEES}</H2>
          {editedNotification === 'EMPLOYEES' ? (
            <EmployeeNotificationEditor
              notification={
                employeeNotification ?? {
                  text: '',
                  validTo: HelsinkiDateTime.now().addHours(24)
                }
              }
              onClose={() => setEditedNotification(null)}
            />
          ) : employeeNotification ? (
            <FixedSpaceColumn>
              <FixedSpaceColumn spacing="xs">
                <Label>{i18n.systemNotifications.text}</Label>
                <P noMargin>{employeeNotification.text}</P>
              </FixedSpaceColumn>
              <div>
                <Label>{i18n.systemNotifications.validTo}: </Label>
                <span>{employeeNotification.validTo.format()}</span>
              </div>
              {editAllowed && (
                <FixedSpaceRow>
                  <Button
                    appearance="inline"
                    onClick={() => setEditedNotification('EMPLOYEES')}
                    text={i18n.common.edit}
                    icon={faPen}
                    disabled={editedNotification !== null}
                  />
                  <MutateButton
                    appearance="inline"
                    mutation={deleteSystemNotificationMutation}
                    onClick={() => ({ targetGroup: 'EMPLOYEES' as const })}
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
                  onClick={() => setEditedNotification('EMPLOYEES')}
                  text={i18n.systemNotifications.setNotification}
                  icon={faPlus}
                  disabled={editedNotification !== null}
                  data-qa="create-btn"
                />
              )}
            </FixedSpaceColumn>
          )}
        </FixedSpaceColumn>
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
        {renderResult(result, ({ citizens, employees }) => (
          <SystemNotificationsPageInner
            citizenNotification={citizens}
            employeeNotification={employees}
          />
        ))}
      </ContentArea>
    </Container>
  )
})
