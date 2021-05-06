// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { sortBy } from 'lodash'
import React, {
  Fragment,
  useCallback,
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
import { Loading, Result } from 'lib-common/api'
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
import InlineButton from 'lib-components/atoms/buttons/InlineButton'

type Props = {
  unitId: string
  groups: Record<UUID, DaycareGroupSummary>
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

const RowButtons = styled.div`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  & > * {
    margin-left: 10px;
  }
`

function GroupListing({
  unitGroups,
  groupIds
}: {
  unitGroups: Record<UUID, DaycareGroupSummary>
  groupIds: UUID[]
}) {
  const { i18n } = useTranslation()
  const [expanded, setExpanded] = useState(false)
  const sortedIds = useMemo(
    () => sortBy(groupIds, (id) => unitGroups[id]?.name),
    [unitGroups, groupIds]
  )
  const shownIds = expanded ? sortedIds : sortedIds.slice(0, 3)
  const hiddenCount = sortedIds.length - shownIds.length
  return (
    <>
      {shownIds.map((id) => (
        <div key={id}>{unitGroups[id]?.name}</div>
      ))}
      {hiddenCount > 0 && (
        <InlineButton
          onClick={() => setExpanded(true)}
          text={`+${hiddenCount} ${i18n.common.expandableList.others}`}
        />
      )}
    </>
  )
}

function AclRowEditor({
  row,
  onSave,
  onCancel,
  unitGroups
}: {
  row: FormattedRow
  onSave: (groupIds: UUID[]) => void
  onCancel: () => void
  unitGroups: Record<UUID, DaycareGroupSummary>
}) {
  const { i18n } = useTranslation()
  const options = useMemo(
    () => sortBy(Object.values(unitGroups), ({ name }) => name),
    [unitGroups]
  )
  const [groups, setGroups] = useState<DaycareGroupSummary[]>(
    options.filter(({ id }) => row.groupIds.includes(id))
  )

  return (
    <Tr data-qa="acl-row">
      <Td data-qa="name">{row.name}</Td>
      <Td data-qa="email">{row.email}</Td>
      <GroupMultiSelectTd>
        <MultiSelect
          data-qa="groups"
          value={groups}
          options={options}
          getOptionId={(x) => x.id}
          getOptionLabel={(x) => x.name}
          onChange={(values) => setGroups(values)}
          placeholder={`${i18n.common.select}...`}
        />
      </GroupMultiSelectTd>
      <Td>
        <RowButtons>
          <InlineButton onClick={onCancel} text={i18n.common.cancel} />
          <InlineButton
            data-qa="save"
            onClick={() => onSave(groups.map(({ id }) => id))}
            text={i18n.common.save}
          />
        </RowButtons>
      </Td>
    </Tr>
  )
}

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
  onChangeGroups: (ids: UUID[]) => void
  rolesAllowedToEdit?: AdRole[]
  unitGroups: Record<UUID, DaycareGroupSummary> | undefined
}) {
  const isEditable = !!unitGroups
  const [editing, setEditing] = useState(false)

  const onClickEdit = useCallback(() => setEditing(true), [setEditing])
  const onCancelEditing = useCallback(() => setEditing(false), [setEditing])
  const onSave = useCallback(
    (groupIds) => {
      setEditing(false)
      onChangeGroups(groupIds)
    },
    [setEditing, onChangeGroups]
  )

  if (editing) {
    return (
      <AclRowEditor
        row={row}
        unitGroups={unitGroups ?? {}}
        onCancel={onCancelEditing}
        onSave={onSave}
      />
    )
  }

  const buttons = (
    <RowButtons>
      {isEditable && (
        <IconButton icon={faPen} onClick={onClickEdit} data-qa="edit" />
      )}
      {isDeletable && (
        <IconButton icon={faTrash} onClick={onClickDelete} data-qa="delete" />
      )}
    </RowButtons>
  )

  return (
    <Tr data-qa="acl-row">
      <Td data-qa="name">{row.name}</Td>
      <Td data-qa="email">{row.email}</Td>
      {unitGroups && (
        <Td data-qa="groups">
          <GroupListing unitGroups={unitGroups} groupIds={row.groupIds} />
        </Td>
      )}
      <Td>
        {rolesAllowedToEdit ? (
          <RequireRole oneOf={rolesAllowedToEdit}>{buttons}</RequireRole>
        ) : (
          buttons
        )}
      </Td>
    </Tr>
  )
}

const GroupMultiSelectTd = styled(Td)`
  padding: 0;
  vertical-align: middle;
`

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

const GroupsTh = styled(Td)`
  width: 40%;
`

function AclTable({
  unitGroups,
  rows,
  onDeleteAclRow,
  onChangeAclGroups,
  rolesAllowedToEdit
}: {
  unitGroups?: Record<UUID, DaycareGroupSummary>
  rows: FormattedRow[]
  onDeleteAclRow: (employeeId: UUID) => void
  onChangeAclGroups?: (employeeId: UUID, groupIds: UUID[]) => void
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
          {unitGroups && <GroupsTh>{i18n.unit.accessControl.groups}</GroupsTh>}
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
                : () => undefined
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

  const updateStaffGroupAcls = (employeeId: UUID, groupIds: UUID[]) => {
    void updateDaycareGroupAcl(unitId, employeeId, groupIds).then(() =>
      reloadDaycareAclRows(unitId)
    )
  }

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
