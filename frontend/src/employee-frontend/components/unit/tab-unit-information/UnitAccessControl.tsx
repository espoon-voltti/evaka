// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import sortBy from 'lodash/sortBy'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import styled from 'styled-components'

import { combine, isLoading, Result } from 'lib-common/api'
import { AdRole } from 'lib-common/api-types/employee-auth'
import { Action } from 'lib-common/generated/action'
import { MobileDevice } from 'lib-common/generated/api-types/pairing'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { ExpandableList } from 'lib-components/atoms/ExpandableList'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import InputField from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { fontWeights, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faCheck, faPen, faQuestion, faTimes, faTrash } from 'lib-icons'

import { getEmployees } from '../../../api/employees'
import {
  addDaycareAclSpecialEducationTeacher,
  addDaycareAclStaff,
  addDaycareAclSupervisor,
  DaycareAclRow,
  DaycareGroupSummary,
  deleteMobileDevice,
  getMobileDevices,
  putMobileDeviceName,
  removeDaycareAclSpecialEducationTeacher,
  removeDaycareAclStaff,
  removeDaycareAclSupervisor,
  updateDaycareGroupAcl
} from '../../../api/unit'
import { Translations, useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { UnitContext } from '../../../state/unit'
import { UserContext } from '../../../state/user'
import { Employee } from '../../../types/employee'
import { formatName } from '../../../utils'
import { renderResult } from '../../async-rendering'

type Props = {
  groups: Record<UUID, DaycareGroupSummary>
  mobileEnabled: boolean
  permittedActions: Set<Action.Unit>
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
  const sortedIds = useMemo(
    () => sortBy(groupIds, (id) => unitGroups[id]?.name),
    [unitGroups, groupIds]
  )
  if (groupIds.length === 0) {
    return <>{i18n.unit.accessControl.noGroups}</>
  }
  return (
    <ExpandableList rowsToOccupy={3} i18n={i18n.common.expandableList}>
      {sortedIds.map((id) => (
        <div key={id}>{unitGroups[id]?.name}</div>
      ))}
    </ExpandableList>
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
    <Tr data-qa={`acl-row-${row.id}`}>
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
          <InlineButton
            icon={faTimes}
            onClick={onCancel}
            text={i18n.common.cancel}
          />
          <InlineButton
            icon={faCheck}
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
  isEditable,
  isDeletable,
  onClickDelete,
  onChangeGroups,
  unitGroups
}: {
  row: FormattedRow
  isEditable: boolean
  isDeletable: boolean
  onClickDelete: () => void
  onChangeGroups: (ids: UUID[]) => void
  unitGroups: Record<UUID, DaycareGroupSummary> | undefined
}) {
  const [editing, setEditing] = useState(false)

  const onClickEdit = useCallback(() => setEditing(true), [setEditing])
  const onCancelEditing = useCallback(() => setEditing(false), [setEditing])
  const onSave = useCallback(
    (groupIds: UUID[]) => {
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
    <Tr data-qa={`acl-row-${row.id}`}>
      <Td data-qa="name">{row.name}</Td>
      <Td data-qa="email">{row.email}</Td>
      {unitGroups && (
        <Td data-qa="groups">
          <GroupListing unitGroups={unitGroups} groupIds={row.groupIds} />
        </Td>
      )}
      <Td>{(isEditable || isDeletable) && buttons}</Td>
    </Tr>
  )
}

const GroupMultiSelectTd = styled(Td)`
  padding: 0;
  vertical-align: middle;
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

const GroupsTh = styled(Th)`
  width: 40%;
`

const ActionsTh = styled(Th)`
  width: 240px;
`

function AclTable({
  unitGroups,
  rows,
  onDeleteAclRow,
  onChangeAclGroups,
  editPermitted,
  deletePermitted
}: {
  unitGroups?: Record<UUID, DaycareGroupSummary>
  rows: FormattedRow[]
  onDeleteAclRow: (employeeId: UUID) => void
  onChangeAclGroups?: (employeeId: UUID, groupIds: UUID[]) => void
  editPermitted?: boolean
  deletePermitted: boolean
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
          <ActionsTh />
        </Tr>
      </Thead>
      <Tbody>
        {rows.map((row) => (
          <AclRow
            key={row.id}
            unitGroups={unitGroups}
            row={row}
            isDeletable={deletePermitted && row.id !== user?.id}
            isEditable={!!(editPermitted && unitGroups)}
            onClickDelete={() => onDeleteAclRow(row.id)}
            onChangeGroups={
              onChangeAclGroups
                ? (ids) => onChangeAclGroups(row.id, ids)
                : () => undefined
            }
          />
        ))}
      </Tbody>
    </Table>
  )
}

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
  return (
    <Tr data-qa="device-row">
      <Td data-qa="name">{row.name}</Td>
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

const AddAclLabel = styled.p`
  font-weight: ${fontWeights.semibold};
  margin-bottom: 0;
`

interface AclOption {
  label: string
  value: string
}

function getAclItemLabel(item: AclOption): string {
  return item.label
}

function getAclItemDataQa(item: AclOption): string {
  return `value-${item.value}`
}

const AddAcl = React.memo(function AddAcl({
  employees,
  onSave,
  onSuccess
}: {
  employees: Employee[]
  onSave: (employeeId: UUID) => Promise<Result<unknown>>
  onSuccess: () => void
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

  const options: AclOption[] = useMemo(
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

  const onClick = useCallback(
    () => (selectedEmployee ? onSave(selectedEmployee.value) : undefined),
    [onSave, selectedEmployee]
  )

  return (
    <>
      <AddAclLabel>{i18n.unit.accessControl.addPerson}</AddAclLabel>
      <AddAclSelectContainer>
        <Combobox
          data-qa="acl-combobox"
          placeholder={i18n.unit.accessControl.choosePerson}
          selectedItem={selectedEmployee}
          onChange={setSelectedEmployee}
          items={options}
          menuEmptyLabel={i18n.common.noResults}
          getItemLabel={getAclItemLabel}
          getItemDataQa={getAclItemDataQa}
        />
        <AsyncButton
          data-qa="acl-add-button"
          disabled={!selectedEmployee}
          onClick={onClick}
          onSuccess={onSuccess}
          text={i18n.common.add}
        />
      </AddAclSelectContainer>
    </>
  )
})

interface RemoveState {
  employeeId: UUID
  removeFn: (unitId: UUID, employeeId: UUID) => Promise<unknown>
}

function formatRowsOfRole(
  rows: DaycareAclRow[],
  role: AdRole,
  i18n: Translations
): FormattedRow[] {
  return orderBy(
    rows
      .filter((row) => row.role === role)
      .map((row) => ({
        id: row.employee.id,
        name: formatName(row.employee.firstName, row.employee.lastName, i18n),
        email: row.employee.email ?? row.employee.id,
        groupIds: row.groupIds
      })),
    (row) => row.name
  )
}

const DeleteConfirmationModal = React.memo(function DeleteConfirmationModal({
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
      title={i18n.unit.accessControl.removeConfirmation}
      icon={faQuestion}
      reject={{ action: onClose, label: i18n.common.cancel }}
      resolve={{ action: onConfirm, label: i18n.common.remove }}
    />
  )
})

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

export default React.memo(function UnitAccessControl({
  groups,
  mobileEnabled,
  permittedActions
}: Props) {
  const { i18n } = useTranslation()

  const { unitId, daycareAclRows, reloadDaycareAclRows } =
    useContext(UnitContext)
  const { user } = useContext(UserContext)
  const [employees] = useApiState(getEmployees, [])
  const [mobileDevices, reloadMobileDevices] = useApiState(
    () => getMobileDevices(unitId),
    [unitId]
  )
  const [mobileId, setMobileId] = useState<UUID | undefined>(undefined)

  const candidateEmployees = useMemo(
    () =>
      combine(employees, daycareAclRows).map(([employees, daycareAclRows]) =>
        orderBy(
          employees.filter(
            (employee) =>
              employee.id !== user?.id &&
              !daycareAclRows.some((row) => row.employee.id === employee.id)
          ),
          [(row) => row.email, (row) => row.id]
        )
      ),
    [employees, daycareAclRows, user]
  )

  const unitSupervisors = useMemo(
    () =>
      daycareAclRows.map((daycareAclRows) =>
        formatRowsOfRole(daycareAclRows, 'UNIT_SUPERVISOR', i18n)
      ),
    [daycareAclRows, i18n]
  )
  const specialEducationTeachers = useMemo(
    () =>
      daycareAclRows.map((daycareAclRows) =>
        formatRowsOfRole(daycareAclRows, 'SPECIAL_EDUCATION_TEACHER', i18n)
      ),
    [daycareAclRows, i18n]
  )
  const staff = useMemo(
    () =>
      daycareAclRows.map((daycareAclRows) =>
        formatRowsOfRole(daycareAclRows, 'STAFF', i18n)
      ),
    [daycareAclRows, i18n]
  )

  const addUnitSupervisor = useCallback(
    (employeeId: UUID) => addDaycareAclSupervisor(unitId, employeeId),
    [unitId]
  )

  const addSpecialEducationTeacher = useCallback(
    (employeeId: UUID) =>
      addDaycareAclSpecialEducationTeacher(unitId, employeeId),
    [unitId]
  )

  const addStaff = useCallback(
    (employeeId: UUID) => addDaycareAclStaff(unitId, employeeId),
    [unitId]
  )

  const updateStaffGroupAcls = useCallback(
    (employeeId: UUID, groupIds: UUID[]) => {
      void updateDaycareGroupAcl(unitId, employeeId, groupIds).then(() =>
        reloadDaycareAclRows()
      )
    },
    [reloadDaycareAclRows, unitId]
  )

  const [removeState, setRemoveState] = useState<RemoveState | undefined>(
    undefined
  )
  const { uiMode, toggleUiMode, clearUiMode, startPairing } =
    useContext(UIContext)

  const openRemoveModal = useCallback(
    (removeState: RemoveState) => {
      setRemoveState(removeState)
      toggleUiMode(`remove-daycare-acl-${unitId}`)
    },
    [toggleUiMode, unitId]
  )
  const closeRemoveModal = useCallback(() => {
    clearUiMode()
    setRemoveState(undefined)
  }, [clearUiMode])
  const confirmRemoveModal = useCallback(async () => {
    await removeState?.removeFn(unitId, removeState.employeeId)
    closeRemoveModal()
    reloadDaycareAclRows()
  }, [closeRemoveModal, reloadDaycareAclRows, removeState, unitId])
  const confirmRemoveDevice = useCallback(async () => {
    if (mobileId) {
      await deleteMobileDevice(mobileId)
      void reloadMobileDevices()
    }
    closeRemoveModal()
  }, [closeRemoveModal, mobileId, reloadMobileDevices])

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
        await putMobileDeviceName(mobileId, name)
        void reloadMobileDevices()
        clearUiMode()
      }
    },
    [clearUiMode, mobileId, reloadMobileDevices]
  )

  return (
    <div
      data-qa="daycare-acl"
      data-isloading={isLoading(combine(daycareAclRows, mobileDevices))}
    >
      {uiMode === `remove-daycare-acl-${unitId}` && (
        <DeleteConfirmationModal
          onClose={closeRemoveModal}
          onConfirm={confirmRemoveModal}
        />
      )}
      {uiMode === `edit-daycare-mobile-device-${unitId}` && (
        <EditMobileDeviceModal
          onClose={closeRemoveModal}
          onConfirm={saveMobileDevice}
        />
      )}
      {uiMode === `remove-daycare-mobile-device-${unitId}` && (
        <DeleteMobileDeviceConfirmationModal
          onClose={closeRemoveModal}
          onConfirm={confirmRemoveDevice}
        />
      )}
      <ContentArea opaque data-qa="daycare-acl-supervisors">
        <H2>{i18n.unit.accessControl.unitSupervisors}</H2>
        {renderResult(
          combine(unitSupervisors, candidateEmployees),
          ([unitSupervisors, candidateEmployees]) => (
            <>
              <AclTable
                rows={unitSupervisors}
                onDeleteAclRow={(employeeId) =>
                  openRemoveModal({
                    employeeId,
                    removeFn: removeDaycareAclSupervisor
                  })
                }
                unitGroups={groups}
                onChangeAclGroups={updateStaffGroupAcls}
                editPermitted={permittedActions.has('UPDATE_STAFF_GROUP_ACL')}
                deletePermitted={permittedActions.has(
                  'DELETE_ACL_UNIT_SUPERVISOR'
                )}
              />
              {permittedActions.has('INSERT_ACL_UNIT_SUPERVISOR') && (
                <AddAcl
                  employees={candidateEmployees}
                  onSave={addUnitSupervisor}
                  onSuccess={reloadDaycareAclRows}
                />
              )}
            </>
          )
        )}
      </ContentArea>
      <ContentArea opaque data-qa="daycare-acl-set">
        <H2>{i18n.unit.accessControl.specialEducationTeachers}</H2>
        {renderResult(
          combine(specialEducationTeachers, candidateEmployees),
          ([specialEducationTeachers, candidateEmployees]) => (
            <>
              <AclTable
                rows={specialEducationTeachers}
                onDeleteAclRow={(employeeId) =>
                  openRemoveModal({
                    employeeId,
                    removeFn: removeDaycareAclSpecialEducationTeacher
                  })
                }
                unitGroups={groups}
                onChangeAclGroups={updateStaffGroupAcls}
                editPermitted={permittedActions.has('UPDATE_STAFF_GROUP_ACL')}
                deletePermitted={permittedActions.has(
                  'DELETE_ACL_SPECIAL_EDUCATION_TEACHER'
                )}
              />
              {permittedActions.has('INSERT_ACL_SPECIAL_EDUCATION_TEACHER') && (
                <AddAcl
                  employees={candidateEmployees}
                  onSave={addSpecialEducationTeacher}
                  onSuccess={reloadDaycareAclRows}
                />
              )}
            </>
          )
        )}
      </ContentArea>
      <ContentArea opaque data-qa="daycare-acl-staff">
        <H2>{i18n.unit.accessControl.staff}</H2>
        {renderResult(
          combine(staff, candidateEmployees),
          ([staff, candidateEmployees]) => (
            <>
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
                editPermitted={permittedActions.has('UPDATE_STAFF_GROUP_ACL')}
                deletePermitted={permittedActions.has('DELETE_ACL_STAFF')}
              />
              {permittedActions.has('INSERT_ACL_STAFF') && (
                <AddAcl
                  employees={candidateEmployees}
                  onSave={addStaff}
                  onSuccess={reloadDaycareAclRows}
                />
              )}
            </>
          )
        )}
      </ContentArea>
      {mobileEnabled && (
        <ContentArea opaque data-qa="daycare-mobile-devices">
          <H2>{i18n.unit.accessControl.mobileDevices.mobileDevices}</H2>
          {renderResult(mobileDevices, (mobileDevices) => (
            <>
              <DevicesTable
                rows={mobileDevices}
                onEditDevice={openEditMobileDeviceModal}
                onDeleteDevice={openRemoveMobileDeviceModal}
              />
              <Gap />
              <AddButton
                text={i18n.unit.accessControl.mobileDevices.addMobileDevice}
                onClick={openPairMobileDeviceModal}
                data-qa="start-mobile-pairing"
              />
            </>
          ))}
        </ContentArea>
      )}
    </div>
  )
})
