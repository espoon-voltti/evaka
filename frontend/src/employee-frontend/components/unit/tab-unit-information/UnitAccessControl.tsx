// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import sortBy from 'lodash/sortBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled from 'styled-components'

import { combine, isLoading } from 'lib-common/api'
import { AdRole } from 'lib-common/api-types/employee-auth'
import { Action } from 'lib-common/generated/action'
import { UserRole } from 'lib-common/generated/api-types/shared'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { ExpandableList } from 'lib-components/atoms/ExpandableList'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPen, faQuestion, faTrash } from 'lib-icons'

import { getEmployees } from '../../../api/employees'
import {
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
import { formatName } from '../../../utils'
import { renderResult } from '../../async-rendering'

import DaycareAclAdditionModal from './DaycareAclAdditionModal'
import EmployeeAclRowEditModal from './EmployeeAclRowEditModal'

type Props = {
  groups: Record<UUID, DaycareGroupSummary>
  permittedActions: Set<Action.Unit>
}

export type DaycareAclRole = Extract<
  UserRole,
  | 'UNIT_SUPERVISOR'
  | 'STAFF'
  | 'SPECIAL_EDUCATION_TEACHER'
  | 'EARLY_CHILDHOOD_EDUCATION_SECRETARY'
>

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

interface RemoveState {
  employeeId: UUID
  removeFn: (unitId: UUID, employeeId: UUID) => Promise<unknown>
}

export interface UpdateState {
  employeeRow: FormattedRow
}

interface AdditionState {
  role: DaycareAclRole
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

  const [removeState, setRemoveState] = useState<RemoveState | undefined>(
    undefined
  )

  const [updateState, setUpdateState] = useState<UpdateState | undefined>(
    undefined
  )

  const [additionState, setAdditionState] = useState<AdditionState | undefined>(
    undefined
  )

  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)

  // in-row editing modal generic fns
  const openEmployeeAclRowEditModal = useCallback(
    (employeeRow: FormattedRow) => {
      setUpdateState({ employeeRow })
      toggleUiMode(`edit-daycare-acl-${unitId}`)
    },
    [toggleUiMode, unitId]
  )

  const closeEmployeeAclRowEditModal = useCallback(() => {
    clearUiMode()
    setUpdateState(undefined)
  }, [clearUiMode])

  const confirmEmployeeAclRowEditModal = useCallback(() => {
    closeEmployeeAclRowEditModal()
    reloadDaycareAclRows()
  }, [closeEmployeeAclRowEditModal, reloadDaycareAclRows])

  // in-row removal modal generic fns
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

  // daycare addition modal role-based addition fns
  const openAddStaffModal = useCallback(() => {
    toggleUiMode(`add-daycare-acl-${unitId}`)
    setAdditionState({ role: 'STAFF' })
  }, [toggleUiMode, unitId])

  const openAddEcesModal = useCallback(() => {
    toggleUiMode(`add-daycare-acl-${unitId}`)
    setAdditionState({
      role: 'EARLY_CHILDHOOD_EDUCATION_SECRETARY'
    })
  }, [toggleUiMode, unitId])

  const openAddSpecialEducatioTeachedModal = useCallback(() => {
    toggleUiMode(`add-daycare-acl-${unitId}`)
    setAdditionState({ role: 'SPECIAL_EDUCATION_TEACHER' })
  }, [toggleUiMode, unitId])

  const openAddSupervisorModal = useCallback(() => {
    toggleUiMode(`add-daycare-acl-${unitId}`)
    setAdditionState({ role: 'UNIT_SUPERVISOR' })
  }, [toggleUiMode, unitId])

  // daycare addition modal generic fns
  const closeAddDaycareAclModal = useCallback(() => {
    clearUiMode()
  }, [clearUiMode])

  const confirmAddDaycareAclModal = useCallback(() => {
    closeAddDaycareAclModal()
    reloadDaycareAclRows()
  }, [closeAddDaycareAclModal, reloadDaycareAclRows])

  return (
    <div data-qa="daycare-acl" data-isloading={isLoading(daycareAclRows)}>
      {uiMode === `remove-daycare-acl-${unitId}` && (
        <DeleteConfirmationModal
          onClose={closeRemoveModal}
          onConfirm={confirmRemoveModal}
        />
      )}

      {uiMode === `edit-daycare-acl-${unitId}` && (
        <EmployeeAclRowEditModal
          onClose={closeEmployeeAclRowEditModal}
          onSuccess={confirmEmployeeAclRowEditModal}
          updatesGroupAcl={updateDaycareGroupAcl}
          employeeRow={updateState?.employeeRow}
          unitId={unitId}
          groups={groups}
        />
      )}

      {renderResult(candidateEmployees, (candidateEmployees) => (
        <>
          {uiMode === `add-daycare-acl-${unitId}` && (
            <DaycareAclAdditionModal
              onClose={closeAddDaycareAclModal}
              onSuccess={confirmAddDaycareAclModal}
              role={additionState?.role}
              employees={candidateEmployees}
              unitId={unitId}
              groups={groups}
              groupsPermitted={permittedActions.has('UPDATE_STAFF_GROUP_ACL')}
            />
          )}
        </>
      ))}
      <ContentArea opaque data-qa="daycare-acl-supervisors">
        <H2>{i18n.unit.accessControl.unitSupervisors}</H2>
        {renderResult(unitSupervisors, (unitSupervisors) => (
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
              onClickEdit={openEmployeeAclRowEditModal}
              editPermitted={permittedActions.has('UPDATE_STAFF_GROUP_ACL')}
              deletePermitted={permittedActions.has(
                'DELETE_ACL_UNIT_SUPERVISOR'
              )}
            />
            {permittedActions.has('INSERT_ACL_UNIT_SUPERVISOR') && (
              <>
                <Gap />
                <AddButton
                  text={i18n.unit.accessControl.addDaycareAclModal.title}
                  onClick={openAddSupervisorModal}
                  data-qa="open-add-supervisor-modal"
                />
              </>
            )}
          </>
        ))}
      </ContentArea>
      <ContentArea opaque data-qa="daycare-acl-set">
        <H2>{i18n.unit.accessControl.specialEducationTeachers}</H2>
        {renderResult(specialEducationTeachers, (specialEducationTeachers) => (
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
              onClickEdit={openEmployeeAclRowEditModal}
              editPermitted={permittedActions.has('UPDATE_STAFF_GROUP_ACL')}
              deletePermitted={permittedActions.has(
                'DELETE_ACL_SPECIAL_EDUCATION_TEACHER'
              )}
            />
            {permittedActions.has('INSERT_ACL_SPECIAL_EDUCATION_TEACHER') && (
              <>
                <Gap />
                <AddButton
                  text={i18n.unit.accessControl.addDaycareAclModal.title}
                  onClick={openAddSpecialEducatioTeachedModal}
                  data-qa="open-add-seteacher-modal"
                />
              </>
            )}
          </>
        ))}
      </ContentArea>
      <ContentArea opaque data-qa="daycare-acl-eces">
        <H2>{i18n.unit.accessControl.earlyChildhoodEducationSecretary}</H2>
        {renderResult(
          earlyChildhoodEducationSecretaries,
          (earlyChildhoodEducationSecretaries) => (
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
                onClickEdit={openEmployeeAclRowEditModal}
                editPermitted={permittedActions.has('UPDATE_STAFF_GROUP_ACL')}
                deletePermitted={permittedActions.has(
                  'DELETE_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY'
                )}
              />
              {permittedActions.has(
                'INSERT_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY'
              ) && (
                <>
                  <Gap />
                  <AddButton
                    text={i18n.unit.accessControl.addDaycareAclModal.title}
                    onClick={openAddEcesModal}
                    data-qa="open-add-eces-modal"
                  />
                </>
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
              onClickEdit={openEmployeeAclRowEditModal}
              editPermitted={permittedActions.has('UPDATE_STAFF_GROUP_ACL')}
              deletePermitted={permittedActions.has('DELETE_ACL_STAFF')}
            />
            {permittedActions.has('INSERT_ACL_STAFF') && (
              <>
                <Gap />
                <AddButton
                  text={i18n.unit.accessControl.addDaycareAclModal.title}
                  onClick={openAddStaffModal}
                  data-qa="open-add-staff-modal"
                />
              </>
            )}
          </>
        ))}
      </ContentArea>
    </div>
  )
})
