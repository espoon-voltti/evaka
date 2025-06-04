// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import sortBy from 'lodash/sortBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled, { useTheme } from 'styled-components'

import { combine, isLoading } from 'lib-common/api'
import type { Action } from 'lib-common/generated/action'
import type {
  DaycareGroupResponse,
  DaycareResponse
} from 'lib-common/generated/api-types/daycare'
import type { Employee } from 'lib-common/generated/api-types/pis'
import type {
  DaycareAclRow,
  DaycareId,
  EmployeeId,
  ScheduledDaycareAclRow,
  UserRole
} from 'lib-common/generated/api-types/shared'
import type { MutationDescription } from 'lib-common/query'
import { constantQuery, useQueryResult } from 'lib-common/query'
import type { UUID } from 'lib-common/types'
import { ExpandableList } from 'lib-components/atoms/ExpandableList'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { PersonName } from 'lib-components/molecules/PersonNames'
import { H2, H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPen, faTrash } from 'lib-icons'
import { faUndo } from 'lib-icons'

import { getEmployeesQuery } from '../../../queries'
import { useTranslation } from '../../../state/i18n'
import { UserContext } from '../../../state/user'
import { renderResult } from '../../async-rendering'
import {
  deleteEarlyChildhoodEducationSecretaryMutation,
  deleteScheduledAclMutation,
  deleteSpecialEducationTeacherMutation,
  deleteStaffMutation,
  deleteTemporaryEmployeeAclMutation,
  deleteTemporaryEmployeeMutation,
  deleteUnitSupervisorMutation,
  reactivateTemporaryEmployeeMutation,
  temporaryEmployeesQuery,
  unitAclQuery,
  unitScheduledAclQuery
} from '../queries'

import AddAclModal from './acl-modals/AddAclModal'
import AddTemporaryEmployeeModal from './acl-modals/AddTemporaryEmployeeModal'
import EditAclModal from './acl-modals/EditAclModal'
import EditTemporaryEmployeeModal from './acl-modals/EditTemporaryEmployeeModal'

export type DaycareAclRole = Extract<
  UserRole,
  | 'UNIT_SUPERVISOR'
  | 'STAFF'
  | 'SPECIAL_EDUCATION_TEACHER'
  | 'EARLY_CHILDHOOD_EDUCATION_SECRETARY'
>

