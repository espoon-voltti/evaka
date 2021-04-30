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

import { ContentArea } from 'lib-components/layout/Container'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import { Table, Th, Tr, Td, Thead, Tbody } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Loading, Result, Success } from 'lib-common/api'
import {
  addDaycareAclStaff,
  addDaycareAclSupervisor,
  addDaycareAclSpecialEducationTeacher,
  DaycareAclRow,
  deleteMobileDevice,
  getDaycareAclRows,
  getMobileDevices,
  MobileDevice,
  putMobileDeviceName,
  removeDaycareAclStaff,
  removeDaycareAclSupervisor,
  removeDaycareAclSpecialEducationTeacher,
  DaycareGroupSummary,
  updateDaycareGroupAcl
} from '../../../api/unit'
import { getEmployees } from '../../../api/employees'
import { Employee } from '../../../types/employee'
import { UserContext } from '../../../state/user'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { RequireRole } from '../../../utils/roles'
import { useTranslation } from '../../../state/i18n'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { faPen, faQuestion, faTrash } from 'lib-icons'
import { H2 } from 'lib-components/typography'
import Button from 'lib-components/atoms/buttons/Button'
import { UUID } from '../../../types'
import { UIContext } from '../../../state/ui'
import { formatName } from '../../../utils'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Gap } from 'lib-components/white-space'
import MobilePairingModal from '../MobilePairingModal'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import InputField from 'lib-components/atoms/form/InputField'
import { isNotProduction, isPilotUnit } from '../../../constants'
import { AdRole } from '../../../types'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'

type Props = {
  unitId: string
  groups: DaycareGroupSummary[]
}

interface FormattedRow {
  id: UUID
  name: string
  email: string
  groupIds: UUID[]
}

const Flex = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
`

function AclRow({
  row,
  isDeletable,
  onClickDelete,
  onChangeGroups,
  rolesAllowedToEdit,
  unitGroups
}: {
  row: FormattedRow
  isDeletable: boolean
  onClickDelete: () => void
  onChangeGroups: (ids: UUID[]) => Promise<Result<void>>
  rolesAllowedToEdit?: AdRole[]
  unitGroups: DaycareGroupSummary[] | undefined
}) {
  const [saved, setSaved] = useState(true)
  const saveGroups = useRestApi(onChangeGroups, (result) => {
    setSaved(!result.isLoading)
  })

  return (
    <Tr data-qa="acl-row" data-saved={saved ? true : undefined}>
      <Td data-qa="name">{row.name}</Td>
      <Td data-qa="email">{row.email}</Td>
      {unitGroups && (
        <GroupMultiSelectTd>
          <GroupMultiSelect
            unitGroups={unitGroups}
            value={row.groupIds}
            onChange={saveGroups}
          />
        </GroupMultiSelectTd>
      )}
      <Td>
        {rolesAllowedToEdit ? (
          <RequireRole oneOf={rolesAllowedToEdit}>
            {isDeletable && (
              <IconButton
                icon={faTrash}
                onClick={onClickDelete}
                data-qa="delete"
              />
            )}
          </RequireRole>
        ) : (
          <>
            {isDeletable && (
              <IconButton
                icon={faTrash}
                onClick={onClickDelete}
                data-qa="delete"
              />
            )}
          </>
        )}
      </Td>
    </Tr>
  )
}

const GroupMultiSelectTd = styled(Td)`
  padding: 0;
  vertical-align: middle;
`

function GroupMultiSelect({
  unitGroups,
  value: initialValue,
  onChange
}: {
  unitGroups: DaycareGroupSummary[]
  value: UUID[]
  onChange: (groupIds: UUID[]) => void
}) {
  const { i18n } = useTranslation()
  const [value, setValue] = useState(
    unitGroups.filter(({ id }) => initialValue.includes(id))
  )

  return (
    <MultiSelect
      data-qa="groups"
      value={value}
      options={unitGroups}
      getOptionId={(x) => x.id}
      getOptionLabel={(x) => x.name}
      onChange={(values) => {
        setValue(values)
        onChange(values.map(({ id }) => id))
      }}
      placeholder={`${i18n.common.select}...`}
    />
  )
}

const AddAclTitleContainer = styled.div`
  display: flex;
  align-items: center;

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

  & button {
    margin-left: 20px;
    flex: 0 0 auto;
  }
