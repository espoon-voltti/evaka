// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  Fragment,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { orderBy } from 'lodash'
import ReactSelect, { components } from 'react-select'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

import { ContentArea } from '~components/shared/layout/Container'
import Loader from '~components/shared/atoms/Loader'
import Title from '~components/shared/atoms/Title'
import {
  Table,
  Th,
  Tr,
  Td,
  Thead,
  Tbody
} from '~components/shared/layout/Table'
import InfoModal from '~components/common/InfoModal'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import {
  addDaycareAclStaff,
  addDaycareAclSupervisor,
  DaycareAclRow,
  deleteMobileDevice,
  getDaycareAclRows,
  getMobileDevices,
  MobileDevice,
  putMobileDeviceName,
  removeDaycareAclStaff,
  removeDaycareAclSupervisor
} from '~api/unit'
import { getEmployees } from '~api/employees'
import { Employee } from '~types/employee'
import { UserContext } from '~state/user'
import { useRestApi } from '~utils/useRestApi'
import { RequireRole } from '~utils/roles'
import { useTranslation } from '~state/i18n'
import IconButton from '~components/shared/atoms/buttons/IconButton'
import { faPen, faPlusCircle, faQuestion, faTrash } from '~icon-set'
import { H2 } from '~components/shared/Typography'
import Button from '~components/shared/atoms/buttons/Button'
import { UUID } from '~types'
import { UIContext } from '~state/ui'
import { formatName } from '~utils'
import AddButton from '~components/shared/atoms/buttons/AddButton'
import { Gap } from '~components/shared/layout/white-space'
import MobilePairingModal from '../MobilePairingModal'
import { FixedSpaceRow } from '~components/shared/layout/flex-helpers'
import InputField from '~components/shared/atoms/form/InputField'
import { isNotProduction } from '~constants'

type Props = { unitId: string }

interface FormattedRow {
  id: UUID
  name: string
  email: string
}

const Flex = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
`

function AclRow({
  row,
  isDeletable,
  onClickDelete
}: {
  row: FormattedRow
  isDeletable: boolean
  onClickDelete: () => void
}) {
  return (
    <Tr data-qa="acl-row">
      <Td data-qa="name">{row.name}</Td>
      <Td data-qa="email">{row.email}</Td>
      <Td>
        {isDeletable && (
          <IconButton icon={faTrash} onClick={onClickDelete} data-qa="delete" />
        )}
      </Td>
    </Tr>
  )
}

const AddAclTitleContainer = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: 20px;

  > svg {
    flex: 0 0 auto;
    margin-right: 10px;
  }
`

const AddAclSelectContainer = styled.div`
  display: flex;
  align-items: center;

  > :nth-child(1) {
    width: 400px;
  }

  > button {
    margin-left: 20px;
    flex: 0 0 auto;
  }
`

function AclTable({
  rows,
  onDeleteAclRow
}: {
  rows: FormattedRow[]
  onDeleteAclRow: (employeeId: UUID) => void
}) {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)

  return (
    <Table data-qa="acl-table">
      <Thead>
        <Tr>
          <Th>{i18n.common.form.name}</Th>
          <Th>{i18n.unit.accessControl.email}</Th>
          <Th />
        </Tr>
      </Thead>
      <Tbody>
        {rows.map((row) => (
          <AclRow
            key={row.id}
            row={row}
            isDeletable={row.id !== user?.id}
            onClickDelete={() => onDeleteAclRow(row.id)}
          />
        ))}
      </Tbody>
    </Table>
  )
}

