// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import sortBy from 'lodash/sortBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled, { useTheme } from 'styled-components'

import { combine, isLoading, Success, wrapResult } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import { DaycareGroupResponse } from 'lib-common/generated/api-types/daycare'
import { DaycareAclRow, UserRole } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { StaticChip } from 'lib-components/atoms/Chip'
import { ExpandableList } from 'lib-components/atoms/ExpandableList'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { IconButton } from 'lib-components/atoms/buttons/IconButton'
import { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faPen, faQuestion, faTrash } from 'lib-icons'

import {
  deleteEarlyChildhoodEducationSecretary,
  deleteSpecialEducationTeacher,
  deleteStaff,
  deleteTemporaryEmployee,
  deleteTemporaryEmployeeAcl,
  deleteUnitSupervisor,
  getTemporaryEmployees,
  updateGroupAclWithOccupancyCoefficient
} from '../../../generated/api-clients/daycare'
import { getEmployeesQuery } from '../../../queries'
import { Translations, useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { UnitContext } from '../../../state/unit'
import { UserContext } from '../../../state/user'
import { formatName } from '../../../utils'
import { renderResult } from '../../async-rendering'

import DaycareAclAdditionModal from './DaycareAclAdditionModal'
import EmployeeAclRowEditModal from './EmployeeAclRowEditModal'

const getTemporaryEmployeesResult = wrapResult(getTemporaryEmployees)
const updateGroupAclWithOccupancyCoefficientResult = wrapResult(
  updateGroupAclWithOccupancyCoefficient
)
const deleteEarlyChildhoodEducationSecretaryResult = wrapResult(
  deleteEarlyChildhoodEducationSecretary
)
const deleteSpecialEducationTeacherResult = wrapResult(
  deleteSpecialEducationTeacher
)
const deleteStaffResult = wrapResult(deleteStaff)
const deleteUnitSupervisorResult = wrapResult(deleteUnitSupervisor)
const deleteTemporaryEmployeeAclResult = wrapResult(deleteTemporaryEmployeeAcl)
const deleteTemporaryEmployeeResult = wrapResult(deleteTemporaryEmployee)

type Props = {
  groups: Record<UUID, DaycareGroupResponse>
  permittedActions: Action.Unit[]
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
  firstName: string
  lastName: string
  name: string
  email: string
  groupIds: UUID[]
  hasStaffOccupancyEffect: boolean
  temporary: boolean
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
  unitGroups: Record<UUID, DaycareGroupResponse>
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
  hideTemporaryChip,
  isEditable,
  isDeletable,
  coefficientPermitted,
  onClickDelete,
  onClickEdit,
  unitGroups
}: {
  row: FormattedRow
  hideTemporaryChip: boolean
  isEditable: boolean
  isDeletable: boolean
  coefficientPermitted: boolean
  onClickDelete: () => void
  onClickEdit: (employeeRow: FormattedRow) => void
  unitGroups: Record<UUID, DaycareGroupResponse> | undefined
}) {
  const { i18n } = useTranslation()
  const theme = useTheme()
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
      <Td data-qa="name">
        <span data-qa="text">{row.name}</span>
        {!hideTemporaryChip && row.temporary && (
          <>
            {' '}
            <StaticChip color={colors.accents.a8lightBlue} data-qa="icon">
              TS
            </StaticChip>
          </>
        )}
      </Td>
      <Td data-qa="email">{row.email}</Td>
      {unitGroups && (
        <Td data-qa="groups">
          <GroupListing unitGroups={unitGroups} groupIds={row.groupIds} />
        </Td>
      )}
      {coefficientPermitted && (
        <Td
          data-qa={
            row.hasStaffOccupancyEffect ? 'coefficient-on' : 'coefficient-off'
          }
        >
          {row.hasStaffOccupancyEffect && (
            <Tooltip
              tooltip={i18n.unit.attendanceReservations.affectsOccupancy}
              position="bottom"
              width="large"
            >
              <RoundIcon
                content="K"
                active={true}
                color={theme.colors.accents.a3emerald}
                size="s"
              />
            </Tooltip>
          )}
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
  dataQa = 'acl-table',
  unitGroups,
  rows,
  hideTemporaryChip,
  onDeleteAclRow,
  onClickEdit,
  editPermitted,
  deletePermitted,
  coefficientPermitted
}: {
  dataQa?: string
  unitGroups?: Record<UUID, DaycareGroupResponse>
  rows: FormattedRow[]
  hideTemporaryChip?: boolean
  onDeleteAclRow: (employee: FormattedRow) => void
  onClickEdit: (employeeRow: FormattedRow) => void
  editPermitted?: boolean
  deletePermitted: boolean
  coefficientPermitted: boolean
}) {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)

  return (
    <Table data-qa={dataQa}>
      <Thead>
        <Tr>
          <Th>{i18n.common.form.name}</Th>
          <Th>{i18n.unit.accessControl.email}</Th>
          {unitGroups && <GroupsTh>{i18n.unit.accessControl.groups}</GroupsTh>}
          {coefficientPermitted && (
            <Th>{i18n.unit.accessControl.hasOccupancyCoefficient}</Th>
          )}
          <ActionsTh />
        </Tr>
      </Thead>
      <Tbody>
        {rows.map((row) => (
          <AclRow
            key={row.id}
            unitGroups={unitGroups}
            row={row}
            hideTemporaryChip={hideTemporaryChip ?? false}
            isDeletable={deletePermitted && row.id !== user?.id}
            isEditable={!!(editPermitted && unitGroups)}
            coefficientPermitted={coefficientPermitted}
            onClickDelete={() => onDeleteAclRow(row)}
            onClickEdit={onClickEdit}
          />
        ))}
      </Tbody>
    </Table>
  )
}

