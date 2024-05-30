// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import styled from 'styled-components'

import { ChildBackupPickup } from 'lib-common/generated/api-types/backuppickup'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import InputField from 'lib-components/atoms/form/InputField'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import FormModal from 'lib-components/molecules/modals/FormModal'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H3, Label } from 'lib-components/typography'
import { faPen, faQuestion, faTrash } from 'lib-icons'

import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { RequireRole } from '../../utils/roles'
import { FlexRow } from '../common/styled/containers'

import {
  createBackupPickupMutation,
  deleteBackupPickupMutation,
  getBackupPickupsQuery,
  updateBackupPickupMutation
} from './queries'

interface BackupPickupProps {
  childId: UUID
}

function BackupPickup({ childId }: BackupPickupProps) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const { permittedActions } = useContext(ChildContext)

  const backupPickups = useQueryResult(getBackupPickupsQuery({ childId }))
  const { mutateAsync: createBackupPickup } = useMutationResult(
    createBackupPickupMutation
  )
  const { mutateAsync: updateBackupPickup } = useMutationResult(
    updateBackupPickupMutation
  )
  const { mutateAsync: deleteBackupPickup } = useMutationResult(
    deleteBackupPickupMutation
  )

  const [backupPickup, setBackupPickup] = useState<
    ChildBackupPickup | undefined
  >(undefined)

  const openEditBackupPickupModal = (pickup: ChildBackupPickup) => {
    setBackupPickup(pickup)
    toggleUiMode(`edit-backup-pickup`)
  }

  const openRemoveBackupPickupModal = (pickup: ChildBackupPickup) => {
    setBackupPickup(pickup)
    toggleUiMode(`remove-backup-pickup`)
  }

  const confirmRemoveModal = async () => {
    if (backupPickups.isSuccess && backupPickup) {
      await deleteBackupPickup({ childId, id: backupPickup.id })
      setBackupPickup(undefined)
      clearUiMode()
    }
  }

  const CreateBackupPickupModal = () => {
    const [name, setName] = useState<string>('')
    const [phone, setPhone] = useState<string>('')

    async function saveBackupPickup() {
      if (name !== '' && phone !== '') {
        await createBackupPickup({ childId, body: { name, phone } })
        setBackupPickup(undefined)
        clearUiMode()
      }
    }

    return (
      <FormModal
        title={i18n.childInformation.backupPickups.add}
        resolveAction={saveBackupPickup}
        resolveLabel={i18n.common.save}
        rejectAction={clearUiMode}
        rejectLabel={i18n.common.cancel}
      >
        <FixedSpaceColumn>
          <FixedSpaceColumn spacing="xxs">
            <Label>{i18n.childInformation.backupPickups.name}</Label>
            <InputField
              value={name}
              onChange={setName}
              placeholder={i18n.childInformation.backupPickups.name}
              width="full"
              data-qa="backup-pickup-name-input"
            />
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xxs">
            <Label>{i18n.childInformation.backupPickups.phone}</Label>
            <InputField
              value={phone}
              onChange={setPhone}
              placeholder={i18n.childInformation.backupPickups.phone}
              width="full"
              data-qa="backup-pickup-phone-input"
            />
          </FixedSpaceColumn>
        </FixedSpaceColumn>
      </FormModal>
    )
  }

  const EditBackupPickupModal = () => {
    const [name, setName] = useState<string>(backupPickup?.name ?? '')
    const [phone, setPhone] = useState<string>(backupPickup?.phone ?? '')

    async function saveBackupPickup() {
      if (backupPickup) {
        await updateBackupPickup({
          childId,
          id: backupPickup.id,
          body: {
            name: name !== '' ? name : backupPickup.name,
            phone: phone !== '' ? phone : backupPickup.phone
          }
        })
        setBackupPickup(undefined)
        clearUiMode()
      }
    }

    return (
      <FormModal
        title={i18n.childInformation.backupPickups.edit}
        resolveAction={saveBackupPickup}
        resolveLabel={i18n.common.save}
        rejectAction={clearUiMode}
        rejectLabel={i18n.common.cancel}
      >
        <FixedSpaceColumn>
          <FixedSpaceColumn spacing="xxs">
            <Label>{i18n.childInformation.backupPickups.name}</Label>
            <InputField
              value={name}
              onChange={setName}
              placeholder={i18n.childInformation.backupPickups.name}
              width="full"
              data-qa="backup-pickup-name-input"
            />
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xxs">
            <Label>{i18n.childInformation.backupPickups.phone}</Label>
            <InputField
              value={phone}
              onChange={setPhone}
              placeholder={i18n.childInformation.backupPickups.phone}
              width="full"
              data-qa="backup-pickup-phone-input"
            />
          </FixedSpaceColumn>
        </FixedSpaceColumn>
      </FormModal>
    )
  }

  const DeleteBackupPickupModal = () => (
    <InfoModal
      type="warning"
      title={i18n.childInformation.backupPickups.removeConfirmation}
      icon={faQuestion}
      reject={{ action: () => clearUiMode(), label: i18n.common.cancel }}
      resolve={{ action: confirmRemoveModal, label: i18n.common.remove }}
    />
  )

  return (
    <>
      {backupPickups.isLoading && <SpinnerSegment />}
      {backupPickups.isFailure && <ErrorSegment />}
      {backupPickups.isSuccess && (
        <>
          <FlexRow justifyContent="space-between">
            <H3 noMargin>{i18n.childInformation.backupPickups.title}</H3>
            {permittedActions.has('CREATE_BACKUP_PICKUP') && (
              <AddButton
                text={i18n.childInformation.backupPickups.add}
                onClick={() => toggleUiMode('create-backup-pickup')}
                data-qa="create-backup-pickup-btn"
              />
            )}
          </FlexRow>
          {backupPickups.value.length > 0 && (
            <Table>
              <Thead>
                <Tr>
                  <Th>{i18n.childInformation.backupPickups.name}</Th>
                  <Th>{i18n.childInformation.backupPickups.phone}</Th>
                  <RequireRole oneOf={['ADMIN', 'UNIT_SUPERVISOR', 'STAFF']}>
                    <Th />
                  </RequireRole>
                </Tr>
              </Thead>
              <Tbody>
                {backupPickups.value.map((row) => (
                  <Tr
                    key={row.id}
                    data-qa={`table-backup-pickup-row-${row.name}`}
                  >
                    <Td data-qa="backup-pickup-name">{row.name}</Td>
                    <Td>{row.phone}</Td>
                    <RequireRole oneOf={['ADMIN', 'UNIT_SUPERVISOR', 'STAFF']}>
                      <Td>
                        <FixedSpaceRowAlignRight>
                          <IconOnlyButton
                            icon={faPen}
                            onClick={() => openEditBackupPickupModal(row)}
                            data-qa="edit-backup-pickup"
                            aria-label={i18n.common.edit}
                          />
                          <IconOnlyButton
                            icon={faTrash}
                            onClick={() => openRemoveBackupPickupModal(row)}
                            data-qa="delete-backup-pickup"
                            aria-label={i18n.common.remove}
                          />
                        </FixedSpaceRowAlignRight>
                      </Td>
                    </RequireRole>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )}
        </>
      )}
      {uiMode === `create-backup-pickup` && <CreateBackupPickupModal />}
      {uiMode === `remove-backup-pickup` && <DeleteBackupPickupModal />}
      {uiMode === `edit-backup-pickup` && <EditBackupPickupModal />}
    </>
  )
}

const FixedSpaceRowAlignRight = styled(FixedSpaceRow)`
  justify-content: flex-end;
`

export default BackupPickup
