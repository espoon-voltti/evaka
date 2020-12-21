// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { UUID } from 'types'
import { Loading, Result } from 'api'
import { getFamilyContacts } from 'api/family-overview'
import { useRestApi } from 'utils/useRestApi'
import { FamilyContact } from 'types/family-overview'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import CollapsibleSection from 'components/shared/molecules/CollapsibleSection'
import { faUsers } from '@evaka/lib-icons'
import { useTranslation } from 'state/i18n'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import {
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from '@evaka/lib-components/src/layout/Table'
import { formatName } from 'utils'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'

interface FamilyContactsProps {
  id: UUID
  open: boolean
}

function FamilyContacts({ id, open }: FamilyContactsProps) {
  const { i18n } = useTranslation()
  const [result, setResult] = useState<Result<FamilyContact[]>>(Loading.of())

  const loadContacts = useRestApi(getFamilyContacts, setResult)
  useEffect(() => loadContacts(id), [id, loadContacts])

  const orderedRows: FamilyContact[] = result
    .map((r) => [
      ...r.filter((row) => row.role === 'LOCAL_GUARDIAN'),
      ...r.filter((row) => row.role === 'LOCAL_ADULT'),
      ...r.filter((row) => row.role === 'LOCAL_SIBLING'),
      ...r.filter((row) => row.role === 'REMOTE_GUARDIAN')
    ])
    .getOrElse([])

  return (
    <CollapsibleSection
      icon={faUsers}
      title={i18n.childInformation.familyContacts.title}
      startCollapsed={!open}
    >
      {result.isLoading && <SpinnerSegment />}
      {result.isFailure && <ErrorSegment />}
      {result.isSuccess && (
        <Table>
          <Thead>
            <Tr>
              <Th>{i18n.childInformation.familyContacts.name}</Th>
              <Th>{i18n.childInformation.familyContacts.role}</Th>
              <Th>{i18n.childInformation.familyContacts.contact}</Th>
              <Th>{i18n.childInformation.familyContacts.address}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {orderedRows.map((row) => (
              <Tr
                key={`${row.role}:${row.lastName || ''}:${row.firstName || ''}`}
              >
                <Td>{formatName(row.firstName, row.lastName, i18n, true)}</Td>
                <Td>{i18n.childInformation.familyContacts.roles[row.role]}</Td>
                <Td>
                  <FixedSpaceColumn spacing="xs">
                    <span>{row.email}</span>
                    <span>{row.phone}</span>
                  </FixedSpaceColumn>
                </Td>
                <Td>{`${row.streetAddress}, ${row.postalCode} ${row.postOffice}`}</Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      )}
    </CollapsibleSection>
  )
}

export default FamilyContacts
