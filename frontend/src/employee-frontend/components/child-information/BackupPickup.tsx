// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'

import { Loading, Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import InputField from 'lib-components/atoms/form/InputField'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { faPen, faQuestion, faTrash } from 'lib-icons'

import { UUID } from '../../types'
import {
  createBackupPickup,
  getChildBackupPickups,
  removeBackupPickup,
  updateBackupPickup
} from '../../api/child/backup-pickup'
import { useTranslation } from '../../state/i18n'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ChildBackupPickup } from '../../types/child'
import { UIContext } from '../../state/ui'
import { H2, Label } from 'lib-components/typography'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { RequireRole } from '../../utils/roles'
import { CollapsibleContentArea } from '../../../lib-components/layout/Container'

interface BackupPickupProps {
  id: UUID
  startOpen: boolean
}

function BackupPickup({ id, startOpen }: BackupPickupProps) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)

  const [result, setResult] = useState<Result<ChildBackupPickup[]>>(
    Loading.of()
  )
  const [backupPickup, setBackupPickup] = useState<
    ChildBackupPickup | undefined
  >(undefined)
  const [open, setOpen] = useState(startOpen)

  const loadBackupPickups = useRestApi(getChildBackupPickups, setResult)
  useEffect(() => loadBackupPickups(id), [id, loadBackupPickups])

  const openEditBackupPickupModal = (pickup: ChildBackupPickup) => {
    setBackupPickup(pickup)
    toggleUiMode(`edit-backup-pickup`)
  }

  const openRemoveBackupPickupModal = (pickup: ChildBackupPickup) => {
    setBackupPickup(pickup)
    toggleUiMode(`remove-backup-pickup`)
  }

  const confirmRemoveModal = async () => {
    if (result.isSuccess && backupPickup) {
      await removeBackupPickup(backupPickup.id, id)
      setBackupPickup(undefined)
      loadBackupPickups(id)
      clearUiMode()
    }
  }

  const CreateBackupPickupModal = () => {
    const [name, setName] = useState<string>('')
    const [phone, setPhone] = useState<string>('')

    async function saveBackupPickup() {
      if (name !== '' && phone !== '') {
        await createBackupPickup(id, { childId: id, name, phone })
        loadBackupPickups(id)
        setBackupPickup(undefined)
        clearUiMode()
      }
    }
    return (
      <FormModal
        title={i18n.childInformation.backupPickups.add}
        reject={{ action: () => clearUiMode(), label: i18n.common.cancel }}
        resolve={{ action: () => saveBackupPickup(), label: i18n.common.save }}
      >
        <FixedSpaceColumn>
          <FixedSpaceColumn spacing={'xxs'}>
            <Label>{i18n.childInformation.backupPickups.name}</Label>
            <InputField
              value={name}
              onChange={setName}
              placeholder={i18n.childInformation.backupPickups.name}
              width={'full'}
              data-qa="backup-pickup-name-input"
            />
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing={'xxs'}>
            <Label>{i18n.childInformation.backupPickups.phone}</Label>
            <InputField
              value={phone}
              onChange={setPhone}
              placeholder={i18n.childInformation.backupPickups.phone}
              width={'full'}
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
        await updateBackupPickup(id, {
          ...backupPickup,
          name: name !== '' ? name : backupPickup.name,
          phone: phone !== '' ? phone : backupPickup.phone
        })
        loadBackupPickups(id)
        setBackupPickup(undefined)
        clearUiMode()
      }
    }
    return (
      <FormModal
        title={i18n.childInformation.backupPickups.edit}
        reject={{ action: () => clearUiMode(), label: i18n.common.cancel }}
        resolve={{ action: () => saveBackupPickup(), label: i18n.common.save }}
      >
        <FixedSpaceColumn>
          <FixedSpaceColumn spacing={'xxs'}>
            <Label>{i18n.childInformation.backupPickups.name}</Label>
            <InputField
              value={name}
              onChange={setName}
              placeholder={i18n.childInformation.backupPickups.name}
              width={'full'}
              data-qa="backup-pickup-name-input"
            />
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing={'xxs'}>
            <Label>{i18n.childInformation.backupPickups.phone}</Label>
            <InputField
              value={phone}
              onChange={setPhone}
              placeholder={i18n.childInformation.backupPickups.phone}
              width={'full'}
              data-qa="backup-pickup-phone-input"
            />
          </FixedSpaceColumn>
        </FixedSpaceColumn>
      </FormModal>
    )
  }

  const DeleteBackupPickupModal = () => (
    <InfoModal
      iconColour={'orange'}
      title={i18n.childInformation.backupPickups.removeConfirmation}
      icon={faQuestion}
      reject={{ action: () => clearUiMode(), label: i18n.common.cancel }}
      resolve={{ action: confirmRemoveModal, label: i18n.common.remove }}
    />
  )

  return (
    <CollapsibleContentArea
      title={<H2 noMargin>{i18n.childInformation.backupPickups.title}</H2>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-qa="backup-pickups-collapsible"
    >
      {result.isLoading && <SpinnerSegment />}
      {result.isFailure && <ErrorSegment />}
      {result.isSuccess && (
        <>
          <RequireRole oneOf={['ADMIN', 'UNIT_SUPERVISOR', 'STAFF']}>
            <AddButtonRow
              text={i18n.childInformation.backupPickups.add}
              onClick={() => toggleUiMode('create-backup-pickup')}
              data-qa="create-backup-pickup-btn"
            />
          </RequireRole>
          {result.value.length > 0 && (
            <Table>
              <Thead>
                <Tr>
                  <Th>{i18n.childInformation.backupPickups.name}</Th>
                  <Th>{i18n.childInformation.backupPickups.phone}</Th>
                  <RequireRole oneOf={['ADMIN', 'UNIT_SUPERVISOR', 'STAFF']}>
                    <Th></Th>
                  </RequireRole>
                </Tr>
              </Thead>
              <Tbody>
                {result.value.map((row) => (
                  <Tr
                    key={row.id}
                    data-qa={`table-backup-pickup-row-${row.name}`}
                  >
                    <Td data-qa="backup-pickup-name">{row.name}</Td>
                    <Td>{row.phone}</Td>
                    <RequireRole oneOf={['ADMIN', 'UNIT_SUPERVISOR', 'STAFF']}>
                      <Td>
                        <FixedSpaceRowAlignRight>
                          <IconButton
                            icon={faPen}
                            onClick={() => openEditBackupPickupModal(row)}
                            data-qa="edit-backup-pickup"
                          />
                          <IconButton
                            icon={faTrash}
                            onClick={() => openRemoveBackupPickupModal(row)}
                            data-qa="delete-backup-pickup"
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
    </CollapsibleContentArea>
  )
}

const FixedSpaceRowAlignRight = styled(FixedSpaceRow)`
  justify-content: flex-end;
`

export default BackupPickup
