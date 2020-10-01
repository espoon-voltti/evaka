// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useMemo, useState } from 'react'
import { orderBy } from 'lodash'
import { ContentArea, Loader, Table, Title } from '~components/shared/alpha'
import InfoModal from '~components/common/InfoModal'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import {
  addDaycareAclStaff,
  addDaycareAclSupervisor,
  DaycareAclRow,
  getDaycareAclRows,
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
import { faPlusCircle, faQuestion, faTrash } from '@evaka/icons'
import { H2 } from '~components/shared/Typography'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import styled from 'styled-components'
import ReactSelect, { components } from 'react-select'
import Button from '~components/shared/atoms/buttons/Button'
import { UUID } from '~types'
import { UIContext } from '~state/ui'
import { formatName } from '~utils'

type Props = { unitId: string }

interface FormattedRow {
  id: UUID
  name: string
  email: string
}

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
    <Table.Row dataQa="acl-row">
      <Table.Td dataQa="name">{row.name}</Table.Td>
      <Table.Td dataQa="email">{row.email}</Table.Td>
      <Table.Td>
        {isDeletable && (
          <IconButton icon={faTrash} onClick={onClickDelete} dataQa="delete" />
        )}
      </Table.Td>
    </Table.Row>
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
    <Table.Table dataQa="acl-table">
      <Table.Head>
        <Table.Row>
          <Table.Th>{i18n.common.form.name}</Table.Th>
          <Table.Th>{i18n.unit.accessControl.email}</Table.Th>
          <Table.Th />
        </Table.Row>
      </Table.Head>
      <Table.Body>
        {rows.map((row) => (
          <AclRow
            key={row.id}
            row={row}
            isDeletable={row.id !== user?.id}
            onClickDelete={() => onDeleteAclRow(row.id)}
          />
        ))}
      </Table.Body>
    </Table.Table>
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
  const loading = isLoading(daycareAclRows) || isLoading(employees)
  const failed = isFailure(daycareAclRows) || isFailure(employees)

  const reloadEmployees = useRestApi(getEmployees, setEmployees)
  const reloadDaycareAclRows = useRestApi(getDaycareAclRows, setDaycareAclRows)
  useEffect(() => reloadEmployees(), [])
  useEffect(() => reloadDaycareAclRows(unitId), [unitId])

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

  return (
    <>
      {uiMode === `remove-daycare-acl-${unitId}` && <DeleteConfirmationModal />}
      <RequireRole oneOf={['ADMIN']}>
        <ContentArea opaque dataQa="daycare-acl-supervisors">
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
        <ContentArea opaque dataQa="daycare-acl-staff">
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
    </>
  )
}

export default UnitAccessControl
