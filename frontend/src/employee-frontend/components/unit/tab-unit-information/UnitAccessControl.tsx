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
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { ExpandableList } from 'lib-components/atoms/ExpandableList'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { fontWeights, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPen, faQuestion, faTrash } from 'lib-icons'

import { getEmployees } from '../../../api/employees'
import {
  addDaycareAclEarlyChildhoodEducationSecretary,
  addDaycareAclSpecialEducationTeacher,
  addDaycareAclSupervisor,
  addStaffWithGroupAcl,
  DaycareAclRow,
  DaycareGroupSummary,
  removeDaycareAclEarlyChildhoodEducationSecretary,
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

import StaffAclAdditionModal from './StaffAclAdditionModal'
import StaffEditModal from './StaffEditModal'

type Props = {
  groups: Record<UUID, DaycareGroupSummary>
  permittedActions: Set<Action.Unit>
}

export interface FormattedRow {
  id: UUID
  name: string
  email: string
  groupIds: UUID[]
}

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

function AclRow({
  row,
  isEditable,
  isDeletable,
  onClickDelete,
  onClickEdit,
  unitGroups
}: {
  row: FormattedRow
  isEditable: boolean
  isDeletable: boolean
  onClickDelete: () => void
  onClickEdit: (employeeRow: FormattedRow) => void
  unitGroups: Record<UUID, DaycareGroupSummary> | undefined
}) {
  const { i18n } = useTranslation()

  const buttons = (
    <RowButtons>
      {isEditable && (
        <IconButton
          icon={faPen}
          onClick={() => onClickEdit(row)}
          data-qa="edit"
          aria-label={i18n.common.edit}
        />
      )}
      {isDeletable && (
        <IconButton
          icon={faTrash}
          onClick={onClickDelete}
          data-qa="delete"
          aria-label={i18n.common.remove}
        />
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
  onClickEdit,
  editPermitted,
  deletePermitted
}: {
  unitGroups?: Record<UUID, DaycareGroupSummary>
  rows: FormattedRow[]
  onDeleteAclRow: (employeeId: UUID) => void
  onClickEdit: (employeeRow: FormattedRow) => void
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
            onClickEdit={onClickEdit}
          />
        ))}
      </Tbody>
    </Table>
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
      <AddAclLabel>{i18n.unit.accessControl.addPersonModal.title}</AddAclLabel>
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

export interface UpdateState {
  employeeRow: FormattedRow
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

export default React.memo(function UnitAccessControl({
  groups,
  permittedActions
}: Props) {
  const { i18n } = useTranslation()

  const { unitId, daycareAclRows, reloadDaycareAclRows } =
    useContext(UnitContext)
  const { user } = useContext(UserContext)
  const [employees] = useApiState(getEmployees, [])

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
  const earlyChildhoodEducationSecretaries = useMemo(
    () =>
      daycareAclRows.map((daycareAclRows) =>
        formatRowsOfRole(
          daycareAclRows,
          'EARLY_CHILDHOOD_EDUCATION_SECRETARY',
          i18n
        )
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

  const addEarlyChildhoodEducationSecretary = useCallback(
    (employeeId: UUID) =>
      addDaycareAclEarlyChildhoodEducationSecretary(unitId, employeeId),
    [unitId]
  )

  const [removeState, setRemoveState] = useState<RemoveState | undefined>(
    undefined
  )

  const [updateState, setUpdateState] = useState<UpdateState | undefined>(
    undefined
  )

  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)

  const openStaffEditModal = useCallback(
    (employeeRow: FormattedRow) => {
      setUpdateState({ employeeRow })
      toggleUiMode(`edit-daycare-acl-${unitId}`)
    },
    [toggleUiMode, unitId]
  )

  const closeStaffEditModal = useCallback(() => {
    clearUiMode()
    setUpdateState(undefined)
  }, [clearUiMode])

  const confirmStaffEditModal = useCallback(() => {
    closeStaffEditModal()
    reloadDaycareAclRows()
  }, [closeStaffEditModal, reloadDaycareAclRows])

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

  const openAddStaffModal = useCallback(() => {
    toggleUiMode(`add-staff-daycare-acl-${unitId}`)
  }, [toggleUiMode, unitId])

  const closeAddStaffModal = useCallback(() => {
    clearUiMode()
  }, [clearUiMode])

  const confirmAddStaffModal = useCallback(() => {
    closeAddStaffModal()
    reloadDaycareAclRows()
  }, [closeAddStaffModal, reloadDaycareAclRows])

  return (
    <div data-qa="daycare-acl" data-isloading={isLoading(daycareAclRows)}>
      {uiMode === `remove-daycare-acl-${unitId}` && (
        <DeleteConfirmationModal
          onClose={closeRemoveModal}
          onConfirm={confirmRemoveModal}
        />
      )}

      {uiMode === `edit-daycare-acl-${unitId}` && (
        <StaffEditModal
          onClose={closeStaffEditModal}
          onSuccess={confirmAddStaffModal}
          updatesGroupAcl={updateDaycareGroupAcl}
          employeeRow={updateState?.employeeRow}
          unitId={unitId}
          groups={groups}
        />
      )}

      {renderResult(candidateEmployees, (candidateEmployees) => (
        <>
          {uiMode === `add-staff-daycare-acl-${unitId}` && (
            <StaffAclAdditionModal
              onClose={closeAddStaffModal}
              onSuccess={confirmStaffEditModal}
              addPersonAndGroupAcl={addStaffWithGroupAcl}
              employees={candidateEmployees}
              unitId={unitId}
              groups={groups}
            />
          )}
        </>
      ))}
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
                onClickEdit={openStaffEditModal}
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
                onClickEdit={openStaffEditModal}
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
      <ContentArea opaque data-qa="daycare-acl-eces">
        <H2>{i18n.unit.accessControl.earlyChildhoodEducationSecretary}</H2>
        {renderResult(
          combine(earlyChildhoodEducationSecretaries, candidateEmployees),
          ([earlyChildhoodEducationSecretaries, candidateEmployees]) => (
            <>
              <AclTable
                rows={earlyChildhoodEducationSecretaries}
                onDeleteAclRow={(employeeId) =>
                  openRemoveModal({
                    employeeId,
                    removeFn: removeDaycareAclEarlyChildhoodEducationSecretary
                  })
                }
                unitGroups={groups}
                onClickEdit={openStaffEditModal}
                editPermitted={permittedActions.has('UPDATE_STAFF_GROUP_ACL')}
                deletePermitted={permittedActions.has(
                  'DELETE_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY'
                )}
              />
              {permittedActions.has(
                'INSERT_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY'
              ) && (
                <AddAcl
                  employees={candidateEmployees}
                  onSave={addEarlyChildhoodEducationSecretary}
                  onSuccess={reloadDaycareAclRows}
                />
              )}
            </>
          )
        )}
      </ContentArea>
      <ContentArea opaque data-qa="daycare-acl-staff">
        <H2>{i18n.unit.accessControl.staff}</H2>
        {renderResult(staff, (staff) => (
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
              onClickEdit={openStaffEditModal}
              editPermitted={permittedActions.has('UPDATE_STAFF_GROUP_ACL')}
              deletePermitted={permittedActions.has('DELETE_ACL_STAFF')}
            />
            {permittedActions.has('INSERT_ACL_STAFF') && (
              <>
                <Gap />
                <AddButton
                  text={i18n.unit.accessControl.addPersonModal.title}
                  onClick={openAddStaffModal}
                  data-qa="open-staff-add-modal"
                />
              </>
            )}
          </>
        ))}
      </ContentArea>
    </div>
  )
})
