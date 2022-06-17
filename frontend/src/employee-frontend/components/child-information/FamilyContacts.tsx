// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { cloneDeep, range } from 'lodash'
import React, { useContext, useEffect, useState } from 'react'

import { Loading, Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { useRestApi } from 'lib-common/utils/useRestApi'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import InputField from 'lib-components/atoms/form/InputField'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2, H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import {
  getFamilyContacts,
  updateFamilyContactDetails,
  updateFamilyContactPriority
} from '../../api/family-overview'
import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { FamilyContact } from '../../types/family-overview'
import { formatName } from '../../utils'
import { renderResult } from '../async-rendering'

import BackupPickup from './BackupPickup'

interface FamilyContactsProps {
  id: UUID
  startOpen: boolean
}

export default React.memo(function FamilyContacts({
  id,
  startOpen
}: FamilyContactsProps) {
  const { i18n } = useTranslation()
  const [result, setResult] = useState<Result<FamilyContact[]>>(Loading.of())
  const [dirty, setDirty] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [open, setOpen] = useState(startOpen)
  const { permittedActions } = useContext(ChildContext)

  const loadContacts = useRestApi(getFamilyContacts, setResult)
  useEffect(() => loadContacts(id), [id, loadContacts])

  const contactPriorityOptions = result
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

  function onCancel() {
    loadContacts(id)
  }

  function onSubmit(personId: UUID) {
    setSubmitting(true)
    result.map((data) => {
      data
        .filter((row) => row.id === personId)
        .map((person) => {
          void updateFamilyContactDetails({
            childId: id,
            contactPersonId: personId,
            email: person.email ?? null,
            phone: person.phone ?? null,
            backupPhone: person.backupPhone ?? null
          }).then(() => {
            loadContacts(id)
          })
        })
    })
    setDirty(false)
    setSubmitting(false)
  }

  return (
    <CollapsibleContentArea
      title={<H2 noMargin>{i18n.childInformation.familyContacts.title}</H2>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-qa="family-contacts-collapsible"
    >
      {renderResult(result, (result) => (
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
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {result.map((row) => (
                <Tr
                  data-qa={`table-family-contact-row-${row.id}`}
                  key={`${row.role}:${row.lastName || ''}:${
                    row.firstName || ''
                  }`}
                >
                  <Td>{formatName(row.firstName, row.lastName, i18n, true)}</Td>
                  <Td>
                    {i18n.childInformation.familyContacts.roles[row.role]}
                  </Td>
                  <Td>
                    {permittedActions.has('UPDATE_FAMILY_CONTACT_DETAILS') ? (
                      <FixedSpaceColumn spacing="xs">
                        <span>
                          <InputField
                            data-qa="family-contact-email-input"
                            value={row.email ?? ''}
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
                        <span>
                          <InputField
                            data-qa="family-contact-phone-input"
                            value={row.phone ?? ''}
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
                        <span>
                          <InputField
                            data-qa="family-contact-backup-phone-input"
                            value={row.backupPhone ?? ''}
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
                      </FixedSpaceColumn>
                    ) : (
                      <FixedSpaceColumn spacing="xs">
                        {row.email !== null && (
                          <span data-qa="family-contact-email">
                            {row.email}
                          </span>
                        )}
                        {row.phone !== null && (
                          <span data-qa="family-contact-phone">
                            {row.phone}
                          </span>
                        )}
                        {row.backupPhone !== null && (
                          <span data-qa="family-contact-backup-phone">
                            {row.backupPhone}{' '}
                            {`(${i18n.childInformation.familyContacts.backupPhone})`}
                          </span>
                        )}
                      </FixedSpaceColumn>
                    )}
                  </Td>
                  <Td>
                    {row.role !== 'LOCAL_SIBLING' ? (
                      permittedActions.has('UPDATE_FAMILY_CONTACT_PRIORITY') ? (
                        <Select
                          selectedItem={row.priority}
                          items={contactPriorityOptions}
                          onChange={(value) => {
                            void updateFamilyContactPriority({
                              childId: id,
                              contactPersonId: row.id,
                              priority: value ? Number(value) : null
                            }).then(() => {
                              loadContacts(id)
                            })
                          }}
                          placeholder="-"
                        />
                      ) : (
                        row.priority
                      )
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
                        data-qa="family-contact-save"
                      />
                    </FixedSpaceRow>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        </>
      ))}
      <Gap size="XL" />
      {permittedActions.has('READ_BACKUP_PICKUP') && <BackupPickup id={id} />}
    </CollapsibleContentArea>
  )
})