const roleOrder = (role: UserRole) => {
  switch (role) {
    case 'UNIT_SUPERVISOR':
      return 0
    case 'SPECIAL_EDUCATION_TEACHER':
      return 1
    case 'EARLY_CHILDHOOD_EDUCATION_SECRETARY':
      return 2
    case 'STAFF':
      return 3
    default:
      return 999 // not expected
  }
}

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
  unitId,
  row,
  scheduledRow,
  isEditable,
  isDeletable,
  coefficientPermitted,
  onClickEdit,
  unitGroups
}: {
  unitId: DaycareId
  row: DaycareAclRow
  scheduledRow: ScheduledDaycareAclRow | undefined
  isEditable: boolean
  isDeletable: boolean
  coefficientPermitted: boolean
  onClickEdit: () => void
  unitGroups: Record<UUID, DaycareGroupResponse> | undefined
}) {
  const { i18n } = useTranslation()
  const theme = useTheme()
  const deleteMutation: MutationDescription<
    { unitId: DaycareId; employeeId: EmployeeId },
    void
  > | null =
    row.role === 'UNIT_SUPERVISOR'
      ? deleteUnitSupervisorMutation
      : row.role === 'SPECIAL_EDUCATION_TEACHER'
        ? deleteSpecialEducationTeacherMutation
        : row.role === 'EARLY_CHILDHOOD_EDUCATION_SECRETARY'
          ? deleteEarlyChildhoodEducationSecretaryMutation
          : row.role === 'STAFF' && !row.employee.temporary
            ? deleteStaffMutation
            : null

  const roleChangeDate =
    scheduledRow &&
    (row.endDate === null || scheduledRow.startDate <= row.endDate.addDays(1))
      ? scheduledRow.startDate.subDays(1)
      : null

  return (
    <Tr data-qa={`acl-row-${row.employee.id}`}>
      <Td>
        <FixedSpaceRow>
          <span data-qa="role">{i18n.roles.adRoles[row.role]}</span>
          {coefficientPermitted && (
            <span
              data-qa={
                row.employee.hasStaffOccupancyEffect
                  ? 'coefficient-on'
                  : 'coefficient-off'
              }
            >
              {row.employee.hasStaffOccupancyEffect && (
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
            </span>
          )}
        </FixedSpaceRow>
      </Td>
      <Td>
        <FixedSpaceColumn spacing="zero">
          <span data-qa="name">
            <PersonName person={row.employee} format="First Last" />
          </span>
          <EmailSpan data-qa="email">{row.employee.email}</EmailSpan>
        </FixedSpaceColumn>
      </Td>
      {unitGroups && (
        <Td data-qa="groups">
          <GroupListing unitGroups={unitGroups} groupIds={row.groupIds} />
        </Td>
      )}
      <Td>
        {roleChangeDate ? `${roleChangeDate.format()}*` : row.endDate?.format()}
      </Td>
      <Td>
        <FixedSpaceRow justifyContent="flex-end">
          {isEditable && (
            <IconOnlyButton
              icon={faPen}
              onClick={onClickEdit}
              data-qa="edit"
              aria-label={i18n.common.edit}
            />
          )}
          {isDeletable && deleteMutation && (
            <ConfirmedMutation
              buttonStyle="ICON"
              icon={faTrash}
              buttonAltText={i18n.common.remove}
              confirmationTitle={i18n.unit.accessControl.removeConfirmation}
              mutation={deleteMutation}
              onClick={() => ({ unitId, employeeId: row.employee.id })}
              data-qa="delete"
              data-qa-modal="confirm-delete"
            />
          )}
        </FixedSpaceRow>
      </Td>
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
  unitId,
  dataQa = 'acl-table',
  unitGroups,
  rows,
  scheduledRows,
  onClickEdit,
  permittedActions
}: {
  unitId: DaycareId
  dataQa?: string
  unitGroups?: Record<UUID, DaycareGroupResponse>
  rows: DaycareAclRow[]
  scheduledRows: ScheduledDaycareAclRow[]
  onClickEdit: (employeeRow: DaycareAclRow) => void
  permittedActions: Action.Unit[]
}) {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)

  const orderedRows = useMemo(
    () =>
      orderBy(
        rows.filter((row) => !row.employee.temporary),
        [
          (row) => roleOrder(row.role),
          (row) => row.employee.firstName,
          (row) => row.employee.lastName
        ]
      ),
    [rows]
  )

  const editPermitted = useMemo(
    () =>
      permittedActions.includes('UPDATE_STAFF_GROUP_ACL') ||
      permittedActions.includes('UPSERT_STAFF_OCCUPANCY_COEFFICIENTS'),
    [permittedActions]
  )
  const deletePermitted = useCallback(
    (role: UserRole) => {
      switch (role) {
        case 'UNIT_SUPERVISOR':
          return permittedActions.includes('DELETE_ACL_UNIT_SUPERVISOR')
        case 'SPECIAL_EDUCATION_TEACHER':
          return permittedActions.includes(
            'DELETE_ACL_SPECIAL_EDUCATION_TEACHER'
          )
        case 'EARLY_CHILDHOOD_EDUCATION_SECRETARY':
          return permittedActions.includes(
            'DELETE_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY'
          )
        case 'STAFF':
          return permittedActions.includes('DELETE_ACL_STAFF')
        default:
          return false
      }
    },
    [permittedActions]
  )
  const coefficientPermitted = useMemo(
    () => permittedActions.includes('READ_STAFF_OCCUPANCY_COEFFICIENTS'),
    [permittedActions]
  )

  return (
    <Table data-qa={dataQa}>
      <Thead>
        <Tr>
          <Th>{i18n.unit.accessControl.role}</Th>
          <Th>{i18n.common.form.name}</Th>
          {unitGroups && <GroupsTh>{i18n.unit.accessControl.groups}</GroupsTh>}
          <Th>{i18n.unit.accessControl.aclEndDate}</Th>
          <ActionsTh />
        </Tr>
      </Thead>
      <Tbody>
        {orderedRows.map((row) => (
          <AclRow
            key={row.employee.id}
            unitId={unitId}
            unitGroups={unitGroups}
            row={row}
            scheduledRow={scheduledRows.find((sr) => sr.id === row.employee.id)}
            isDeletable={
              deletePermitted(row.role) && row.employee.id !== user?.id
            }
            isEditable={!!(editPermitted && unitGroups)}
            coefficientPermitted={coefficientPermitted}
            onClickEdit={() => onClickEdit(row)}
          />
        ))}
      </Tbody>
    </Table>
  )
}

function ScheduledAclTable({
  unitId,
  rows,
  currentRows,
  permittedActions
}: {
  unitId: DaycareId
  rows: ScheduledDaycareAclRow[]
  currentRows: DaycareAclRow[]
  permittedActions: Action.Unit[]
}) {
  const { i18n } = useTranslation()

  const orderedRows = useMemo(
    () =>
      orderBy(rows, [
        (row) => roleOrder(row.role),
        (row) => row.firstName,
        (row) => row.lastName
      ]),
    [rows]
  )

  return (
    <Table data-qa="scheduled-acl-table">
      <Thead>
        <Tr>
          <Th>{i18n.unit.accessControl.role}</Th>
          <Th>{i18n.common.form.name}</Th>
          <Th>{i18n.unit.accessControl.aclStartDate}</Th>
          <Th>{i18n.unit.accessControl.aclEndDate}</Th>
          <Th />
        </Tr>
      </Thead>
      <Tbody>
        {orderedRows.map((row) => {
          const isRoleChange = currentRows.some(
            (r) =>
              r.employee.id === row.id &&
              (r.endDate === null || r.endDate >= row.startDate.subDays(1))
          )
          return (
            <Tr key={row.id} data-qa={`scheduled-acl-row-${row.id}`}>
              <Td>
                <span data-qa="role">{i18n.roles.adRoles[row.role]}</span>
              </Td>
              <Td>
                <FixedSpaceColumn spacing="zero">
                  <span data-qa="name">
                    <PersonName person={row} format="First Last" />
                  </span>
                  <EmailSpan data-qa="email">{row.email}</EmailSpan>
                </FixedSpaceColumn>
              </Td>
              <Td>
                {row.startDate.format()}
                {isRoleChange && '*'}
              </Td>
              <Td>{row.endDate?.format()}</Td>
              <Td>
                {permittedActions.includes('DELETE_ACL_SCHEDULED') && (
                  <ConfirmedMutation
                    buttonStyle="ICON"
                    icon={faTrash}
                    buttonAltText={i18n.common.remove}
                    confirmationTitle={
                      i18n.unit.accessControl.removeScheduledConfirmation
                    }
                    mutation={deleteScheduledAclMutation}
                    onClick={() => ({ unitId, employeeId: row.id })}
                  />
                )}
              </Td>
            </Tr>
          )
        })}
      </Tbody>
    </Table>
  )
}

function TemporaryEmployeesTable({
  unitId,
  unitGroups,
  rows,
  onClickEdit,
  editPermitted,
  deletePermitted
}: {
  unitId: DaycareId
  unitGroups: Record<UUID, DaycareGroupResponse>
  rows: DaycareAclRow[]
  onClickEdit: (employeeRow: DaycareAclRow) => void
  editPermitted: boolean
  deletePermitted: boolean
}) {
  const { i18n } = useTranslation()
  const theme = useTheme()

  const orderedRows = useMemo(
    () =>
      orderBy(
        rows.filter((row) => row.employee.temporary),
        [(row) => row.employee.firstName, (row) => row.employee.lastName]
      ),
    [rows]
  )

  return (
    <Table data-qa="temporary-employee-table">
      <Thead>
        <Tr>
          <Th>{i18n.common.form.name}</Th>
          <GroupsTh>{i18n.unit.accessControl.groups}</GroupsTh>
          <ActionsTh />
        </Tr>
      </Thead>
      <Tbody>
        {orderedRows.map((row) => (
          <Tr
            key={row.employee.id}
            data-qa={`temporary-employee-row-${row.employee.id}`}
          >
            <Td>
              <FixedSpaceRow>
                <span data-qa="name">
                  <PersonName person={row.employee} format="First Last" />
                </span>
                <span
                  data-qa={
                    row.employee.hasStaffOccupancyEffect
                      ? 'coefficient-on'
                      : 'coefficient-off'
                  }
                >
                  {row.employee.hasStaffOccupancyEffect && (
                    <Tooltip
                      tooltip={
                        i18n.unit.attendanceReservations.affectsOccupancy
                      }
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
                </span>
              </FixedSpaceRow>
            </Td>
            {unitGroups && (
              <Td data-qa="groups">
                <GroupListing unitGroups={unitGroups} groupIds={row.groupIds} />
              </Td>
            )}
            <Td>
              <FixedSpaceRow justifyContent="flex-end">
                {editPermitted && (
                  <IconOnlyButton
                    icon={faPen}
                    onClick={() => onClickEdit(row)}
                    data-qa="edit"
                    aria-label={i18n.common.edit}
                  />
                )}
                {deletePermitted && (
                  <ConfirmedMutation
                    buttonStyle="ICON"
                    icon={faTrash}
                    buttonAltText={i18n.common.remove}
                    confirmationTitle={
                      i18n.unit.accessControl.removeConfirmation
                    }
                    mutation={deleteTemporaryEmployeeAclMutation}
                    onClick={() => ({ unitId, employeeId: row.employee.id })}
                    data-qa="delete"
                    data-qa-modal="confirm-delete"
                  />
                )}
              </FixedSpaceRow>
            </Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
  )
}

function PreviousTemporaryEmployeesTable({
  unitId,
  rows,
  editPermitted,
  deletePermitted
}: {
  unitId: DaycareId
  rows: Employee[]
  editPermitted: boolean
  deletePermitted: boolean
}) {
  const { i18n } = useTranslation()

  const orderedRows = useMemo(
    () => orderBy(rows, [(row) => row.firstName, (row) => row.lastName]),
    [rows]
  )

  return (
    <Table data-qa="previous-temporary-employee-table">
      <Thead>
        <Tr>
          <Th>{i18n.common.form.name}</Th>
          <ActionsTh />
        </Tr>
      </Thead>
      <Tbody>
        {orderedRows.map((row) => (
          <Tr
            key={row.id}
            data-qa={`previous-temporary-employee-row-${row.id}`}
          >
            <Td data-qa="name">
              <PersonName person={row} format="First Last" />
            </Td>
            <StyledTd $width="400px">
              <FixedSpaceRow justifyContent="flex-end">
                {editPermitted && (
                  <MutateButton
                    appearance="inline"
                    icon={faUndo}
                    text={i18n.unit.accessControl.reactivateTemporaryEmployee}
                    mutation={reactivateTemporaryEmployeeMutation}
                    onClick={() => ({ unitId, employeeId: row.id })}
                    data-qa="reactivate"
                  />
                )}
                {deletePermitted && (
                  <ConfirmedMutation
                    buttonStyle="INLINE"
                    icon={faTrash}
                    buttonText={i18n.common.remove}
                    confirmationTitle={
                      i18n.unit.accessControl
                        .removeTemporaryEmployeeConfirmation
                    }
                    mutation={deleteTemporaryEmployeeMutation}
                    onClick={() => ({ unitId, employeeId: row.id })}
                    data-qa="delete"
                    data-qa-modal="confirm-delete"
                  />
                )}
              </FixedSpaceRow>
            </StyledTd>
          </Tr>
        ))}
      </Tbody>
    </Table>
  )
}

export default React.memo(function UnitAccessControl({
  unitInformation
}: {
  unitInformation: DaycareResponse
}) {
  const { i18n } = useTranslation()
  const {
    daycare: { id: unitId },
    permittedActions
  } = unitInformation
  const { user } = useContext(UserContext)

  const groups = useMemo(
    () =>
      Object.fromEntries(
        unitInformation.groups.map((group) => [group.id, group] as const)
      ),
    [unitInformation]
  )

  const employees = useQueryResult(getEmployeesQuery())
  const daycareAclRows = useQueryResult(unitAclQuery({ unitId }))
  const scheduledDaycareAclRows = useQueryResult(
    unitScheduledAclQuery({ unitId })
  )
  const temporaryEmployees = useQueryResult(
    permittedActions.includes('READ_TEMPORARY_EMPLOYEE')
      ? temporaryEmployeesQuery({ unitId })
      : constantQuery([])
  )

  const candidateEmployees = useMemo(
    () =>
      combine(employees, daycareAclRows).map(([employees, daycareAclRows]) =>
        orderBy(
          employees.filter(
            (employee) =>
              employee.id !== user?.id &&
              (employee.externalId !== null || employee.hasSsn) &&
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

  const [addAclModalOpen, setAddAclModalOpen] = useState<boolean>(false)
  const [editedAclRow, setEditedAclRow] = useState<DaycareAclRow | null>(null)

  const [addTemporaryEmployeeModalOpen, setAddTemporaryEmployeeModalOpen] =
    useState<boolean>(false)

  const canInsertAcl = useMemo(
    () =>
      unitInformation.permittedActions.includes('INSERT_ACL_UNIT_SUPERVISOR') ||
      unitInformation.permittedActions.includes(
        'INSERT_ACL_SPECIAL_EDUCATION_TEACHER'
      ) ||
      unitInformation.permittedActions.includes(
        'INSERT_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY'
      ) ||
      unitInformation.permittedActions.includes('INSERT_ACL_STAFF'),
    [unitInformation.permittedActions]
  )

  return (
    <div data-qa="unit-employees" data-isloading={isLoading(daycareAclRows)}>
      {editedAclRow &&
        (editedAclRow.employee.temporary ? (
          <EditTemporaryEmployeeModal
            onClose={() => setEditedAclRow(null)}
            row={editedAclRow}
            unitId={unitId}
            groups={groups}
          />
        ) : (
          <EditAclModal
            onClose={() => setEditedAclRow(null)}
            permittedActions={permittedActions}
            row={editedAclRow}
            unitId={unitId}
            groups={groups}
          />
        ))}

      {renderResult(
        combine(candidateEmployees, scheduledDaycareAclRows),
        ([candidateEmployees, scheduledDaycareAclRows]) => (
          <>
            {addAclModalOpen && (
              <AddAclModal
                onClose={() => setAddAclModalOpen(false)}
                employees={candidateEmployees}
                unitId={unitId}
                groups={groups}
                permittedActions={permittedActions}
                scheduledAclRows={scheduledDaycareAclRows}
              />
            )}
          </>
        )
      )}

      {addTemporaryEmployeeModalOpen && (
        <AddTemporaryEmployeeModal
          onClose={() => setAddTemporaryEmployeeModalOpen(false)}
          unitId={unitId}
          groups={groups}
          permittedActions={permittedActions}
        />
      )}

      <ContentArea opaque>
        <H2>{i18n.unit.accessControl.aclRoles}</H2>

        <FixedSpaceRow justifyContent="space-between" alignItems="center">
          <H3 noMargin>{i18n.unit.accessControl.activeAclRoles}</H3>
          {canInsertAcl && (
            <AddButton
              text={i18n.unit.accessControl.addDaycareAclModal.title}
              onClick={() => setAddAclModalOpen(true)}
              data-qa="open-add-daycare-acl-modal"
            />
          )}
        </FixedSpaceRow>

        {renderResult(
          combine(daycareAclRows, scheduledDaycareAclRows),
          ([daycareAclRows, scheduledDaycareAclRows]) => (
            <>
              <AclTable
                unitId={unitId}
                rows={daycareAclRows}
                scheduledRows={scheduledDaycareAclRows}
                unitGroups={groups}
                onClickEdit={(row) => setEditedAclRow(row)}
                permittedActions={permittedActions}
              />
            </>
          )
        )}
        <Gap size="xs" />
        <span>*{i18n.unit.accessControl.roleChange}</span>

        <Gap size="XL" />

        <H3 noMargin>{i18n.unit.accessControl.scheduledAclRoles}</H3>
        {renderResult(
          combine(daycareAclRows, scheduledDaycareAclRows),
          ([daycareAclRows, scheduledDaycareAclRows]) => (
            <ScheduledAclTable
              unitId={unitId}
              rows={scheduledDaycareAclRows}
              currentRows={daycareAclRows}
              permittedActions={permittedActions}
            />
          )
        )}

        <Gap size="XL" />

        <FixedSpaceRow justifyContent="space-between" alignItems="center">
          <H3 noMargin>{i18n.unit.accessControl.temporaryEmployees.title}</H3>
          {permittedActions.includes('CREATE_TEMPORARY_EMPLOYEE') && (
            <AddButton
              text={i18n.unit.accessControl.addTemporaryEmployeeModal.title}
              onClick={() => setAddTemporaryEmployeeModalOpen(true)}
              data-qa="open-add-temporary-employee-modal"
            />
          )}
        </FixedSpaceRow>

        {renderResult(daycareAclRows, (daycareAclRows) => (
          <TemporaryEmployeesTable
            unitId={unitId}
            rows={daycareAclRows}
            unitGroups={groups}
            onClickEdit={(row) => setEditedAclRow(row)}
            editPermitted={
              permittedActions.includes('UPDATE_TEMPORARY_EMPLOYEE') &&
              permittedActions.includes(
                'UPSERT_STAFF_OCCUPANCY_COEFFICIENTS'
              ) &&
              permittedActions.includes('UPDATE_STAFF_GROUP_ACL')
            }
            deletePermitted={permittedActions.includes(
              'UPDATE_TEMPORARY_EMPLOYEE'
            )}
          />
        ))}

        {permittedActions.includes('READ_TEMPORARY_EMPLOYEE') &&
          renderResult(candidateTemporaryEmployees, (employees) =>
            employees.length > 0 ? (
              <>
                <Gap size="XL" />
                <H3 noMargin>
                  {
                    i18n.unit.accessControl.temporaryEmployees
                      .previousEmployeesTitle
                  }
                </H3>
                <PreviousTemporaryEmployeesTable
                  unitId={unitId}
                  rows={employees}
                  editPermitted={permittedActions.includes(
                    'UPDATE_TEMPORARY_EMPLOYEE'
                  )}
                  deletePermitted={permittedActions.includes(
                    'DELETE_TEMPORARY_EMPLOYEE'
                  )}
                />
              </>
            ) : null
          )}
      </ContentArea>
    </div>
  )
})

const EmailSpan = styled.span`
  font-size: 14px;
  color: ${(p) => p.theme.colors.grayscale.g70};
  font-weight: 600;
`

const StyledTd = styled(Td)<{ $width: string }>`
  width: ${(p) => p.$width};
`
