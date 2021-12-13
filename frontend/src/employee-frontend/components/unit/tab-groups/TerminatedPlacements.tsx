// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import Title from 'lib-components/atoms/Title'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import _ from 'lodash'
import React from 'react'
import { Link } from 'react-router-dom'
import CareTypeLabel, {
  careTypesFromPlacementType
} from '../../../components/common/CareTypeLabel'
import { Translations, useTranslation } from '../../../state/i18n'
import { DaycarePlacement } from '../../../types/unit'
import { formatName } from '../../../utils'
import { isPartDayPlacement } from '../../../utils/placements'
import LocalDate from 'lib-common/local-date'

function renderTerminatedPlacementRow(
  placement: DaycarePlacement,
  i18n: Translations
) {
  const groupNameToday = (): string => {
    const groupToday = placement.groupPlacements.find((gp) =>
      new FiniteDateRange(gp.startDate, gp.endDate).includes(LocalDate.today())
    )
    return groupToday?.groupName ? groupToday.groupName : ''
  }

  return (
    <Tr key={`${placement.child.id}`} data-qa="terminated-placement-row">
      <Td data-qa="child-name">
        <Link to={`/child-information/${placement.child.id}`}>
          {formatName(
            placement.child.firstName,
            placement.child.lastName,
            i18n,
            true
          )}
        </Link>
      </Td>
      <Td data-qa="child-dob">{placement.child.dateOfBirth.format()}</Td>
      <Td data-qa="placement-type">
        {placement.type ? (
          careTypesFromPlacementType(placement.type)
        ) : (
          <CareTypeLabel type="backup-care" />
        )}
      </Td>
      <Td data-qa="placement-subtype">
        {placement.type && (
          <PlacementCircle
            type={isPartDayPlacement(placement.type) ? 'half' : 'full'}
            label={placement.serviceNeeds
              .filter((sn) =>
                new FiniteDateRange(
                  placement.startDate,
                  placement.endDate
                ).overlaps(new FiniteDateRange(sn.startDate, sn.endDate))
              )
              .map((sn) => sn.option.nameFi)
              .join(' / ')}
          />
        )}
      </Td>
      <Td data-qa="placement-end-date">{`${placement.endDate.format()}`}</Td>
      <Td data-qa="group-name">{groupNameToday()}</Td>
    </Tr>
  )
}

type Props = {
  placements: DaycarePlacement[]
}

export default React.memo(function TerminatedPlacements({ placements }: Props) {
  const { i18n } = useTranslation()

  const terminatedPlacementsToShow = placements.filter(
    (placement) =>
      placement.endDate != null &&
      placement.terminationRequestedDate != null &&
      placement.terminationRequestedDate.isAfter(LocalDate.today().subWeeks(2))
  )

  const sortedRows = _.sortBy(terminatedPlacementsToShow, [
    (p: DaycarePlacement) => p.child.lastName,
    (p: DaycarePlacement) => p.child.firstName,
    (p: DaycarePlacement) => p.startDate
  ])

  return (
    <>
      <Title size={2}>{i18n.unit.termination.title}</Title>
      <div
        className="table-of-terminated-placements"
        data-qa="table-of-terminated-placements"
      >
        <Table data-qa="table-of-terminated-placements">
          <Thead>
            <Tr>
              <Th>{i18n.unit.placements.name}</Th>
              <Th>{i18n.unit.placements.birthday}</Th>
              <Th>{i18n.unit.placements.type}</Th>
              <Th>{i18n.unit.placements.subtype}</Th>
              <Th>{i18n.unit.termination.endDate}</Th>
              <Th>{i18n.unit.termination.groupName}</Th>
              <Th />
            </Tr>
          </Thead>
          <Tbody>
            {sortedRows.map((row) => renderTerminatedPlacementRow(row, i18n))}
          </Tbody>
        </Table>
      </div>
    </>
  )
})