interface RemoveState {
  employee: FormattedRow
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
  role: UserRole,
  i18n: Translations
): FormattedRow[] {
  return orderBy(
    rows
      .filter((row) => row.role === role)
      .map((row) => ({
        id: row.employee.id,
        hasStaffOccupancyEffect: row.employee.hasStaffOccupancyEffect ?? false,
        firstName: row.employee.firstName,
        lastName: row.employee.lastName,
        name: formatName(row.employee.firstName, row.employee.lastName, i18n),
        email: row.employee.email ?? '',
        groupIds: row.groupIds,
        temporary: row.employee.temporary
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
      data-qa="remove-daycare-acl-modal"
      type="warning"
      title={i18n.unit.accessControl.removeConfirmation}
      icon={faQuestion}
      reject={{ action: onClose, label: i18n.common.cancel }}
      resolve={{ action: onConfirm, label: i18n.common.remove }}
    />
  )
})

const DeleteTemporaryEmployeeConfirmationModal = React.memo(
  function DeleteTemporaryEmployeeConfirmationModal({
    onClose,
    onConfirm
  }: {
    onClose: () => void
    onConfirm: () => void
  }) {
    const { i18n } = useTranslation()
    return (
      <InfoModal
        data-qa="remove-temporary-employee-modal"
        type="warning"
        title={i18n.unit.accessControl.removeTemporaryEmployeeConfirmation}
        icon={faQuestion}
        reject={{ action: onClose, label: i18n.common.cancel }}
        resolve={{ action: onConfirm, label: i18n.common.remove }}
      />
    )
  }
)

export default React.memo(function UnitAccessControl({
  groups,
  permittedActions
}: Props) {
  const { i18n } = useTranslation()

  const { unitId, daycareAclRows, reloadDaycareAclRows } =
    useContext(UnitContext)
  const { user } = useContext(UserContext)
  const employees = useQueryResult(getEmployeesQuery())
  const [temporaryEmployees, reloadTemporaryEmployees] = useApiState(
    () =>
      permittedActions.includes('READ_TEMPORARY_EMPLOYEE')
        ? getTemporaryEmployeesResult({ unitId })
        : Promise.resolve(Success.of([])),
    [permittedActions, unitId]
  )

  const candidateEmployees = useMemo(
    () =>
      combine(employees, daycareAclRows).map(([employees, daycareAclRows]) =>
        orderBy(
          employees.filter(
            (employee) =>
              employee.id !== user?.id &&
              employee.externalId !== null &&
              employee.temporaryInUnitId === null &&
              !daycareAclRows.some((row) => row.employee.id === employee.id)
          ),
          [(row) => row.email, (row) => row.id]
        )
      ),
    [employees, daycareAclRows, user]
  )
  const candidateTemporaryEmployees = useMemo(
    () =>
      combine(temporaryEmployees, daycareAclRows).map(
        ([temporaryEmployees, daycareAclRows]) =>
          orderBy(
            temporaryEmployees.filter(
              (employee) =>
                employee.id !== user?.id &&
                !daycareAclRows.some((row) => row.employee.id === employee.id)
            ),
            [(row) => row.email, (row) => row.id]
          )
      ),
    [temporaryEmployees, daycareAclRows, user]
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
    await removeState?.removeFn(unitId, removeState.employee.id)
    closeRemoveModal()
    reloadDaycareAclRows()
    await reloadTemporaryEmployees()
  }, [
    closeRemoveModal,
    reloadDaycareAclRows,
    reloadTemporaryEmployees,
    removeState,
    unitId
  ])

  const openRemoveTemporaryEmployeeModal = useCallback(
    (removeState: RemoveState) => {
      setRemoveState(removeState)
      toggleUiMode(`remove-temporary-employee-${unitId}`)
    },
    [toggleUiMode, unitId]
  )

  const closeRemoveTemporaryEmployeeModal = useCallback(() => {
    clearUiMode()
    setRemoveState(undefined)
  }, [clearUiMode])

  const confirmRemoveTemporaryEmployeeModal = useCallback(async () => {
    await removeState?.removeFn(unitId, removeState.employee.id)
    closeRemoveTemporaryEmployeeModal()
    await reloadTemporaryEmployees()
  }, [
    closeRemoveTemporaryEmployeeModal,
    reloadTemporaryEmployees,
    removeState,
    unitId
  ])

  // daycare addition modal role-based addition fns
  const openAddStaffModal = useCallback(() => {
    setAdditionState({ role: 'STAFF' })
    toggleUiMode(`add-daycare-acl-${unitId}`)
  }, [toggleUiMode, unitId])

  const openAddEcesModal = useCallback(() => {
    setAdditionState({
      role: 'EARLY_CHILDHOOD_EDUCATION_SECRETARY'
    })
    toggleUiMode(`add-daycare-acl-${unitId}`)
  }, [toggleUiMode, unitId])

  const openAddSpecialEducatioTeachedModal = useCallback(() => {
    setAdditionState({ role: 'SPECIAL_EDUCATION_TEACHER' })
    toggleUiMode(`add-daycare-acl-${unitId}`)
  }, [toggleUiMode, unitId])

  const openAddSupervisorModal = useCallback(() => {
    setAdditionState({ role: 'UNIT_SUPERVISOR' })
    toggleUiMode(`add-daycare-acl-${unitId}`)
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

      {uiMode === `remove-temporary-employee-${unitId}` && (
        <DeleteTemporaryEmployeeConfirmationModal
          onClose={closeRemoveTemporaryEmployeeModal}
          onConfirm={confirmRemoveTemporaryEmployeeModal}
        />
      )}

      {uiMode === `edit-daycare-acl-${unitId}` && updateState && (
        <EmployeeAclRowEditModal
          onClose={closeEmployeeAclRowEditModal}
          onSuccess={confirmEmployeeAclRowEditModal}
          updatesGroupAcl={updateGroupAclWithOccupancyCoefficientResult}
          permittedActions={permittedActions}
          employeeRow={updateState.employeeRow}
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
              permittedActions={permittedActions}
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
              onDeleteAclRow={(employee) =>
                openRemoveModal({
                  employee,
                  removeFn: async (daycareId, employeeId) =>
                    deleteUnitSupervisorResult({ daycareId, employeeId })
                })
              }
              unitGroups={groups}
              onClickEdit={openEmployeeAclRowEditModal}
              editPermitted={
                permittedActions.includes('UPDATE_STAFF_GROUP_ACL') ||
                permittedActions.includes('UPSERT_STAFF_OCCUPANCY_COEFFICIENTS')
              }
              deletePermitted={permittedActions.includes(
                'DELETE_ACL_UNIT_SUPERVISOR'
              )}
              coefficientPermitted={permittedActions.includes(
                'READ_STAFF_OCCUPANCY_COEFFICIENTS'
              )}
            />
            {permittedActions.includes('INSERT_ACL_UNIT_SUPERVISOR') && (
              <>
                <Gap />
                <AddButton
                  text={i18n.unit.accessControl.addDaycareAclModal.title}
                  onClick={openAddSupervisorModal}
                  data-qa="open-add-daycare-acl-modal"
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
              onDeleteAclRow={(employee) =>
                openRemoveModal({
                  employee,
                  removeFn: async (daycareId, employeeId) =>
                    deleteSpecialEducationTeacherResult({
                      daycareId,
                      employeeId
                    })
                })
              }
              unitGroups={groups}
              onClickEdit={openEmployeeAclRowEditModal}
              editPermitted={
                permittedActions.includes('UPDATE_STAFF_GROUP_ACL') ||
                permittedActions.includes('UPSERT_STAFF_OCCUPANCY_COEFFICIENTS')
              }
              deletePermitted={permittedActions.includes(
                'DELETE_ACL_SPECIAL_EDUCATION_TEACHER'
              )}
              coefficientPermitted={permittedActions.includes(
                'READ_STAFF_OCCUPANCY_COEFFICIENTS'
              )}
            />
            {permittedActions.includes(
              'INSERT_ACL_SPECIAL_EDUCATION_TEACHER'
            ) && (
              <>
                <Gap />
                <AddButton
                  text={i18n.unit.accessControl.addDaycareAclModal.title}
                  onClick={openAddSpecialEducatioTeachedModal}
                  data-qa="open-add-daycare-acl-modal"
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
                onDeleteAclRow={(employee) =>
                  openRemoveModal({
                    employee,
                    removeFn: async (daycareId, employeeId) =>
                      deleteEarlyChildhoodEducationSecretaryResult({
                        daycareId,
                        employeeId
                      })
                  })
                }
                unitGroups={groups}
                onClickEdit={openEmployeeAclRowEditModal}
                editPermitted={
                  permittedActions.includes('UPDATE_STAFF_GROUP_ACL') ||
                  permittedActions.includes(
                    'UPSERT_STAFF_OCCUPANCY_COEFFICIENTS'
                  )
                }
                deletePermitted={permittedActions.includes(
                  'DELETE_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY'
                )}
                coefficientPermitted={permittedActions.includes(
                  'READ_STAFF_OCCUPANCY_COEFFICIENTS'
                )}
              />
              {permittedActions.includes(
                'INSERT_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY'
              ) && (
                <>
                  <Gap />
                  <AddButton
                    text={i18n.unit.accessControl.addDaycareAclModal.title}
                    onClick={openAddEcesModal}
                    data-qa="open-add-daycare-acl-modal"
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
          <AclTable
            rows={staff}
            onDeleteAclRow={(employee) =>
              openRemoveModal({
                employee,
                removeFn: async (daycareId, employeeId) =>
                  employee.temporary
                    ? deleteTemporaryEmployeeAclResult({
                        unitId: daycareId,
                        employeeId
                      })
                    : deleteStaffResult({ daycareId, employeeId })
              })
            }
            unitGroups={groups}
            onClickEdit={openEmployeeAclRowEditModal}
            editPermitted={
              permittedActions.includes('UPDATE_STAFF_GROUP_ACL') ||
              permittedActions.includes('UPSERT_STAFF_OCCUPANCY_COEFFICIENTS')
            }
            deletePermitted={permittedActions.includes('DELETE_ACL_STAFF')}
            coefficientPermitted={permittedActions.includes(
              'READ_STAFF_OCCUPANCY_COEFFICIENTS'
            )}
          />
        ))}
        {(permittedActions.includes('INSERT_ACL_STAFF') ||
          permittedActions.includes('CREATE_TEMPORARY_EMPLOYEE')) && (
          <>
            <Gap />
            <AddButton
              text={i18n.unit.accessControl.addDaycareAclModal.title}
              onClick={openAddStaffModal}
              data-qa="open-add-daycare-acl-modal"
            />
          </>
        )}
        {permittedActions.includes('READ_TEMPORARY_EMPLOYEE')
          ? renderResult(candidateTemporaryEmployees, (employees) =>
              employees.length > 0 ? (
                <>
                  <HorizontalLine />
                  <H4>{i18n.unit.accessControl.previousTemporaryEmployees}</H4>
                  <AclTable
                    dataQa="previous-temporary-employee-table"
                    rows={employees.map((employee) => ({
                      id: employee.id,
                      firstName: employee.firstName ?? '',
                      lastName: employee.lastName ?? '',
                      name: formatName(
                        employee.firstName,
                        employee.lastName,
                        i18n
                      ),
                      email: employee.email ?? '',
                      groupIds: [],
                      hasStaffOccupancyEffect: false,
                      temporary: true
                    }))}
                    hideTemporaryChip={true}
                    onDeleteAclRow={(employee) =>
                      openRemoveTemporaryEmployeeModal({
                        employee,
                        removeFn: async (unitId, employeeId) =>
                          deleteTemporaryEmployeeResult({ unitId, employeeId })
                      })
                    }
                    unitGroups={groups}
                    onClickEdit={openEmployeeAclRowEditModal}
                    editPermitted={permittedActions.includes(
                      'UPDATE_TEMPORARY_EMPLOYEE'
                    )}
                    deletePermitted={permittedActions.includes(
                      'DELETE_TEMPORARY_EMPLOYEE'
                    )}
                    coefficientPermitted={true}
                  />
                </>
              ) : null
            )
          : null}
      </ContentArea>
    </div>
  )
})
