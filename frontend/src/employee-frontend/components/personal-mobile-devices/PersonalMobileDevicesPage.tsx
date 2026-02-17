// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import React, { useCallback, useContext, useState } from 'react'

import type { MobileDeviceId } from 'lib-common/generated/api-types/shared'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import InputField from 'lib-components/atoms/form/InputField'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H1, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPen, faQuestion, faTrash } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'

import {
  deletePersonalMobileDeviceMutation,
  personalMobileDevicesQuery,
  putPersonalMobileDeviceNameMutation
} from './queries'

export default React.memo(function PersonalMobileDevicesPage() {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)
  const { startPairing } = useContext(UIContext)
  const queryClient = useQueryClient()
  const mobileDevices = useQueryResult(personalMobileDevicesQuery())
  const [openModal, setOpenModal] = useState<{
    id: MobileDeviceId
    action: 'rename' | 'delete'
    currentName?: string
  }>()

  const invalidateDevices = useCallback(
    () => void queryClient.invalidateQueries(personalMobileDevicesQuery()),
    [queryClient]
  )
  const pairNewDevice = useCallback(
    () =>
      user
        ? startPairing({ employeeId: user?.id }, invalidateDevices)
        : undefined,
    [user, startPairing, invalidateDevices]
  )
  const closeModal = useCallback(() => {
    setOpenModal(undefined)
  }, [])

  if (!user) {
    return null
  }

  return (
    <Container>
      <ContentArea opaque>
        <H1>{i18n.personalMobileDevices.title}</H1>
        <P>{i18n.personalMobileDevices.infoParagraph1}</P>
        <P>{i18n.personalMobileDevices.infoParagraph2}</P>
        {renderResult(mobileDevices, (devices) => (
          <>
            <Table>
              <Thead>
                <Tr>
                  <Th>{i18n.personalMobileDevices.name}</Th>
                  <Th align="right" />
                </Tr>
              </Thead>
              <Tbody>
                {devices.map(({ id, name }) => (
                  <Tr key={id}>
                    <Td>{name}</Td>
                    <Td align="right">
                      <FixedSpaceRow justifyContent="flex-end" spacing="L">
                        <IconOnlyButton
                          icon={faPen}
                          onClick={() =>
                            setOpenModal({
                              id,
                              action: 'rename',
                              currentName: name
                            })
                          }
                          disabled={openModal !== undefined}
                          aria-label={i18n.common.edit}
                        />
                        <IconOnlyButton
                          icon={faTrash}
                          onClick={() => setOpenModal({ id, action: 'delete' })}
                          disabled={openModal !== undefined}
                          aria-label={i18n.common.remove}
                        />
                      </FixedSpaceRow>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
            <Gap size="m" />
            <AddButton
              onClick={pairNewDevice}
              text={i18n.personalMobileDevices.addDevice}
            />
          </>
        ))}
      </ContentArea>
      {openModal && openModal.action === 'rename' && (
        <EditNameModal
          id={openModal.id}
          close={closeModal}
          currentName={openModal.currentName ?? ''}
        />
      )}
      {openModal && openModal.action === 'delete' && (
        <DeleteModal id={openModal.id} close={closeModal} />
      )}
    </Container>
  )
})

const EditNameModal = React.memo(function EditNameModal({
  id,
  close,
  currentName
}: {
  id: MobileDeviceId
  close: () => void
  currentName: string
}) {
  const { i18n } = useTranslation()
  const { mutateAsync: doRenameMobileDevice } = useMutationResult(
    putPersonalMobileDeviceNameMutation
  )
  const [newName, setNewName] = useState(currentName)

  return (
    <InfoModal
      title={i18n.personalMobileDevices.editName}
      icon={faQuestion}
      type="info"
      reject={{ action: close, label: i18n.common.cancel }}
      resolve={{
        action: () =>
          doRenameMobileDevice({ id, body: { name: newName } }).then(() =>
            close()
          ),
        label: i18n.common.save
      }}
    >
      <FixedSpaceColumn alignItems="center">
        <InputField
          value={newName}
          onChange={setNewName}
          placeholder={i18n.mobilePairingModal.namePlaceholder}
          width="m"
        />
      </FixedSpaceColumn>
    </InfoModal>
  )
})

const DeleteModal = React.memo(function DeleteModal({
  id,
  close
}: {
  id: MobileDeviceId
  close: () => void
}) {
  const { i18n } = useTranslation()
  const { mutateAsync: doDeleteMobileDevice } = useMutationResult(
    deletePersonalMobileDeviceMutation
  )

  return (
    <InfoModal
      title={i18n.personalMobileDevices.deleteDevice}
      icon={faQuestion}
      type="warning"
      reject={{ action: close, label: i18n.common.cancel }}
      resolve={{
        action: () => doDeleteMobileDevice({ id }).then(() => close()),
        label: i18n.common.confirm
      }}
    />
  )
})
