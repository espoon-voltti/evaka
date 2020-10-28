import React, { useContext } from 'react'
import { ContentArea } from '~components/shared/layout/Container'
import { UnitContext } from '~state/unit'
import { isFailure, isLoading } from '~api'
import { SpinnerSegment } from '~components/shared/atoms/state/Spinner'
import ErrorSegment from '~components/shared/atoms/state/ErrorSegment'
import { useTranslation } from '~state/i18n'
import Title from '~components/shared/atoms/Title'
import {
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from '~components/shared/layout/Table'
import { formatName } from '~utils'
import PlacementCircle from '~components/shared/atoms/PlacementCircle'
import { careTypesFromPlacementType } from '~components/common/CareTypeLabel'

function TabApplications() {
  const { i18n } = useTranslation()
  const { unitData } = useContext(UnitContext)

  if (isLoading(unitData)) {
    return <SpinnerSegment />
  }

  if (isFailure(unitData) || !unitData.data.applications) {
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
            {unitData.data.applications.map((row) => (
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
                  <PlacementCircle type={row.requestedPlacementType} />
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
