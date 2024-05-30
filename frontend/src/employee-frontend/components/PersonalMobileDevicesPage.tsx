// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useState } from 'react'

import { wrapResult } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
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

import {
  deleteMobileDevice,
  getPersonalMobileDevices,
  putMobileDeviceName
} from '../generated/api-clients/pairing'
import { useTranslation } from '../state/i18n'
import { UIContext } from '../state/ui'
import { UserContext } from '../state/user'

import { renderResult } from './async-rendering'

const getPersonalMobileDevicesResult = wrapResult(getPersonalMobileDevices)
const deleteMobileDeviceResult = wrapResult(deleteMobileDevice)
const putMobileDeviceNameResult = wrapResult(putMobileDeviceName)

export default React.memo(function PersonalMobileDevicesPage() {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)
  const { startPairing } = useContext(UIContext)
  const [mobileDevices, reloadDevices] = useApiState(
    () => getPersonalMobileDevicesResult(),
    []
  )
  const [openModal, setOpenModal] = useState<{
    id: UUID
    action: 'rename' | 'delete'
    currentName?: string
  }>()

  const pairNewDevice = useCallback(
    () =>
      user ? startPairing({ employeeId: user?.id }, reloadDevices) : undefined,
    [user, startPairing, reloadDevices]
  )
  const closeModal = useCallback(() => {
    setOpenModal(undefined)
    void reloadDevices()
  }, [reloadDevices])

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
  id: UUID
  close: () => void
  currentName: string
}) {
  const { i18n } = useTranslation()
  const [newName, setNewName] = useState(currentName)

  return (
    <InfoModal
      title={i18n.personalMobileDevices.editName}
      icon={faQuestion}
      type="info"
      reject={{ action: close, label: i18n.common.cancel }}
      resolve={{
        action: () =>
          putMobileDeviceNameResult({ id, body: { name: newName } }).then(
            close
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
  id: UUID
  close: () => void
}) {
  const { i18n } = useTranslation()

  return (
    <InfoModal
      title={i18n.personalMobileDevices.deleteDevice}
      icon={faQuestion}
      type="warning"
      reject={{ action: close, label: i18n.common.cancel }}
      resolve={{
        action: () => deleteMobileDeviceResult({ id }).then(close),
        label: i18n.common.confirm
      }}
    />
  )
})
