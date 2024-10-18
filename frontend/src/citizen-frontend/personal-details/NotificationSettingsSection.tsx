// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { boolean } from 'lib-common/form/fields'
import { object } from 'lib-common/form/form'
import { useBoolean, useForm, useFormFields } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import {
  EmailMessageType,
  emailMessageTypes
} from 'lib-common/generated/api-types/pis'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { H2, P, Strong } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'
import { faPen } from 'lib-icons'

import { useTranslation } from '../localization'

import { EditButtonRow } from './components'
import { updateNotificationSettingsMutation } from './queries'

const notificationSettingsForm = object({
  message: boolean(),
  bulletin: boolean(),
  bulletinFromSupervisor: boolean(),
  outdatedIncome: boolean(),
  newCustomerIncome: boolean(),
  calendarEvent: boolean(),
  decision: boolean(),
  document: boolean(),
  informalDocument: boolean(),
  missingAttendanceReservation: boolean(),
  missingHolidayAttendanceReservation: boolean(),
  discussionTimeReservationConfirmation: boolean(),
  discussionTimeReservationReminder: boolean(),
  discussionSurveyCreationNotification: boolean()
})

function isEnabled(
  state: StateOf<typeof notificationSettingsForm>,
  type: EmailMessageType
): boolean {
  switch (type) {
    case 'TRANSACTIONAL':
      return true // always enabled
    case 'MESSAGE_NOTIFICATION':
      return state.message
    case 'BULLETIN_NOTIFICATION':
      return state.bulletin
    case 'BULLETIN_FROM_SUPERVISOR_NOTIFICATION':
      return state.bulletinFromSupervisor
    case 'OUTDATED_INCOME_NOTIFICATION':
      return state.outdatedIncome
    case 'NEW_CUSTOMER_INCOME_NOTIFICATION':
      return state.newCustomerIncome
    case 'CALENDAR_EVENT_NOTIFICATION':
      return state.calendarEvent
    case 'DECISION_NOTIFICATION':
      return state.decision
    case 'DOCUMENT_NOTIFICATION':
      return state.document
    case 'INFORMAL_DOCUMENT_NOTIFICATION':
      return state.informalDocument
    case 'MISSING_ATTENDANCE_RESERVATION_NOTIFICATION':
      return state.missingAttendanceReservation
    case 'MISSING_HOLIDAY_ATTENDANCE_RESERVATION_NOTIFICATION':
      return state.missingHolidayAttendanceReservation
    case 'DISCUSSION_TIME_RESERVATION_CONFIRMATION':
      return state.discussionTimeReservationConfirmation
    case 'DISCUSSION_SURVEY_CREATION_NOTIFICATION':
      return state.discussionSurveyCreationNotification
    case 'DISCUSSION_TIME_RESERVATION_REMINDER':
      return state.discussionTimeReservationReminder
  }
}

const getInitialState = (
  disabledTypes: EmailMessageType[]
): StateOf<typeof notificationSettingsForm> => ({
  message: !disabledTypes.includes('MESSAGE_NOTIFICATION'),
  bulletin: !disabledTypes.includes('BULLETIN_NOTIFICATION'),
  bulletinFromSupervisor: !disabledTypes.includes(
    'BULLETIN_FROM_SUPERVISOR_NOTIFICATION'
  ),
  outdatedIncome: !disabledTypes.includes('OUTDATED_INCOME_NOTIFICATION'),
  newCustomerIncome: !disabledTypes.includes(
    'NEW_CUSTOMER_INCOME_NOTIFICATION'
  ),
  calendarEvent: !disabledTypes.includes('CALENDAR_EVENT_NOTIFICATION'),
  decision: !disabledTypes.includes('DECISION_NOTIFICATION'),
  document: !disabledTypes.includes('DOCUMENT_NOTIFICATION'),
  informalDocument: !disabledTypes.includes('INFORMAL_DOCUMENT_NOTIFICATION'),
  missingAttendanceReservation: !disabledTypes.includes(
    'MISSING_ATTENDANCE_RESERVATION_NOTIFICATION'
  ),
  missingHolidayAttendanceReservation: !disabledTypes.includes(
    'MISSING_HOLIDAY_ATTENDANCE_RESERVATION_NOTIFICATION'
  ),
  discussionTimeReservationConfirmation: !disabledTypes.includes(
    'DISCUSSION_TIME_RESERVATION_CONFIRMATION'
  ),
  discussionSurveyCreationNotification: !disabledTypes.includes(
    'DISCUSSION_SURVEY_CREATION_NOTIFICATION'
  ),
  discussionTimeReservationReminder: !disabledTypes.includes(
    'DISCUSSION_TIME_RESERVATION_REMINDER'
  )
})