`

function AclTable({
  unitGroups,
  rows,
  onDeleteAclRow,
  onChangeAclGroups,
  rolesAllowedToEdit
}: {
  unitGroups?: DaycareGroupSummary[]
  rows: FormattedRow[]
  onDeleteAclRow: (employeeId: UUID) => void
  onChangeAclGroups?: (
    employeeId: UUID,
    groupIds: UUID[]
  ) => Promise<Result<void>>
  rolesAllowedToEdit?: AdRole[]
}) {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)

  return (
    <Table data-qa="acl-table">
      <Thead>
        <Tr>
          <Th>{i18n.common.form.name}</Th>
          <Th>{i18n.unit.accessControl.email}</Th>
          {unitGroups && <Th>{i18n.unit.accessControl.groups}</Th>}
          <Th />
        </Tr>
      </Thead>
      <Tbody>
        {rows.map((row) => (
          <AclRow
            key={row.id}
            unitGroups={unitGroups}
            row={row}
            isDeletable={row.id !== user?.id}
            onClickDelete={() => onDeleteAclRow(row.id)}
            onChangeGroups={
              onChangeAclGroups
                ? (ids) => onChangeAclGroups(row.id, ids)
                : () => Promise.resolve(Success.of(undefined))
            }
            rolesAllowedToEdit={rolesAllowedToEdit}
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
        {rows
          .map((rs) =>
            rs.map((row) => (
              <DeviceRow
                key={row.id}
                row={row}
                onClickEdit={() => onEditDevice(row.id)}
                onClickDelete={() => onDeleteDevice(row.id)}
              />
            ))
          )
          .getOrElse(null)}
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
          data-qa="acl-add-button"
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

function UnitAccessControl({ unitId, groups }: Props) {
  const { i18n } = useTranslation()

  const { user } = useContext(UserContext)
  const [employees, setEmployees] = useState<Result<Employee[]>>(Loading.of())
  const [daycareAclRows, setDaycareAclRows] = useState<Result<DaycareAclRow[]>>(
    Loading.of()
  )
  const [mobileDevices, setMobileDevices] = useState<Result<MobileDevice[]>>(
    Loading.of()
  )
  const [mobileId, setMobileId] = useState<UUID | undefined>(undefined)
  const loading = daycareAclRows.isLoading || employees.isLoading
  const failed = daycareAclRows.isFailure || employees.isFailure

  const reloadEmployees = useRestApi(getEmployees, setEmployees)
  const reloadDaycareAclRows = useRestApi(getDaycareAclRows, setDaycareAclRows)
  const reloadMobileDevices = useRestApi(getMobileDevices, setMobileDevices)
  useEffect(() => reloadEmployees(), [])
  useEffect(() => reloadDaycareAclRows(unitId), [unitId])
  useEffect(() => reloadMobileDevices(unitId), [unitId])

  const candidateEmployees = useMemo(
    () =>
      employees.isSuccess && daycareAclRows.isSuccess
        ? orderBy(
            employees.value.filter(
              (employee) =>
                employee.id !== user?.id &&
                !daycareAclRows.value.some(
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
        email: row.employee.email ?? row.employee.id,
        groupIds: row.groupIds
      })),
      (row) => row.name
    )
  }

  const unitSupervisors = useMemo(
    () =>
      daycareAclRows.isSuccess
        ? formatRows(
            daycareAclRows.value.filter(
              ({ role }) => role === 'UNIT_SUPERVISOR'
            )
          )
        : undefined,
    [daycareAclRows]
  )

  const specialEducationTeachers = useMemo(
    () =>
      daycareAclRows.isSuccess
        ? formatRows(
            daycareAclRows.value.filter(
              ({ role }) => role === 'SPECIAL_EDUCATION_TEACHER'
            )
          )
        : undefined,
    [daycareAclRows]
  )
  const staff = useMemo(
    () =>
      daycareAclRows.isSuccess
        ? formatRows(
            daycareAclRows.value.filter(({ role }) => role === 'STAFF')
          )
        : undefined,
    [daycareAclRows]
  )

  const addUnitSupervisor = (employeeId: UUID) =>
    addDaycareAclSupervisor(unitId, employeeId).then(() =>
      reloadDaycareAclRows(unitId)
    )

  const addSpecialEducationTeacher = (employeeId: UUID) =>
    addDaycareAclSpecialEducationTeacher(unitId, employeeId).then(() =>
      reloadDaycareAclRows(unitId)
    )

  const addStaff = (employeeId: UUID) =>
    addDaycareAclStaff(unitId, employeeId).then(() =>
      reloadDaycareAclRows(unitId)
    )

  const updateStaffGroupAcls = (employeeId: UUID, groupIds: UUID[]) =>
    updateDaycareGroupAcl(unitId, employeeId, groupIds)

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
      icon={faQuestion}
      reject={{ action: closeRemoveModal, label: i18n.common.cancel }}
      resolve={{ action: confirmRemoveModal, label: i18n.common.remove }}
    />
  )

  const DeleteMobileDeviceConfirmationModal = () => (
    <InfoModal
      iconColour={'orange'}
      title={i18n.unit.accessControl.mobileDevices.removeConfirmation}
      icon={faQuestion}
      reject={{ action: closeRemoveModal, label: i18n.common.cancel }}
      resolve={{ action: confirmRemoveDevice, label: i18n.common.remove }}
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
        icon={faQuestion}
        reject={{ action: closeRemoveModal, label: i18n.common.cancel }}
        resolve={{ action: () => saveDevice(), label: i18n.common.save }}
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
      <RequireRole oneOf={['ADMIN', 'UNIT_SUPERVISOR']}>
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
              rolesAllowedToEdit={['ADMIN']}
            />
          )}
          <RequireRole oneOf={['ADMIN']}>
            <AddAcl
              employees={candidateEmployees}
              onAddAclRow={addUnitSupervisor}
            />
          </RequireRole>
        </ContentArea>
        <ContentArea opaque data-qa="daycare-acl-set">
          <H2>{i18n.unit.accessControl.specialEducationTeachers}</H2>
          {loading && <Loader />}
          {failed && !loading && <div>{i18n.common.loadingFailed}</div>}
          {!failed && !loading && specialEducationTeachers && (
            <AclTable
              rows={specialEducationTeachers}
              onDeleteAclRow={(employeeId) =>
                openRemoveModal({
                  employeeId,
                  removeFn: removeDaycareAclSpecialEducationTeacher
                })
              }
              rolesAllowedToEdit={['ADMIN']}
            />
          )}
          <RequireRole oneOf={['ADMIN']}>
            <AddAcl
              employees={candidateEmployees}
              onAddAclRow={addSpecialEducationTeacher}
            />
          </RequireRole>
        </ContentArea>
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
              unitGroups={groups}
              onChangeAclGroups={updateStaffGroupAcls}
            />
          )}
          <AddAcl employees={candidateEmployees} onAddAclRow={addStaff} />
        </ContentArea>
        {(isNotProduction() || isPilotUnit(unitId)) && (
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
        )}
      </RequireRole>
    </>
  )
}

export default UnitAccessControl
