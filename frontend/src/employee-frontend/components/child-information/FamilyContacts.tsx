// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import range from 'lodash/range'
import React, { useCallback, useContext, useMemo, useState } from 'react'

import { isLoading, wrapResult } from 'lib-common/api'
import { FamilyContact } from 'lib-common/generated/api-types/pis'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
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
  getFamilyContactSummary,
  updateFamilyContactDetails,
  updateFamilyContactPriority
} from '../../generated/api-clients/pis'
import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import { renderResult } from '../async-rendering'

import BackupPickup from './BackupPickup'

const getFamilyContactSummaryResult = wrapResult(getFamilyContactSummary)
const updateFamilyContactDetailsResult = wrapResult(updateFamilyContactDetails)
const updateFamilyContactPriorityResult = wrapResult(
  updateFamilyContactPriority
)

export interface Props {
  childId: UUID
  startOpen: boolean
}

export default React.memo(function FamilyContacts({
  childId,
  startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext(ChildContext)

  const [contacts, reloadContacts] = useApiState(
    () => getFamilyContactSummaryResult({ childId }),
    [childId]
  )
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
        <FamilyContactTable
          childId={childId}
          contacts={contacts}
          reloadContacts={reloadContacts}
        />
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
  contacts,
  reloadContacts
}: {
  childId: string
  contacts: FamilyContact[]
  reloadContacts: () => void
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
              reloadContacts={reloadContacts}
              contactPriorityOptions={contactPriorityOptions}
            />
          ))}
        </Tbody>
      </Table>
    </>
  )
})

interface FamilyContactFields {
  email: string
  phone: string
  backupPhone: string
}

const FamilyContactRow = React.memo(function FamilyContactRow({
  childId,
  contact,
  reloadContacts,
  contactPriorityOptions
}: {
  childId: UUID
  contact: FamilyContact
  reloadContacts: () => void
  contactPriorityOptions: number[]
}) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext(ChildContext)

  const [editState, setEditState] = useState<'viewing' | 'editing' | 'saving'>(
    'viewing'
  )
  const edit = useCallback(() => setEditState('editing'), [])
  const cancel = useCallback(() => setEditState('viewing'), [])

  const saveContact = useCallback(
    async (formData: FamilyContactFields) => {
      setEditState('saving')
      await updateFamilyContactDetailsResult({
        body: {
          childId,
          contactPersonId: contact.id,
          ...formData
        }
      })
      reloadContacts()
      setEditState('viewing')
    },
    [childId, contact.id, reloadContacts]
  )

  const setContactPriority = useCallback(
    async (priority: number | null) => {
      if (priority == null) return

      await updateFamilyContactPriorityResult({
        body: {
          childId,
          contactPersonId: contact.id,
          priority
        }
      })
      reloadContacts()
    },
    [childId, contact.id, reloadContacts]
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
        {editState === 'editing' || editState === 'saving' ? (
          <EditContactFields
            childId={childId}
            contact={contact}
            isSaving={editState === 'saving'}
            onSave={saveContact}
            onCancel={cancel}
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
              <InlineButton
                onClick={edit}
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
  contact,
  isSaving,
  onSave,
  onCancel
}: {
  childId: UUID
  contact: FamilyContact
  isSaving: boolean
  onSave: (formData: FamilyContactFields) => void
  onCancel: () => void
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
        <InlineButton
          onClick={onCancel}
          text={i18n.common.cancel}
          disabled={isSaving}
        />
        <InlineButton
          onClick={() => onSave(formData)}
          text={i18n.common.save}
          disabled={isSaving || !dirty}
          data-qa="family-contact-save"
        />
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
})
