// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useMemo } from 'react'

import { useBoolean } from 'lib-common/form/hooks'
import { TransferApplicationUnitSummary } from 'lib-common/generated/api-types/application'
import Title from 'lib-components/atoms/Title'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'

interface Props {
  transferApplications: TransferApplicationUnitSummary[]
}

export default React.memo(function TabTransferApplications({
  transferApplications
}: Props) {
  const { i18n } = useTranslation()
  const [open, { toggle: toggleOpen }] = useBoolean(true)
  const sortedTransferApplications = useMemo(
    () =>
      sortBy(transferApplications, [
        (row) => row.lastName,
        (row) => row.firstName
      ]),
    [transferApplications]
  )

  return (
    <CollapsibleContentArea
      title={
        <Title size={2}>
          {i18n.unit.transferApplications.title} (
          {sortedTransferApplications.length})
        </Title>
      }
      opaque
      open={open}
      toggleOpen={toggleOpen}
      data-qa="transfer-applications-section"
    >
      <div>
        <Table>
          <Thead>
            <Tr>
              <Th>{i18n.unit.transferApplications.child}</Th>
              <Th>{i18n.unit.transferApplications.startDate}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {sortedTransferApplications.map((row) => (
              <Tr key={row.applicationId} data-qa="transfer-application-row">
                <Td>
                  <div>
                    {formatName(row.firstName, row.lastName, i18n, true)}
                  </div>
                  <div>{row.dateOfBirth.format()}</div>
                </Td>
                <Td>{row.preferredStartDate.format()}</Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </div>
    </CollapsibleContentArea>
  )
})
