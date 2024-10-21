// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faX } from '@fortawesome/free-solid-svg-icons'
import orderBy from 'lodash/orderBy'
import partition from 'lodash/partition'
import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import {
  CalendarEvent,
  CalendarEventTime
} from 'lib-common/generated/api-types/calendarevent'
import { ChildBasics } from 'lib-common/generated/api-types/placement'
import { UUID } from 'lib-common/types'
import { Button } from 'lib-components/atoms/buttons/Button'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { clearChildCalendarEventTimeReservationsForSurveyMutation } from '../queries'

import { ChildGroupInfo } from './DiscussionSurveyView'

const ReservationCount = styled.span`
  font-weight: ${fontWeights.bold};
`

const getChildUrl = (c: { id: string }) => `/child-information/${c.id}`
type InviteeInfo = { child: ChildBasics; reservations: CalendarEventTime[] }
const ChildNameList = React.memo(function ChildNameList({
  eventId,
  childList,
  'data-qa': dataQa
}: {
  eventId: UUID
  childList: InviteeInfo[]
  'data-qa'?: string
}) {
  const { i18n } = useTranslation()
  const [selectedInvitee, setSelectedInvitee] = useState<InviteeInfo | null>(
    null
  )
  return (
    <InviteeGrid data-qa={dataQa}>
      {selectedInvitee && (
        <MutateFormModal
          resolveMutation={
            clearChildCalendarEventTimeReservationsForSurveyMutation
          }
          title={
            i18n.unit.calendar.events.discussionReservation
              .reservationClearConfirmationTitle
          }
          resolveAction={() => ({
            body: {
              childId: selectedInvitee.child.id,
              calendarEventId: eventId
            },
            eventId
          })}
          resolveLabel={i18n.common.remove}
          onSuccess={() => {
            setSelectedInvitee(null)
          }}
          rejectAction={() => setSelectedInvitee(null)}
          rejectLabel={i18n.common.cancel}
          data-qa="clear-reservations-confirmation-modal"
        >
          <CenteringDiv>
            <h3 data-qa="child-name">{`${selectedInvitee.child.firstName} ${selectedInvitee.child.lastName}`}</h3>
            {selectedInvitee.reservations.map((r, i) => (
              <p
                key={r.id}
                data-qa={`reservation-datetime-${i}`}
              >{`${r.date.format()}: ${r.startTime.format()} - ${r.endTime.format()}`}</p>
            ))}
          </CenteringDiv>
        </MutateFormModal>
      )}
      {childList.map((item) => (
        <React.Fragment key={item.child.id}>
          <div>
            <Link
              to={getChildUrl(item.child)}
              data-qa={`attendee-${item.child.id}`}
            >
              {`${item.child.firstName.split(' ')[0]} ${item.child.lastName}`}
            </Link>
          </div>
          {item.reservations.length > 0 ? (
            <div>
              <Button
                appearance="inline"
                icon={faX}
                onClick={() => setSelectedInvitee(item)}
                text={
                  i18n.unit.calendar.events.discussionReservation
                    .clearReservationButtonLabel
                }
                data-qa={`clear-reservations-button-${item.child.id}`}
              />
            </div>
          ) : (
            <div />
          )}
        </React.Fragment>
      ))}
    </InviteeGrid>
  )
})

const CenteringDiv = styled.div`
  text-align: center;
`

const InviteeGrid = styled.div`
  padding: ${defaultMargins.s};
  display: grid;
  grid-template-columns: [name] 60% [button] 40%;
  gap: 4px;
`

export default React.memo(function InviteeSection({
  invitees,
  event
}: {
  invitees: ChildGroupInfo[]
  event: CalendarEvent
}) {
  const { i18n } = useTranslation()
  const t = i18n.unit.calendar.events.discussionReservation

  const childrenWithReservations = invitees.map((i) => ({
    child: i.child,
    reservations: orderBy(
      event.times.filter((t) => t.childId === i.child.id),
      [(t) => t.date, (t) => t.startTime, (t) => t.endTime]
    )
  }))
  const [reserved, unreserved] = partition(
    childrenWithReservations,
    (c) => c.reservations.length > 0
  )

  return (
    <FixedSpaceRow spacing="XL" fullWidth justifyContent="space-between">
      <FixedSpaceColumn fullWidth spacing="xs">
        <ReservationCount>{`${t.unreservedTitle} (${unreserved.length}/${unreserved.length + reserved.length})`}</ReservationCount>
        <ChildNameList
          childList={unreserved}
          eventId={event.id}
          data-qa="unreserved-attendees"
        />
      </FixedSpaceColumn>
      <FixedSpaceColumn fullWidth spacing="xs">
        <ReservationCount>{`${t.reservedTitle} (${reserved.length}/${unreserved.length + reserved.length})`}</ReservationCount>
        <ChildNameList
          childList={reserved}
          eventId={event.id}
          data-qa="reserved-attendees"
        />
      </FixedSpaceColumn>
    </FixedSpaceRow>
  )
})
