// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useContext, useState } from 'react'
import styled from 'styled-components'

import { isLoading, wrapResult } from 'lib-common/api'
import { MobileDevice } from 'lib-common/generated/api-types/pairing'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import InputField from 'lib-components/atoms/form/InputField'
import { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPen, faQuestion, faTrash } from 'lib-icons'

import {
  deleteMobileDevice,
  getMobileDevices,
  putMobileDeviceName
} from '../../../generated/api-clients/pairing'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { UnitContext } from '../../../state/unit'
import { renderResult } from '../../async-rendering'

const getMobileDevicesResult = wrapResult(getMobileDevices)
const deleteMobileDeviceResult = wrapResult(deleteMobileDevice)
const putMobileDeviceNameResult = wrapResult(putMobileDeviceName)

type Props = {
  canAddNew: boolean
}

const Flex = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
`
const DevicesTable = React.memo(function DevicesTable({
  rows,
  onEditDevice,
  onDeleteDevice
}: {
  rows: MobileDevice[]
  onEditDevice: (deviceId: UUID) => void
  onDeleteDevice: (deviceId: UUID) => void
}) {
  const { i18n } = useTranslation()

  return (
    <Table data-qa="mobile-devices-table">
      <Thead>
        <Tr>
          <Th>{i18n.common.form.name}</Th>
          <Th />
        </Tr>
      </Thead>
      <Tbody>
        {sortBy(rows, (row) => row.name).map((row) => (
          <DeviceRow
            key={row.id}
            row={row}
            onClickEdit={() => onEditDevice(row.id)}
            onClickDelete={() => onDeleteDevice(row.id)}
          />
        ))}
      </Tbody>
    </Table>
  )
})

const FixedSpaceRowAlignRight = styled(FixedSpaceRow)`
  justify-content: flex-end;
`

function DeviceRow({
  row,
  onClickEdit,
  onClickDelete
}: {
  row: MobileDevice
  onClickEdit: () => void
  onClickDelete: () => void
}) {
  const { i18n } = useTranslation()

  return (
    <Tr data-qa="device-row">
      <Td data-qa="name">{row.name}</Td>
      <Td>
        <FixedSpaceRowAlignRight>
          <IconOnlyButton
            icon={faPen}
            onClick={onClickEdit}
            data-qa="edit-mobile-device"
            aria-label={i18n.common.edit}
          />
          <IconOnlyButton
            icon={faTrash}
            onClick={onClickDelete}
            data-qa="delete-mobile-device"
            aria-label={i18n.common.remove}
          />
        </FixedSpaceRowAlignRight>
      </Td>
    </Tr>
  )
}

const DeleteMobileDeviceConfirmationModal = React.memo(
  function DeleteMobileDeviceConfirmationModal({
    onClose,
    onConfirm
  }: {
    onClose: () => void
    onConfirm: () => void
  }) {
    const { i18n } = useTranslation()
    return (
      <InfoModal
        type="warning"
        title={i18n.unit.accessControl.mobileDevices.removeConfirmation}
        icon={faQuestion}
        reject={{ action: onClose, label: i18n.common.cancel }}
        resolve={{ action: onConfirm, label: i18n.common.remove }}
      />
    )
  }
)

const EditMobileDeviceModal = React.memo(function EditMobileDeviceModal({
  onClose,
  onConfirm
}: {
  onClose: () => void
  onConfirm: (name: string) => void
}) {
  const { i18n } = useTranslation()
  const [name, setName] = useState<string>('')

  return (
    <InfoModal
      type="info"
      title={i18n.unit.accessControl.mobileDevices.editName}
      icon={faQuestion}
      reject={{ action: onClose, label: i18n.common.cancel }}
      resolve={{ action: () => onConfirm(name), label: i18n.common.save }}
    >
      <Flex>
        <InputField
          value={name}
          onChange={setName}
          placeholder={i18n.unit.accessControl.mobileDevices.editPlaceholder}
          width="m"
        />
      </Flex>
    </InfoModal>
  )
})

export default React.memo(function UnitMobileDevices({ canAddNew }: Props) {
  const { i18n } = useTranslation()

  const { unitId } = useContext(UnitContext)

  const [mobileDevices, reloadMobileDevices] = useApiState(
    () => getMobileDevicesResult({ unitId }),
    [unitId]
  )
  const [mobileId, setMobileId] = useState<UUID | undefined>(undefined)

  const { uiMode, toggleUiMode, clearUiMode, startPairing } =
    useContext(UIContext)

  const confirmRemoveDevice = useCallback(async () => {
    if (mobileId) {
      await deleteMobileDeviceResult({ id: mobileId })
      void reloadMobileDevices()
    }
    clearUiMode()
  }, [clearUiMode, mobileId, reloadMobileDevices])

  const openRemoveMobileDeviceModal = useCallback(
    (id: UUID) => {
      setMobileId(id)
      toggleUiMode(`remove-daycare-mobile-device-${unitId}`)
    },
    [toggleUiMode, unitId]
  )

  const openEditMobileDeviceModal = useCallback(
    (id: UUID) => {
      setMobileId(id)
      toggleUiMode(`edit-daycare-mobile-device-${unitId}`)
    },
    [toggleUiMode, unitId]
  )

  const openPairMobileDeviceModal = useCallback(() => {
    startPairing({ unitId }, reloadMobileDevices)
  }, [startPairing, unitId, reloadMobileDevices])

  const saveMobileDevice = useCallback(
    async (name: string) => {
      if (mobileId) {
        await putMobileDeviceNameResult({ id: mobileId, body: { name } })
        void reloadMobileDevices()
        clearUiMode()
      }
    },
    [clearUiMode, mobileId, reloadMobileDevices]
  )

  return (
    <div data-isloading={isLoading(mobileDevices)}>
      {uiMode === `edit-daycare-mobile-device-${unitId}` && (
        <EditMobileDeviceModal
          onClose={clearUiMode}
          onConfirm={saveMobileDevice}
        />
      )}
      {uiMode === `remove-daycare-mobile-device-${unitId}` && (
        <DeleteMobileDeviceConfirmationModal
          onClose={clearUiMode}
          onConfirm={confirmRemoveDevice}
        />
      )}
      <ContentArea opaque data-qa="daycare-mobile-devices">
        <H2>{i18n.unit.accessControl.mobileDevices.mobileDevices}</H2>
        {renderResult(mobileDevices, (mobileDevices) => (
          <>
            <DevicesTable
              rows={mobileDevices}
              onEditDevice={openEditMobileDeviceModal}
              onDeleteDevice={openRemoveMobileDeviceModal}
            />
            {canAddNew && (
              <>
                <Gap />
                <AddButton
                  text={i18n.unit.accessControl.mobileDevices.addMobileDevice}
                  onClick={openPairMobileDeviceModal}
                  data-qa="start-mobile-pairing"
                />
              </>
            )}
          </>
        ))}
      </ContentArea>
    </div>
  )
})
