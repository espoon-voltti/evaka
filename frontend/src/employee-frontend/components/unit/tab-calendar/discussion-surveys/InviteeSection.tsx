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

const ReservationCount = styled.span`
  font-weight: ${fontWeights.bold};
`

const getChildUrl = (c: { id: string }) => `/child-information/${c.id}`

const ChildNameList = React.memo(function ChildNameList({
  childList
}: {
  childList: ChildBasics[]
}) {
  return (
    <p>
      {childList.map((item, index) => (
        <span key={item.id}>
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
  reserved: ChildBasics[]
  unreserved: ChildBasics[]
}) {
  const { i18n } = useTranslation()
  const t = i18n.unit.calendar.events.discussionReservation

  return (
    <FixedSpaceRow spacing="XL" fullWidth justifyContent="space-between">
      <FixedSpaceColumn fullWidth>
        <ReservationCount>{`${t.unreservedTitle} (${unreserved.length}/${unreserved.length + reserved.length})`}</ReservationCount>
        <ChildNameList childList={unreserved} />
      </FixedSpaceColumn>
      <FixedSpaceColumn fullWidth>
        <ReservationCount>{`${t.reservedTitle} (${reserved.length}/${unreserved.length + reserved.length})`}</ReservationCount>
        <ChildNameList childList={reserved} />
      </FixedSpaceColumn>
    </FixedSpaceRow>
  )
})
