// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import partition from 'lodash/partition'
import React, { useMemo } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import DateRange from 'lib-common/date-range'
import { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { UnitGroupDetails } from 'lib-common/generated/api-types/daycare'
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
  unitDetails,
  eventData
}: {
  unitDetails: UnitGroupDetails
  eventData: CalendarEvent
}) {
  const [reserved, unreserved] = useMemo(() => {
    const { groups, period, individualChildren, times } = eventData
    const fullGroupSelections = groups.filter((g) => {
      const anyIndividuals =
        individualChildren.length > 0 &&
        eventData.individualChildren.some((c) => c.groupId === g.id)
      return !anyIndividuals
    })
    const childSelections = individualChildren.map((c) => c.id)
    const reservations = times.filter((t) => t.childId !== null)

    const allInvitedChildren = unitDetails.placements
      .filter((p) => {
        const isPartOfFullGroupSelection = p.groupPlacements.some((gp) => {
          const placedGroupIsInFullGroups = fullGroupSelections.some(
            (gi) => gi.id === gp.groupId
          )
          const durationsOverlap = period.overlaps(
            new DateRange(gp.startDate, gp.endDate)
          )
          return placedGroupIsInFullGroups && durationsOverlap
        })
        const isPartOfIndividualSelections = childSelections.includes(
          p.child.id
        )
        return isPartOfFullGroupSelection || isPartOfIndividualSelections
      })
      .map((p) => p.child)

    const sortedResults = orderBy(allInvitedChildren, [
      (c) => c.lastName,
      (c) => c.firstName
    ])
    return partition(sortedResults, (e) =>
      reservations.some((r) => r.childId === e.id)
    )
  }, [unitDetails, eventData])

  return (
    <FixedSpaceRow spacing="XL" fullWidth justifyContent="space-between">
      <FixedSpaceColumn fullWidth>
        <ReservationCount>{`Varaamatta (${unreserved.length}/${unreserved.length + reserved.length})`}</ReservationCount>
        <ChildNameList childList={unreserved} />
      </FixedSpaceColumn>
      <FixedSpaceColumn fullWidth>
        <ReservationCount>{`Varanneet (${reserved.length}/${unreserved.length + reserved.length})`}</ReservationCount>
        <ChildNameList childList={reserved} />
      </FixedSpaceColumn>
    </FixedSpaceRow>
  )
})
