// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faX } from '@fortawesome/free-solid-svg-icons'
import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { UIContext } from 'employee-frontend/state/ui'
import { mapped, object, value } from 'lib-common/form/form'
import { useForm } from 'lib-common/form/hooks'
import {
  CalendarEvent,
  CalendarEventTime
} from 'lib-common/generated/api-types/calendarevent'
import { ChildBasics } from 'lib-common/generated/api-types/placement'
import { useMutation } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import LegacyInlineButton from 'lib-components/atoms/buttons/LegacyInlineButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import BaseModal from 'lib-components/molecules/modals/BaseModal'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2, H3, Label, fontWeights } from 'lib-components/typography'
import { Gap, defaultMargins } from 'lib-components/white-space'
import { faPlus, faQuestion, faTrash } from 'lib-icons'

import {
  deleteCalendarEventTimeMutation,
  setCalendarEventTimeReservationMutation
} from '../../queries'

const ReservationRow = styled(FixedSpaceColumn)`
  border-left: 6px solid transparent;
  padding: ${defaultMargins.xxs} calc(${defaultMargins.s} - 6px);
  border-color: ${(p) => p.theme.colors.accents.a6turquoise};
  margin-top: ${defaultMargins.xs};

  &.unreserved {
    border-color: ${(p) => p.theme.colors.accents.a6turquoise};
  }

  &.reserved {
    background-color: ${(p) => p.theme.colors.main.m4};
    border-color: ${(p) => p.theme.colors.main.m3};
  }
`
const TimeSpan = styled.span`
  text-wrap: nowrap;
  font-weight: ${fontWeights.bold};
`

const ChildLinkButton = styled(LegacyInlineButton)`
  text-wrap: pretty;
  text-align: start;
`
export default React.memo(function CalendarEventTimeReservation({
  eventTime,
  reservationChild,
  reserveAction
}: {
  eventTime: CalendarEventTime
  reservationChild?: ChildBasics
  reserveAction: (time: CalendarEventTime) => void
}) {
  const { i18n } = useTranslation()
  return (
    <ReservationRow
      spacing="xs"
      alignItems="flex-start"
      className={reservationChild ? 'reserved' : 'unreserved'}
      data-qa="reservation-row"
    >
      <TimeSpan data-qa="event-time-range">{`${eventTime.startTime.format()} – ${eventTime.endTime.format()}`}</TimeSpan>
      <ChildLinkButton
        onClick={() => reserveAction(eventTime)}
        text={
          reservationChild
            ? `${reservationChild.firstName}  ${reservationChild.lastName}`
            : i18n.unit.calendar.events.discussionReservation.reserveButton
        }
        data-qa="reserve-event-time-button"
      />
    </ReservationRow>
  )
})

