// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { range } from 'lodash'
import { UUID } from '../../types'
import { Loading, Result } from 'lib-common/api'
import {
  getFamilyContacts,
  updateFamilyContacts
} from '../../api/family-overview'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { FamilyContact } from '../../types/family-overview'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { faUsers } from 'lib-icons'
import { useTranslation } from '../../state/i18n'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { formatName } from '../../utils'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import SimpleSelect from 'lib-components/atoms/form/SimpleSelect'

interface FamilyContactsProps {
  id: UUID
  open: boolean
}

function FamilyContacts({ id, open }: FamilyContactsProps) {
  const { i18n } = useTranslation()
  const [result, setResult] = useState<Result<FamilyContact[]>>(Loading.of())

  const loadContacts = useRestApi(getFamilyContacts, setResult)
  useEffect(() => loadContacts(id), [id, loadContacts])

  const contactPriorityOptions: { label: string; value: string }[] = result
    .map((contacts) => {
      const ordinals = contacts
        .map((contact) => contact.priority)
        .filter((priority) => priority !== null)
      const minMax = Math.min(
        Math.max(...ordinals) + 1,
        contacts.filter(({ role }) => role !== 'LOCAL_SIBLING').length
      )
      return minMax > 1 ? range(1, minMax + 1) : [1]
    })
    .getOrElse([1])
    .map((v: number) => ({ label: String(v), value: String(v) }))

  return (
    <CollapsibleSection
      icon={faUsers}
      title={i18n.childInformation.familyContacts.title}
      startCollapsed={!open}
      data-qa="family-contacts-collapsible"
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
              <Th>{i18n.childInformation.familyContacts.contactPerson}</Th>
              <Th>{i18n.childInformation.familyContacts.address}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {result.value.map((row) => (
              <Tr
                key={`${row.role}:${row.lastName || ''}:${row.firstName || ''}`}
              >
                <Td>{formatName(row.firstName, row.lastName, i18n, true)}</Td>
                <Td>{i18n.childInformation.familyContacts.roles[row.role]}</Td>
                <Td>
                  <FixedSpaceColumn spacing="xs">
                    {row.email && <span>{row.email}</span>}
                    {row.phone && <span>{row.phone}</span>}
                    {row.backupPhone && (
                      <span>
                        {row.backupPhone}{' '}
                        {`(${i18n.childInformation.familyContacts.backupPhone})`}
                      </span>
                    )}
                  </FixedSpaceColumn>
                </Td>
                <Td>
                  {row.role !== 'LOCAL_SIBLING' ? (
                    <SimpleSelect
                      value={String(row.priority)}
                      options={contactPriorityOptions}
                      onChange={(event) => {
                        void updateFamilyContacts({
                          childId: id,
                          contactPersonId: row.id,
                          priority: event.target.value
                            ? Number(event.target.value)
                            : undefined
                        }).then(() => {
                          loadContacts(id)
                        })
                      }}
                      placeholder="-"
                    />
                  ) : null}
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
