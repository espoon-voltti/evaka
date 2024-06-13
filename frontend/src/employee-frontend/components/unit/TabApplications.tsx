// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { ApplicationUnitSummary } from 'lib-common/generated/api-types/application'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import Title from 'lib-components/atoms/Title'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import { isPartDayPlacement } from '../../utils/placements'
import { NotificationCounter } from '../UnitPage'
import { CareTypeChip } from '../common/CareTypeLabel'

interface Props {
  applications: ApplicationUnitSummary[]
}

export default React.memo(function TabApplications({ applications }: Props) {
  const { i18n } = useTranslation()
  const [open, setOpen] = useState<boolean>(true)

  return (
    <CollapsibleContentArea
      title={
        <Title size={2}>
          {i18n.unit.applications.title}
          {applications.length > 0 ? (
            <NotificationCounter>{applications.length}</NotificationCounter>
          ) : null}
        </Title>
      }
      opaque
      open={open}
      toggleOpen={() => setOpen(!open)}
    >
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
            {applications.map((row) => (
              <Tr key={row.applicationId} data-qa="application-row">
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
                  <CareTypeChip type={row.requestedPlacementType} />
                </Td>
                <Td data-qa="placement-subtype">
                  <PlacementCircle
                    type={
                      isPartDayPlacement(row.requestedPlacementType)
                        ? 'half'
                        : 'full'
                    }
                    label={
                      (row.serviceNeed !== null
                        ? row.serviceNeed.nameFi
                        : i18n.placement.type[row.requestedPlacementType]) +
                      (row.extendedCare
                        ? ', ' + i18n.unit.applications.extendedCare
                        : '')
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
    </CollapsibleContentArea>
  )
})