export const DiscussionReservationModal = React.memo(
  function DiscussionReservationModal({
    eventTime,
    eventData,
    invitees,
    onClose
  }: {
    eventTime: CalendarEventTime
    eventData: CalendarEvent
    invitees: ChildBasics[]
    onClose: (shouldRefresh: boolean) => void
  }) {
    const { i18n } = useTranslation()
    const t = i18n.unit.calendar.events.discussionReservation

    const [viewMode, setViewMode] = useState<'free' | 'select' | 'reserved'>(
      eventTime.childId ? 'reserved' : 'free'
    )

    const mappedForm = mapped(
      object({
        childId: value<UUID | null>()
      }),
      (output) => ({
        ...output
      })
    )

    const reservationChild = useForm(
      mappedForm,
      () => ({
        childId: eventTime.childId
      }),
      i18n.validationErrors
    )

    const [cancelConfirmModalVisible, setCancelConfirmModalVisible] =
      useState(false)
    const [deleteConfirmModalVisible, setDeleteConfirmModalVisible] =
      useState(false)

    const cancelChanges = useCallback(() => {
      if (reservationChild.state.childId !== eventTime.childId) {
        setCancelConfirmModalVisible(true)
      } else {
        onClose(false)
      }
    }, [
      reservationChild.state,
      eventTime.childId,
      setCancelConfirmModalVisible,
      onClose
    ])

    const { mutateAsync: deleteCalendarEventTime } = useMutation(
      deleteCalendarEventTimeMutation
    )

    const { setErrorMessage } = useContext(UIContext)

    const deleteEventTime = useCallback(() => {
      deleteCalendarEventTime({
        eventId: eventData.id,
        id: eventTime.id
      })
        .then(() => onClose(true))
        .catch(() =>
          setErrorMessage({
            type: 'error',
            title:
              i18n.unit.calendar.events.discussionReservation.reservationModal
                .deleteError,
            resolveLabel: i18n.common.ok
          })
        )
    }, [
      eventData,
      eventTime,
      deleteCalendarEventTime,
      setErrorMessage,
      onClose,
      i18n
    ])

    const savedChild = useMemo(
      () => invitees.find((i) => i.id === eventTime.childId),
      [eventTime.childId, invitees]
    )

    const sortedInvitees = useMemo(
      () =>
        orderBy(invitees, [(i) => i.lastName, (i) => i.firstName, (i) => i.id]),
      [invitees]
    )

    return (
      <>
        <BaseModal
          title={eventData.title}
          type="info"
          width="wide"
          close={cancelChanges}
          closeLabel={i18n.common.closeModal}
          padding="L"
          data-qa="reservation-modal"
        >
          <H2
            noMargin
          >{`${i18n.common.datetime.weekdaysShort[eventTime.date.getIsoDayOfWeek() - 1]} ${eventTime.date.format()}`}</H2>
          <H2
            noMargin
          >{`${eventTime.startTime.format()} - ${eventTime.endTime.format()}`}</H2>
          <H3>{`${t.reservationModal.reservationStatus}: ${reservationChild.state.childId ? t.reservationModal.reserved : t.reservationModal.unreserved}`}</H3>

          {viewMode === 'free' && (
            <LegacyInlineButton
              icon={faPlus}
              text={
                i18n.unit.calendar.events.discussionReservation.reserveButton
              }
              onClick={() => setViewMode('select')}
              data-qa="add-reservation-select-button"
            />
          )}

          {viewMode === 'select' && (
            <FixedSpaceRow alignItems="center">
              <Label>{t.reservationModal.inviteeLabel}</Label>
              <Select
                selectedItem={
                  invitees.find(
                    (i) => i.id === reservationChild.state.childId
                  ) ?? null
                }
                items={sortedInvitees}
                onChange={(item) =>
                  reservationChild.set({ childId: item?.id ?? null })
                }
                getItemValue={(i: ChildBasics) => (i ? i.id : '')}
                getItemLabel={(i: ChildBasics) =>
                  i ? `${i.lastName} ${i.firstName}` : ''
                }
                placeholder={t.reservationModal.selectPlaceholder}
                data-qa="reservee-select"
              />
              <LegacyInlineButton
                text={i18n.common.remove}
                icon={faX}
                onClick={() => {
                  reservationChild.set({ childId: null })
                  setViewMode('free')
                }}
              />
            </FixedSpaceRow>
          )}

          {viewMode === 'reserved' && (
            <FixedSpaceRow alignItems="center">
              <span>
                {savedChild
                  ? `${savedChild.firstName} ${savedChild.lastName}`
                  : ''}
              </span>
              <LegacyInlineButton
                icon={faX}
                text={i18n.common.remove}
                onClick={() => {
                  reservationChild.set({ childId: null })
                  setViewMode('free')
                }}
              />
            </FixedSpaceRow>
          )}

          <Gap size="X5L" />
          <FixedSpaceRow justifyContent="space-between">
            <LegacyInlineButton
              text={i18n.common.remove}
              onClick={() => {
                if (savedChild) {
                  setDeleteConfirmModalVisible(true)
                } else {
                  deleteCalendarEventTime({
                    eventId: eventData.id,
                    id: eventTime.id
                  })
                    .then(() => onClose(true))
                    .catch(() =>
                      setErrorMessage({
                        type: 'error',
                        title: t.reservationModal.deleteError,
                        resolveLabel: i18n.common.ok
                      })
                    )
                }
              }}
              data-qa="delete-reservation-button"
              icon={faTrash}
            />

            <FixedSpaceRow justifyContent="flex-end" spacing="s">
              <LegacyButton
                onClick={cancelChanges}
                data-qa="cancel"
                text={i18n.common.cancel}
              />
              <MutateButton
                primary
                onClick={() => ({
                  body: {
                    calendarEventTimeId: eventTime.id,
                    childId: reservationChild.value().childId
                  },
                  eventId: eventData.id
                })}
                onSuccess={() => onClose(true)}
                text={i18n.unit.calendar.events.edit.saveChanges}
                mutation={setCalendarEventTimeReservationMutation}
                data-qa="submit-reservation-button"
                disabled={reservationChild.state.childId === eventTime.childId}
              />
            </FixedSpaceRow>
          </FixedSpaceRow>
          <Gap size="L" />
        </BaseModal>

        {cancelConfirmModalVisible && (
          <InfoModal
            type="warning"
            title={t.cancelConfirmation.title}
            icon={faQuestion}
            reject={{
              action: () => setCancelConfirmModalVisible(false),
              label: t.cancelConfirmation.cancelButton
            }}
            resolve={{
              action: () => {
                setCancelConfirmModalVisible(false)
                onClose(false)
              },
              label: t.cancelConfirmation.continueButton
            }}
            text={t.cancelConfirmation.text}
          />
        )}
        {deleteConfirmModalVisible && (
          <InfoModal
            type="warning"
            title={t.reservationModal.deleteConfirmation.title}
            icon={faQuestion}
            reject={{
              action: () => setDeleteConfirmModalVisible(false),
              label: t.reservationModal.deleteConfirmation.cancelButton
            }}
            resolve={{
              action: () => {
                setDeleteConfirmModalVisible(false)
                deleteEventTime()
              },
              label: t.reservationModal.deleteConfirmation.continueButton
            }}
            text={t.reservationModal.deleteConfirmation.text}
            data-qa="delete-confirmation-modal"
          />
        )}
      </>
    )
  }
)
