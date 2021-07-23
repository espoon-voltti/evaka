// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { cloneDeep, range } from 'lodash'
import { UUID } from '../../types'
import { Loading, Result } from 'lib-common/api'
import {
  getFamilyContacts,
  updateFamilyContacts
} from '../../api/family-overview'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { FamilyContact } from '../../types/family-overview'
import { Gap } from 'lib-components/white-space'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { useTranslation } from '../../state/i18n'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { formatName } from '../../utils'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import SimpleSelect from 'lib-components/atoms/form/SimpleSelect'
import { CollapsibleContentArea } from '../../../lib-components/layout/Container'
import { H2, H3 } from '../../../lib-components/typography'
import InputField from 'lib-components/atoms/form/InputField'
import { patchPersonDetails } from 'employee-frontend/api/person'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { UserContext } from 'employee-frontend/state/user'
import BackupPickup from './BackupPickup'

interface FamilyContactsProps {
  id: UUID
  startOpen: boolean
}

function FamilyContacts({ id, startOpen }: FamilyContactsProps) {
  const { i18n } = useTranslation()
  const [result, setResult] = useState<Result<FamilyContact[]>>(Loading.of())
  const [dirty, setDirty] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [open, setOpen] = useState(startOpen)
  const { roles } = useContext(UserContext)

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

  function onCancel() {
    loadContacts(id)
  }

  function onSubmit(personId: UUID) {
    setSubmitting(true)
    result.map((data) => {
      data
        .filter((row) => row.id === personId)
        .map((person) => {
          void patchPersonDetails(personId, {
            firstName: undefined,
            lastName: undefined,
            dateOfBirth: undefined,
            streetAddress: undefined,
            postalCode: undefined,
            postOffice: undefined,
            email: person.email ?? undefined,
            phone: person.phone ?? undefined,
            backupPhone: person.backupPhone ?? undefined
          }).then(() => {
            loadContacts(id)
          })
        })
    })
    setDirty(false)
    setSubmitting(false)
  }

  const editableContacts = roles.find((r) =>
    [
      'ADMIN',
      'SERVICE_WORKER',
      'UNIT_SUPERVISOR',
      'FINANCE_ADMIN',
      'STAFF'
    ].includes(r)
  )

  return (
    <CollapsibleContentArea
      title={<H2 noMargin>{i18n.childInformation.familyContacts.title}</H2>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-qa="family-contacts-collapsible"
    >
      {result.isLoading && <SpinnerSegment />}
      {result.isFailure && <ErrorSegment />}
      {result.isSuccess && (
        <>
          <Gap size="m" />
          <H3 noMargin>{i18n.childInformation.familyContacts.contacts}</H3>
          <Table>
            <Thead>
              <Tr>
                <Th>{i18n.childInformation.familyContacts.name}</Th>
                <Th>{i18n.childInformation.familyContacts.role}</Th>
                <Th>{i18n.childInformation.familyContacts.contact}</Th>
                <Th>{i18n.childInformation.familyContacts.contactPerson}</Th>
                <Th>{i18n.childInformation.familyContacts.address}</Th>
                <Th></Th>
              </Tr>
            </Thead>
            <Tbody>
              {result.value.map((row) => (
                <Tr
                  key={`${row.role}:${row.lastName || ''}:${
                    row.firstName || ''
                  }`}
                >
                  <Td>{formatName(row.firstName, row.lastName, i18n, true)}</Td>
                  <Td>
                    {i18n.childInformation.familyContacts.roles[row.role]}
                  </Td>
                  <Td>
                    {editableContacts ? (
                      <FixedSpaceColumn spacing="xs">
                        {row.email && (
                          <span>
                            <InputField
                              value={row.email}
                              onChange={(value) => {
                                setDirty(true)
                                setResult((prev) => {
                                  return prev.map((prevData) => {
                                    const clone = cloneDeep(prevData)
                                    clone.map((cloneRow) => {
                                      if (cloneRow.id === row.id) {
                                        cloneRow.email = value
                                      }
                                      return cloneRow
                                    })
                                    return clone
                                  })
                                })
                              }}
                            />
                          </span>
                        )}
                        {row.phone && (
                          <span>
                            <InputField
                              value={row.phone}
                              onChange={(value) => {
                                setDirty(true)
                                setResult((prev) => {
                                  return prev.map((prevData) => {
                                    const clone = cloneDeep(prevData)
                                    clone.map((cloneRow) => {
                                      if (cloneRow.id === row.id) {
                                        cloneRow.phone = value
                                      }
                                      return cloneRow
                                    })
                                    return clone
                                  })
                                })
                              }}
                            />
                          </span>
                        )}
                        {row.backupPhone && (
                          <span>
                            <InputField
                              value={row.backupPhone}
                              onChange={(value) => {
                                setResult((prev) => {
                                  setDirty(true)
                                  return prev.map((prevData) => {
                                    const clone = cloneDeep(prevData)
                                    clone.map((cloneRow) => {
                                      if (cloneRow.id === row.id) {
                                        cloneRow.backupPhone = value
                                      }
                                      return cloneRow
                                    })
                                    return clone
                                  })
                                })
                              }}
                            />{' '}
                            {`(${i18n.childInformation.familyContacts.backupPhone})`}
                          </span>
                        )}
                      </FixedSpaceColumn>
                    ) : (
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
                    )}
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
                  <Td>
                    <FixedSpaceRow justifyContent="flex-end" spacing="m">
                      {dirty && (
                        <InlineButton
                          onClick={onCancel}
                          text={i18n.common.cancel}
                          disabled={submitting}
                        />
                      )}
                      <InlineButton
                        onClick={() => onSubmit(row.id)}
                        text={i18n.common.save}
                        disabled={submitting || !dirty}
                      />
                    </FixedSpaceRow>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        </>
      )}
      <Gap size="XL" />
      <BackupPickup id={id} />
    </CollapsibleContentArea>
  )
}

export default FamilyContacts
