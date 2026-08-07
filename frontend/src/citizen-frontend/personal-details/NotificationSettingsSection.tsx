// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useRef, useEffect } from 'react'
import styled from 'styled-components'

import { boolean } from 'lib-common/form/fields'
import { object } from 'lib-common/form/form'
import { useBoolean, useForm, useFormFields } from 'lib-common/form/hooks'
import type { BoundFormState } from 'lib-common/form/hooks'
import type { StateOf } from 'lib-common/form/types'
import type { EmailMessageType } from 'lib-common/generated/api-types/pis'
import { emailMessageTypes } from 'lib-common/generated/api-types/pis'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'
import { faChevronDown, faChevronUp, faEnvelope } from 'lib-icons'

import { useTranslation } from '../localization'

import { EditableSectionHeader } from './components'
import { updateNotificationSettingsMutation } from './queries'

const notificationSettingsForm = object({
  message: boolean(),
  bulletin: boolean(),
  income: boolean(),
  calendarEvent: boolean(),
  decision: boolean(),
  document: boolean(),
  informalDocument: boolean(),
  attendanceReservation: boolean(),
  discussionTime: boolean()
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
    case 'INCOME_NOTIFICATION':
      return state.income
    case 'CALENDAR_EVENT_NOTIFICATION':
      return state.calendarEvent
    case 'DECISION_NOTIFICATION':
      return state.decision
    case 'DOCUMENT_NOTIFICATION':
      return state.document
    case 'INFORMAL_DOCUMENT_NOTIFICATION':
      return state.informalDocument
    case 'ATTENDANCE_RESERVATION_NOTIFICATION':
      return state.attendanceReservation
    case 'DISCUSSION_TIME_NOTIFICATION':
      return state.discussionTime
  }
}

const getInitialState = (
  disabledTypes: EmailMessageType[]
): StateOf<typeof notificationSettingsForm> => ({
  message: !disabledTypes.includes('MESSAGE_NOTIFICATION'),
  bulletin: !disabledTypes.includes('BULLETIN_NOTIFICATION'),
  income: !disabledTypes.includes('INCOME_NOTIFICATION'),
  calendarEvent: !disabledTypes.includes('CALENDAR_EVENT_NOTIFICATION'),
  decision: !disabledTypes.includes('DECISION_NOTIFICATION'),
  document: !disabledTypes.includes('DOCUMENT_NOTIFICATION'),
  informalDocument: !disabledTypes.includes('INFORMAL_DOCUMENT_NOTIFICATION'),
  attendanceReservation: !disabledTypes.includes(
    'ATTENDANCE_RESERVATION_NOTIFICATION'
  ),
  discussionTime: !disabledTypes.includes('DISCUSSION_TIME_NOTIFICATION')
})

const channelColumns = '60px'

const TableHeaderRow = styled.div`
  display: grid;
  grid-template-columns: 1fr ${channelColumns};
  gap: ${defaultMargins.s};
  align-items: end;
  font-weight: 600;
  padding-bottom: ${defaultMargins.xs};
  border-bottom: 2px solid ${(p) => p.theme.colors.grayscale.g15};
`

const ChannelHeader = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  gap: ${defaultMargins.xxs};
  text-align: center;
  hyphens: auto;
  color: ${(p) => p.theme.colors.grayscale.g100};
`

const SettingRow = styled.div`
  display: grid;
  grid-template-columns: 1fr ${channelColumns};
  column-gap: ${defaultMargins.s};
  align-items: start;
  padding: ${defaultMargins.s} 0;
  border-bottom: 1px solid ${(p) => p.theme.colors.grayscale.g15};

  &:last-child {
    border-bottom: none;
  }
`

const RowInfoCell = styled.div`
  grid-column: 1 / -1;
`

const ChannelCell = styled.div`
  display: flex;
  justify-content: center;
