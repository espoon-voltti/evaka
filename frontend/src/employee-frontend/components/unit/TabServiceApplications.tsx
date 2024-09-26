// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { Link } from 'react-router-dom'

import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import { useTranslation } from '../../state/i18n'
import { NotificationCounter } from '../UnitPage'
import { renderResult } from '../async-rendering'

import { unitServiceApplicationsQuery } from './queries'

interface Props {
  unitId: UUID
}

export default React.memo(function TabServiceApplications({ unitId }: Props) {
  const { i18n } = useTranslation()
  const [open, setOpen] = useState<boolean>(true)
  const applications = useQueryResult(unitServiceApplicationsQuery({ unitId }))

  return (
    <CollapsibleContentArea
      title={
        <Title size={2}>
          {i18n.unit.serviceApplications.title}
          {applications.isSuccess && applications.value.length > 0 ? (
            <NotificationCounter>
              {applications.value.length}
            </NotificationCounter>
          ) : null}
        </Title>
      }
      opaque
      open={open}
      toggleOpen={() => setOpen(!open)}
    >
      {renderResult(applications, (applications) => (
        <div>
          <Table data-qa="service-applications-table">
            <Thead>
              <Tr>
                <Th>{i18n.unit.serviceApplications.child}</Th>
                <Th>{i18n.unit.serviceApplications.range}</Th>
                <Th>{i18n.unit.serviceApplications.newNeed}</Th>
                <Th>{i18n.unit.serviceApplications.currentNeed}</Th>
                <Th>{i18n.unit.serviceApplications.sentDate}</Th>
              </Tr>
            </Thead>
            <Tbody>
              {applications.map((row) => (
                <Tr key={row.id} data-qa="service-application-row">
                  <Td data-qa="child-name">
                    <Link to={`/child-information/${row.childId}`}>
                      {row.childName}
                    </Link>
                  </Td>
                  <Td data-qa="range">
                    {row.startDate.format()} - {row.placementEndDate.format()}
                  </Td>
                  <Td data-qa="new-need">{row.newNeed}</Td>
                  <Td data-qa="current-need">{row.currentNeed ?? '-'}</Td>
                  <Td data-qa="sent-date">
                    {row.sentAt.toLocalDate().format()}
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        </div>
      ))}
    </CollapsibleContentArea>
  )
})
