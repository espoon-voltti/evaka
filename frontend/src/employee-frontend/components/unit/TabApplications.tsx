{
  /*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React, { useContext } from 'react'
import { ContentArea } from 'lib-components/layout/Container'
import { UnitContext } from '../../state/unit'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { useTranslation } from '../../state/i18n'
import Title from 'lib-components/atoms/Title'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { formatName } from '../../utils'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import { careTypesFromPlacementType } from '../common/CareTypeLabel'
import { isPartDayPlacement } from '../../utils/placements'

function TabApplications() {
  const { i18n } = useTranslation()
  const { unitData } = useContext(UnitContext)

  if (unitData.isLoading) {
    return <SpinnerSegment />
  }

  if (unitData.isFailure || !unitData.value.applications) {
    return <ErrorSegment />
  }

  return (
    <ContentArea opaque>
      <Title size={2}>{i18n.unit.applications.title}</Title>
      <div>
        <Table>
          <Thead>
            <Tr>
              <Th>{i18n.unit.applications.child}</Th>
              <Th>{i18n.unit.applications.guardian}</Th>
              <Th>{i18n.unit.applications.type}</Th>
              <Th>{i18n.unit.applications.placement}</Th>
              <Th>{i18n.unit.applications.preferenceOrder}</Th>
              <Th>{i18n.unit.applications.startDate}</Th>
              <Th>{i18n.unit.applications.status}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {unitData.value.applications.map((row) => (
              <Tr key={`${row.applicationId}`} data-qa="application-row">
                <Td>
                  <div data-qa="child-name">
                    {formatName(row.firstName, row.lastName, i18n, true)}
                  </div>
                  <div data-qa="child-dob">{row.dateOfBirth.format()}</div>
                </Td>
                <Td>
                  <div data-qa="guardian-name">
                    {formatName(
                      row.guardianFirstName,
                      row.guardianLastName,
                      i18n,
                      true
                    )}
                  </div>
                  <div data-qa="guardian-phone">{row.guardianPhone}</div>
                  <div data-qa="guardian-email">{row.guardianEmail}</div>
                </Td>
                <Td data-qa="placement-type">
                  {careTypesFromPlacementType(row.requestedPlacementType)}
                </Td>
                <Td data-qa="placement-subtype">
                  <PlacementCircle
                    type={
                      isPartDayPlacement(row.requestedPlacementType)
                        ? 'half'
                        : 'full'
                    }
                    label={
                      row.serviceNeed !== null
                        ? row.serviceNeed.name
                        : i18n.placement.type[row.requestedPlacementType]
                    }
                  />
                </Td>
                <Td data-qa="preference-order">{`${row.preferenceOrder}.`}</Td>
                <Td data-qa="preferred-start">
                  {row.preferredStartDate.format()}
                </Td>
                <Td data-qa="status">
                  {i18n.application.statuses[row.status]}
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </div>
    </ContentArea>
  )
}

export default React.memo(TabApplications)