function DevicesTable({
  rows,
  onEditDevice,
  onDeleteDevice
}: {
  rows: Result<MobileDevice[]>
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
        {isSuccess(rows) &&
          rows.data.map((row) => (
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
}

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
  return (
    <Tr data-qa="device-row">
      <Td data-qa="name">{row.name}</Td>
      {/* <Td data-qa="last-used">{row.lastUsed}</Td> */}
      <Td>
        <FixedSpaceRowAlignRight>
          <IconButton
            icon={faPen}
            onClick={onClickEdit}
            data-qa="edit-mobile-device"
          />
          <IconButton
            icon={faTrash}
            onClick={onClickDelete}
            data-qa="delete-mobile-device"
          />
        </FixedSpaceRowAlignRight>
      </Td>
    </Tr>
  )
}

function AddAcl({
  employees,
  onAddAclRow
}: {
  employees: Employee[]
  onAddAclRow: (employeeId: UUID) => void
}) {
  const { i18n } = useTranslation()
  const [selectedEmployee, setSelectedEmployee] = useState<{
    label: string
    value: string
  } | null>(null)
  useEffect(() => {
    if (
      selectedEmployee &&
      !employees.some((e) => e.id === selectedEmployee.value)
    ) {
      setSelectedEmployee(null)
    }
  }, [employees, selectedEmployee, setSelectedEmployee])

  const options = useMemo(
    () =>
      employees.map(({ id, email, firstName, lastName }) => {
        const name = formatName(firstName, lastName, i18n)
        return {
          label: email ? `${email} (${name})` : name,
          value: id
        }
      }),
    [i18n, employees]
  )

  return (
    <>
      <AddAclTitleContainer>
        <FontAwesomeIcon icon={faPlusCircle} size="lg" />
        <Title size={3}>{i18n.unit.accessControl.addPerson}</Title>
      </AddAclTitleContainer>
      <AddAclSelectContainer>
        <ReactSelect
          className="acl-select"
          placeholder={i18n.unit.accessControl.choosePerson}
          value={selectedEmployee}
          components={{
            Option: function Option(props) {
              const { value } = props.data as { value: string }
              return (
                <div data-qa={`value-${value}`}>
                  <components.Option {...props} data-qa={`value-${value}`} />
                </div>
              )
            }
          }}
          onChange={(employee) =>
            setSelectedEmployee(
              employee && Array.isArray(employee)
                ? employee[0]
                : employee ?? null
            )
          }
          options={options}
          noOptionsMessage={() => i18n.common.noResults}
        />
        <Button
          dataQa="acl-add-button"
          disabled={!selectedEmployee}
          onClick={() => {
            if (selectedEmployee) {
              onAddAclRow(selectedEmployee.value)
            }
          }}
          text={i18n.common.add}
        />
      </AddAclSelectContainer>
    </>
  )
}

interface RemoveState {
  employeeId: UUID
  removeFn: (unitId: UUID, employeeId: UUID) => Promise<unknown>
}

function UnitAccessControl({ unitId }: Props) {
  const { i18n } = useTranslation()

  const { user } = useContext(UserContext)
  const [employees, setEmployees] = useState<Result<Employee[]>>(Loading())
  const [daycareAclRows, setDaycareAclRows] = useState<Result<DaycareAclRow[]>>(
    Loading()
  )
  const [mobileDevices, setMobileDevices] = useState<Result<MobileDevice[]>>(
    Loading()
  )
  const [mobileId, setMobileId] = useState<UUID | undefined>(undefined)
  const loading = isLoading(daycareAclRows) || isLoading(employees)
  const failed = isFailure(daycareAclRows) || isFailure(employees)

  const reloadEmployees = useRestApi(getEmployees, setEmployees)
  const reloadDaycareAclRows = useRestApi(getDaycareAclRows, setDaycareAclRows)
  const reloadMobileDevices = useRestApi(getMobileDevices, setMobileDevices)
  useEffect(() => reloadEmployees(), [])
  useEffect(() => reloadDaycareAclRows(unitId), [unitId])
  useEffect(() => reloadMobileDevices(unitId), [unitId])

  const candidateEmployees = useMemo(
    () =>
      isSuccess(employees) && isSuccess(daycareAclRows)
        ? orderBy(
            employees.data.filter(
              (employee) =>
                employee.id !== user?.id &&
                !daycareAclRows.data.some(
                  (row) => row.employee.id === employee.id
                )
            ),
            [(row) => row.email, (row) => row.id]
          )
        : [],
    [employees, daycareAclRows]
  )

  function formatRows(rows: DaycareAclRow[]): FormattedRow[] {
    return orderBy(
      rows.map((row) => ({
        id: row.employee.id,
        name: formatName(row.employee.firstName, row.employee.lastName, i18n),
        email: row.employee.email ?? row.employee.id
      })),
      (row) => row.name
    )
  }

  const unitSupervisors = useMemo(
    () =>
      isSuccess(daycareAclRows)
        ? formatRows(
            daycareAclRows.data.filter(({ role }) => role === 'UNIT_SUPERVISOR')
          )
        : undefined,
    [daycareAclRows]
  )
  const staff = useMemo(
    () =>
      isSuccess(daycareAclRows)
        ? formatRows(daycareAclRows.data.filter(({ role }) => role === 'STAFF'))
        : undefined,
    [daycareAclRows]
  )

  const addUnitSupervisor = (employeeId: UUID) =>
    addDaycareAclSupervisor(unitId, employeeId).then(() =>
      reloadDaycareAclRows(unitId)
    )

  const addStaff = (employeeId: UUID) =>
    addDaycareAclStaff(unitId, employeeId).then(() =>
      reloadDaycareAclRows(unitId)
    )

  const [removeState, setRemoveState] = useState<RemoveState | undefined>(
    undefined
  )
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)

  const openRemoveModal = (removeState: RemoveState) => {
    setRemoveState(removeState)
    toggleUiMode(`remove-daycare-acl-${unitId}`)
  }
  const closeRemoveModal = () => {
    clearUiMode()
    setRemoveState(undefined)
  }
  const confirmRemoveModal = async () => {
    await removeState?.removeFn(unitId, removeState.employeeId)
    closeRemoveModal()
    reloadDaycareAclRows(unitId)
  }
  const confirmRemoveDevice = async () => {
    if (mobileId) {
      await deleteMobileDevice(mobileId)
      reloadMobileDevices(unitId)
    }
    closeRemoveModal()
  }

  const openRemoveMobileDeviceModal = (id: UUID) => {
    setMobileId(id)
    toggleUiMode(`remove-daycare-mobile-device-${unitId}`)
  }

  const openEditMobileDeviceModal = (id: UUID) => {
    setMobileId(id)
    toggleUiMode(`edit-daycare-mobile-device-${unitId}`)
  }

  const openPairMobileDeviceModal = () => {
    toggleUiMode(`pair-daycare-mobile-device-${unitId}`)
  }
  const closePairMobileDeviceModal = () => {
    reloadMobileDevices(unitId)
    clearUiMode()
  }

  const DeleteConfirmationModal = () => (
    <InfoModal
      iconColour={'orange'}
      title={i18n.unit.accessControl.removeConfirmation}
      resolveLabel={i18n.common.remove}
      rejectLabel={i18n.common.cancel}
      icon={faQuestion}
      reject={closeRemoveModal}
      resolve={confirmRemoveModal}
    />
  )

  const DeleteMobileDeviceConfirmationModal = () => (
    <InfoModal
      iconColour={'orange'}
      title={i18n.unit.accessControl.mobileDevices.removeConfirmation}
      resolveLabel={i18n.common.remove}
      rejectLabel={i18n.common.cancel}
      icon={faQuestion}
      reject={closeRemoveModal}
      resolve={confirmRemoveDevice}
    />
  )

  const EditMobileDeviceModal = () => {
    const [name, setName] = useState<string>('')

    async function saveDevice() {
      if (mobileId) {
        await putMobileDeviceName(mobileId, name)
        reloadMobileDevices(unitId)
        clearUiMode()
      }
    }
    return (
      <InfoModal
        iconColour={'blue'}
        title={i18n.unit.accessControl.mobileDevices.editName}
        resolveLabel={i18n.common.save}
        rejectLabel={i18n.common.cancel}
        icon={faQuestion}
        reject={closeRemoveModal}
        resolve={() => saveDevice()}
      >
        <Flex>
          <InputField
            value={name}
            onChange={setName}
            placeholder={i18n.unit.accessControl.mobileDevices.editPlaceholder}
            width={'m'}
          />
        </Flex>
      </InfoModal>
    )
  }

  return (
    <>
      {uiMode === `remove-daycare-acl-${unitId}` && <DeleteConfirmationModal />}
      {uiMode === `edit-daycare-mobile-device-${unitId}` && (
        <EditMobileDeviceModal />
      )}
      {uiMode === `remove-daycare-mobile-device-${unitId}` && (
        <DeleteMobileDeviceConfirmationModal />
      )}
      {uiMode === `pair-daycare-mobile-device-${unitId}` && (
        <MobilePairingModal
          unitId={unitId}
          closeModal={closePairMobileDeviceModal}
        />
      )}
      <RequireRole oneOf={['ADMIN']}>
        <ContentArea opaque data-qa="daycare-acl-supervisors">
          <H2>{i18n.unit.accessControl.unitSupervisors}</H2>
          {loading && <Loader />}
          {failed && !loading && <div>{i18n.common.loadingFailed}</div>}
          {!failed && !loading && unitSupervisors && (
            <AclTable
              rows={unitSupervisors}
              onDeleteAclRow={(employeeId) =>
                openRemoveModal({
                  employeeId,
                  removeFn: removeDaycareAclSupervisor
                })
              }
            />
          )}
          <AddAcl
            employees={candidateEmployees}
            onAddAclRow={addUnitSupervisor}
          />
        </ContentArea>
      </RequireRole>
      <RequireRole oneOf={['ADMIN', 'UNIT_SUPERVISOR']}>
        <ContentArea opaque data-qa="daycare-acl-staff">
          <H2>{i18n.unit.accessControl.staff}</H2>
          {loading && <Loader />}
          {failed && !loading && <div>{i18n.common.loadingFailed}</div>}
          {!failed && !loading && staff && (
            <AclTable
              rows={staff}
              onDeleteAclRow={(employeeId) =>
                openRemoveModal({
                  employeeId,
                  removeFn: removeDaycareAclStaff
                })
              }
            />
          )}
          <AddAcl employees={candidateEmployees} onAddAclRow={addStaff} />
        </ContentArea>
      </RequireRole>

      {isNotProduction && (
        <RequireRole oneOf={['ADMIN', 'UNIT_SUPERVISOR']}>
          <ContentArea opaque data-qa="daycare-mobile-devices">
            <H2>{i18n.unit.accessControl.mobileDevices.mobileDevices}</H2>
            {loading && <Loader />}
            {failed && !loading && <div>{i18n.common.loadingFailed}</div>}
            {!failed && !loading && staff && (
              <Fragment>
                <DevicesTable
                  rows={mobileDevices}
                  onEditDevice={(id) => openEditMobileDeviceModal(id)}
                  onDeleteDevice={(id) => openRemoveMobileDeviceModal(id)}
                />
                <Gap />
                <AddButton
                  text={i18n.unit.accessControl.mobileDevices.addMobileDevice}
                  onClick={() => openPairMobileDeviceModal()}
                  data-qa="start-mobile-pairing"
                />
              </Fragment>
            )}
          </ContentArea>
        </RequireRole>
      )}
    </>
  )
}

export default UnitAccessControl
