// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import range from 'lodash/range'
import React, { useCallback, useContext, useMemo, useState } from 'react'

import { isLoading } from 'lib-common/api'
import { FamilyContact } from 'lib-common/generated/api-types/pis'
import { ChildId } from 'lib-common/generated/api-types/shared'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
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

import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import { renderResult } from '../async-rendering'

import BackupPickup from './BackupPickup'
import {
  familyContactSummaryQuery,
  updateFamilyContactDetailsMutation,
  updateFamilyContactPriorityMutation
} from './queries'

export interface Props {
  childId: ChildId
  startOpen: boolean
}

export default React.memo(function FamilyContacts({
  childId,
  startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext(ChildContext)

  const contacts = useQueryResult(familyContactSummaryQuery({ childId }))
  const [open, setOpen] = useState(startOpen)

  return (
    <CollapsibleContentArea
      title={<H2 noMargin>{i18n.childInformation.familyContacts.title}</H2>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-isloading={isLoading(contacts)}
      data-qa="family-contacts-collapsible"
    >
      {renderResult(contacts, (contacts) => (
        <FamilyContactTable childId={childId} contacts={contacts} />
      ))}
      <Gap size="XL" />
      {permittedActions.has('READ_BACKUP_PICKUP') && (
        <BackupPickup childId={childId} />
      )}
    </CollapsibleContentArea>
  )
})

const FamilyContactTable = React.memo(function FamilyContactForm({
  childId,
  contacts
}: {
  childId: ChildId
  contacts: FamilyContact[]
}) {
  const { i18n } = useTranslation()

  const contactPriorityOptions = useMemo(() => {
    const ordinals = contacts
      .map((contact) => contact.priority)
      .filter((priority): priority is number => priority !== null)
    const minMax = Math.min(
      Math.max(...ordinals) + 1,
      contacts.filter(({ role }) => role !== 'LOCAL_SIBLING').length
    )
    return minMax > 1 ? range(1, minMax + 1) : [1]
  }, [contacts])

  return (
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
          {contacts.map((contact) => (
            <FamilyContactRow
              key={contact.id}
              childId={childId}
              contact={contact}
              contactPriorityOptions={contactPriorityOptions}
            />
          ))}
        </Tbody>
      </Table>
    </>
  )
})

const FamilyContactRow = React.memo(function FamilyContactRow({
  childId,
  contact,
  contactPriorityOptions
}: {
  childId: ChildId
  contact: FamilyContact
  contactPriorityOptions: number[]
}) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext(ChildContext)

  const [editing, setEditing] = useState(false)

  const { mutateAsync: updatePriority } = useMutationResult(
    updateFamilyContactPriorityMutation
  )

  const setContactPriority = useCallback(
    async (priority: number | null) => {
      if (priority == null) return

      await updatePriority({
        body: {
          childId,
          contactPersonId: contact.id,
          priority
        }
      })
    },
    [childId, contact.id, updatePriority]
  )

  return (
    <Tr
      data-qa={`table-family-contact-row-${contact.id}`}
      key={`${contact.role}:${contact.lastName || ''}:${
        contact.firstName || ''
      }`}
    >
      <Td>{formatName(contact.firstName, contact.lastName, i18n, true)}</Td>
      <Td>{i18n.childInformation.familyContacts.roles[contact.role]}</Td>
      <Td>
        {editing ? (
          <EditContactFields
            childId={childId}
            contact={contact}
            onCloseEditor={() => setEditing(false)}
          />
        ) : (
          <FixedSpaceColumn spacing="xs">
            {contact.email ? (
              <span data-qa="family-contact-email">{contact.email}</span>
            ) : null}
            {contact.phone ? (
              <span data-qa="family-contact-phone">{contact.phone}</span>
            ) : null}
            {contact.backupPhone ? (
              <span data-qa="family-contact-backup-phone">
                {contact.backupPhone}{' '}
                {`(${i18n.childInformation.familyContacts.backupPhone})`}
              </span>
            ) : null}
            {contact.role !== 'LOCAL_SIBLING' &&
            permittedActions.has('UPDATE_FAMILY_CONTACT_DETAILS') ? (
              <Button
                appearance="inline"
                onClick={() => setEditing(true)}
                text={i18n.common.edit}
                data-qa="family-contact-edit"
              />
            ) : null}
          </FixedSpaceColumn>
        )}
      </Td>
      <Td>
        {contact.role !== 'LOCAL_SIBLING' ? (
          permittedActions.has('UPDATE_FAMILY_CONTACT_PRIORITY') ? (
            <Select
              selectedItem={contact.priority}
              items={contactPriorityOptions}
              onChange={setContactPriority}
              placeholder="-"
            />
          ) : (
            contact.priority
          )
        ) : null}
      </Td>
      <Td>{`${contact.streetAddress}, ${contact.postalCode} ${contact.postOffice}`}</Td>
    </Tr>
  )
})

const EditContactFields = React.memo(function EditFoo({
  childId,
  contact,
  onCloseEditor
}: {
  childId: ChildId
  contact: FamilyContact
  onCloseEditor: () => void
}) {
  const { i18n } = useTranslation()

  const initialState = useMemo(
    () => ({
      email: contact.email ?? '',
      phone: contact.phone ?? '',
      backupPhone: contact.backupPhone ?? ''
    }),
    [contact.backupPhone, contact.email, contact.phone]
  )
  const [formData, setFormData] = useState(initialState)

  const dirty = useMemo(
    () => !isEqual(formData, initialState),
    [formData, initialState]
  )

  const setEmail = (email: string) =>
    setFormData((prev) => ({ ...prev, email }))
  const setPhone = (phone: string) =>
    setFormData((prev) => ({ ...prev, phone }))
  const setBackupPhone = (backupPhone: string) =>
    setFormData((prev) => ({ ...prev, backupPhone }))

  return (
    <FixedSpaceColumn spacing="xs">
      <span>
        <InputField
          data-qa="family-contact-email-input"
          placeholder={i18n.common.form.email}
          value={formData.email}
          onChange={setEmail}
        />
      </span>
      <span>
        <InputField
          data-qa="family-contact-phone-input"
          placeholder={i18n.common.form.phone}
          value={formData.phone}
          onChange={setPhone}
        />
      </span>
      <span>
        <InputField
          data-qa="family-contact-backup-phone-input"
          placeholder={i18n.childInformation.familyContacts.backupPhone}
          value={formData.backupPhone}
          onChange={setBackupPhone}
        />{' '}
        {`(${i18n.childInformation.familyContacts.backupPhone})`}
      </span>
      <FixedSpaceRow justifyContent="flex-end" spacing="m">
        <Button
          appearance="inline"
          onClick={onCloseEditor}
          text={i18n.common.cancel}
        />
        <MutateButton
          appearance="inline"
          text={i18n.common.save}
          mutation={updateFamilyContactDetailsMutation}
          onClick={() => ({
            body: {
              childId,
              contactPersonId: contact.id,
              ...formData
            }
          })}
          onSuccess={onCloseEditor}
          disabled={!dirty}
          data-qa="family-contact-save"
        />
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
})
