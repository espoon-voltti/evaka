// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { ChildBasics } from 'lib-common/generated/api-types/placement'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'

import { ChildGroupInfo } from './DiscussionSurveyView'

const ReservationCount = styled.span`
  font-weight: ${fontWeights.bold};
`

const getChildUrl = (c: { id: string }) => `/child-information/${c.id}`

const ChildNameList = React.memo(function ChildNameList({
  childList,
  'data-qa': dataQa
}: {
  childList: ChildBasics[]
  'data-qa'?: string
}) {
  return (
    <p data-qa={dataQa}>
      {childList.map((item, index) => (
        <span key={item.id} data-qa={`attendee-${item.id}`}>
          <Link to={getChildUrl(item)}>
            {`${item.firstName} ${item.lastName}`}
          </Link>
          {index === childList.length - 1 ? '' : ', '}
        </span>
      ))}
    </p>
  )
})

export default React.memo(function InviteeSection({
  reserved,
  unreserved
}: {
  reserved: ChildGroupInfo[]
  unreserved: ChildGroupInfo[]
}) {
  const { i18n } = useTranslation()
  const t = i18n.unit.calendar.events.discussionReservation

  return (
    <FixedSpaceRow spacing="XL" fullWidth justifyContent="space-between">
      <FixedSpaceColumn fullWidth>
        <ReservationCount>{`${t.unreservedTitle} (${unreserved.length}/${unreserved.length + reserved.length})`}</ReservationCount>
        <ChildNameList
          childList={unreserved.map((u) => u.child)}
          data-qa="unreserved-attendees"
        />
      </FixedSpaceColumn>
      <FixedSpaceColumn fullWidth>
        <ReservationCount>{`${t.reservedTitle} (${reserved.length}/${unreserved.length + reserved.length})`}</ReservationCount>
        <ChildNameList
          childList={reserved.map((r) => r.child)}
          data-qa="reserved-attendees"
        />
      </FixedSpaceColumn>
    </FixedSpaceRow>
  )
})