`

interface NotificationRow {
  dataQa: string
  label: string
  info?: React.ReactNode
  bind: BoundFormState<boolean>
}

export interface Props {
  initialData: EmailMessageType[]
}

export default React.memo(
  React.forwardRef(function NotificationSettingsSection(
    { initialData }: Props,
    ref: React.Ref<HTMLDivElement>
  ) {
    const t = useTranslation()
    const tn = t.personalDetails.notificationsSection
    const [editing, useEditing] = useBoolean(false)
    const firstCheckboxRef = useRef<HTMLDivElement>(null)

    const form = useForm(
      notificationSettingsForm,
      () => getInitialState(initialData),
      t.validationErrors
    )
    const {
      message,
      bulletin,
      income,
      calendarEvent,
      decision,
      document,
      informalDocument,
      attendanceReservation,
      discussionTime
    } = useFormFields(form)

    useEffect(() => {
      if (editing) {
        const input = firstCheckboxRef.current?.querySelector('input')
        input?.focus()
      }
    }, [editing])

    const rows: NotificationRow[] = [
      { dataQa: 'message', bind: message, label: tn.message },
      { dataQa: 'bulletin', bind: bulletin, label: tn.bulletin },
      { dataQa: 'income', bind: income, label: tn.income, info: tn.incomeInfo },
      {
        dataQa: 'calendar-event',
        bind: calendarEvent,
        label: tn.calendarEvent
      },
      { dataQa: 'decision', bind: decision, label: tn.decision },
      {
        dataQa: 'document',
        bind: document,
        label: tn.document,
        info: tn.documentInfo
      },
      {
        dataQa: 'informal-document',
        bind: informalDocument,
        label: tn.informalDocument,
        info: tn.informalDocumentInfo
      },
      {
        dataQa: 'attendance-reservation',
        bind: attendanceReservation,
        label: tn.attendanceReservation,
        info: tn.attendanceReservationInfo
      },
      ...(featureFlags.discussionReservations
        ? [
            {
              dataQa: 'discussion-time',
              bind: discussionTime,
              label: tn.discussionTime,
              info: tn.discussionTimeInfo
            }
          ]
        : [])
    ]

    return (
      <div data-qa="notification-settings-section" ref={ref}>
        <EditableSectionHeader
          title={tn.title}
          editing={editing}
          onStartEditing={useEditing.on}
          onCancel={() => {
            form.set(getInitialState(initialData))
            useEditing.off()
          }}
          mutation={updateNotificationSettingsMutation}
          onSave={() => ({
            body: emailMessageTypes.filter(
              (type) => !isEnabled(form.state, type)
            )
          })}
          onSaveSuccess={useEditing.off}
        />

        <Gap $size="xs" />

        <TableHeaderRow>
          <div>{tn.subtitle}</div>
          <ChannelHeader>
            <FontAwesomeIcon size="lg" icon={faEnvelope} />
            {tn.email}
          </ChannelHeader>
        </TableHeaderRow>
        <div ref={firstCheckboxRef}>
          {rows.map((row) => (
            <React.Fragment key={row.dataQa}>
              <SettingRow>
                <div>{row.label}</div>
                <ChannelCell>
                  <CheckboxF
                    bind={row.bind}
                    label={row.label}
                    hiddenLabel
                    disabled={!editing}
                    data-qa={row.dataQa}
                  />
                </ChannelCell>
                {row.info !== undefined && (
                  <RowInfoCell>
                    <RowInfo info={row.info} />
                  </RowInfoCell>
                )}
              </SettingRow>
              {row.dataQa === 'income' && income.state === false ? (
                <>
                  <Gap $size="s" />
                  <AlertBox noMargin message={tn.incomeWarning} />
                  <Gap $size="s" />
                </>
              ) : null}
            </React.Fragment>
          ))}
        </div>
      </div>
    )
  })
)

const InfoToggleContainer = styled.div<{ $open: boolean }>`
  margin-top: ${defaultMargins.s};
  border-left: 4px solid
    ${(p) => (p.$open ? p.theme.colors.main.m2 : 'transparent')};
  padding-left: ${defaultMargins.s};
`

const InfoText = styled.p`
  margin: ${defaultMargins.xs} 0 0;
  font-weight: 600;
  font-size: 14px;
`

const InfoToggleButton = styled.button`
  display: inline-flex;
  align-items: center;
  gap: ${defaultMargins.xs};
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  font-family: inherit;
  font-size: 1rem;
  font-weight: 600;
  color: ${(p) => p.theme.colors.main.m2};
`

const RowInfo = React.memo(function RowInfo({
  info
}: {
  info: React.ReactNode
}) {
  const t = useTranslation()
  const [open, { toggle }] = useBoolean(false)
  return (
    <InfoToggleContainer $open={open}>
      <InfoToggleButton type="button" onClick={toggle} aria-expanded={open}>
        {t.personalDetails.notificationsSection.moreInfo}
        <FontAwesomeIcon icon={open ? faChevronUp : faChevronDown} />
      </InfoToggleButton>
      {open && <InfoText>{info}</InfoText>}
    </InfoToggleContainer>
  )
})