export interface Props {
  initialData: EmailMessageType[]
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
      () => getInitialState(initialData),
      t.validationErrors
    )
    const {
      message,
      bulletin,
      bulletinFromSupervisor,
      outdatedIncome,
      newCustomerIncome,
      calendarEvent,
      decision,
      document,
      informalDocument,
      missingAttendanceReservation,
      missingHolidayAttendanceReservation,
      discussionTimeReservationConfirmation,
      discussionTimeReservationReminder,
      discussionSurveyCreationNotification
    } = useFormFields(form)

    return (
      <div data-qa="notification-settings-section" ref={ref}>
        <EditButtonRow>
          <Button
            appearance="inline"
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
        <CheckboxF
          bind={bulletinFromSupervisor}
          label={t.personalDetails.notificationsSection.bulletinFromSupervisor}
          disabled={!editing}
          data-qa="bulletin-from-supervisor"
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
          bind={newCustomerIncome}
          label={t.personalDetails.notificationsSection.newCustomerIncome}
          disabled={!editing}
          data-qa="new-customer-income"
        />
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
        <CheckboxF
          bind={missingHolidayAttendanceReservation}
          label={
            t.personalDetails.notificationsSection
              .missingHolidayAttendanceReservation
          }
          disabled={!editing}
          data-qa="missing-holiday-attendance-reservation"
        />
        <Gap size="s" />
        {featureFlags.discussionReservations && (
          <>
            <ExpandingInfo
              info={
                t.personalDetails.notificationsSection
                  .discussionTimeReservationConfirmationInfo
              }
            >
              <CheckboxF
                bind={discussionTimeReservationConfirmation}
                label={
                  t.personalDetails.notificationsSection
                    .discussionTimeReservationConfirmation
                }
                disabled={!editing}
                data-qa="discussion-time-reservation-confirmation"
              />
            </ExpandingInfo>
            <Gap size="s" />
            <ExpandingInfo
              info={
                t.personalDetails.notificationsSection
                  .discussionTimeReservationReminderInfo
              }
            >
              <CheckboxF
                bind={discussionTimeReservationReminder}
                label={
                  t.personalDetails.notificationsSection
                    .discussionTimeReservationReminder
                }
                disabled={!editing}
                data-qa="discussion-time-reservation-reminder"
              />
            </ExpandingInfo>
            <Gap size="s" />
            <ExpandingInfo
              info={
                t.personalDetails.notificationsSection
                  .discussionSurveyCreationNotificationInfo
              }
            >
              <CheckboxF
                bind={discussionSurveyCreationNotification}
                label={
                  t.personalDetails.notificationsSection
                    .discussionSurveyCreationNotification
                }
                disabled={!editing}
                data-qa="discussion-survey-creation-notification"
              />
            </ExpandingInfo>
            <Gap size="s" />
          </>
        )}
        {editing ? (
          <FixedSpaceRow justifyContent="flex-end">
            <Button
              onClick={() => {
                form.set(getInitialState(initialData))
                useEditing.off()
              }}
              text={t.common.cancel}
              data-qa="cancel"
            />
            <MutateButton
              mutation={updateNotificationSettingsMutation}
              onClick={() => ({
                body: emailMessageTypes.filter(
                  (type) => !isEnabled(form.state, type)
                )
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
