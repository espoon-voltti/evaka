// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import React, { useCallback, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'

import { combine, Failure, Result } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import { IncomeRequest } from 'lib-common/generated/api-types/invoicing'
import { IncomeId, PersonId } from 'lib-common/generated/api-types/shared'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import Pagination from 'lib-components/Pagination'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2, H3, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { renderResult } from '../async-rendering'

import IncomeStatementsTable from './IncomeStatementsTable'
import IncomeList from './income/IncomeList'
import { getMissingIncomePeriodsString } from './income/missingIncomePeriodUtils'
import {
  childPlacementPeriodsQuery,
  createIncomeMutation,
  deleteIncomeMutation,
  incomeCoefficientMultipliersQuery,
  incomeNotificationsQuery,
  incomeStatementChildrenQuery,
  incomeStatementsQuery,
  incomeTypeOptionsQuery,
  personIncomesQuery,
  updateIncomeMutation
} from './queries'
import { PersonContext } from './state'

interface Props {
  id: PersonId
  open: boolean
}

export default React.memo(function PersonIncome({
  id,
  open: startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext(PersonContext)
  const [open, setOpen] = useState(startOpen)

  const children = useQueryResult(
    incomeStatementChildrenQuery({ guardianId: id })
  )

  return (
    <CollapsibleContentArea
      title={<H2>{i18n.personProfile.income.title}</H2>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-qa="person-income-collapsible"
    >
      <H4>{i18n.personProfile.incomeStatement.title}</H4>
      <IncomeStatements personId={id} />
      <Gap size="L" />
      {renderResult(children, (children) => (
        <>
          <H4>{i18n.personProfile.incomeStatement.custodianTitle}</H4>
          {children.map((child) => (
            <ChildIncomeStatementsContainer key={child.id}>
              <Gap size="m" />
              <span data-qa="child-income-statement-title">{`${child.firstName} ${child.lastName}`}</span>
              <IncomeStatements personId={child.id} />
            </ChildIncomeStatementsContainer>
          ))}
        </>
      ))}
      <Gap size="L" />
      <H3>{i18n.personProfile.income.title}</H3>
      <Incomes personId={id} permittedActions={permittedActions} />
    </CollapsibleContentArea>
  )
})

export const IncomeStatements = React.memo(function IncomeStatements({
  personId
}: {
  personId: PersonId
}) {
  const { i18n } = useTranslation()
  const [page, setPage] = useState(1)
  const incomeStatements = useQueryResult(
    incomeStatementsQuery({ personId, page })
  )

  return (
    <>
      {renderResult(incomeStatements, ({ data, pages }) => (
        <>
          <IncomeStatementsTable personId={personId} incomeStatements={data} />
          <Pagination
            pages={pages}
            currentPage={page}
            setPage={setPage}
            label={i18n.common.page}
            hideIfOnlyOnePage={true}
          />
        </>
      ))}
    </>
  )
})

export const Incomes = React.memo(function Incomes({
  personId,
  permittedActions
}: {
  personId: PersonId
  permittedActions: Set<Action.Person> | Set<Action.Child | Action.Person>
}) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)
  const incomes = useQueryResult(personIncomesQuery({ personId }))
  const incomeNotifications = useQueryResult(
    incomeNotificationsQuery({ personId })
  )
  const childPlacementPeriods = useQueryResult(
    childPlacementPeriodsQuery({ adultId: personId })
  )

  const [openIncomeRows, setOpenIncomeRows] = useState<(IncomeId | 'new')[]>([])
  const toggleIncomeRow = useCallback((id: IncomeId | 'new') => {
    setOpenIncomeRows((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    )
  }, [])
  const isIncomeRowOpen = useCallback(
    (id: IncomeId | 'new') => openIncomeRows.includes(id),
    [openIncomeRows]
  )

  useEffect(() => {
    if (incomes.isSuccess && childPlacementPeriods.isSuccess) {
      const missingIncomePeriodsString = getMissingIncomePeriodsString(
        incomes.value.map((income) => income.data),
        childPlacementPeriods.value,
        i18n.common.and.toLowerCase()
      )
      if (missingIncomePeriodsString) {
        setErrorMessage({
          type: 'warning',
          title:
            i18n.personProfile.income.details.missingIncomeDaysWarningTitle,
          text: i18n.personProfile.income.details.missingIncomeDaysWarningText(
            missingIncomePeriodsString
          ),
          resolveLabel: i18n.common.ok
        })
      }
    }
  }, [i18n, incomes, childPlacementPeriods, setErrorMessage])

  const incomeTypeOptions = useQueryResult(incomeTypeOptionsQuery())
  const coefficientMultipliers = useQueryResult(
    incomeCoefficientMultipliersQuery()
  )

  const [editing, setEditing] = useState<string>()
  const [deleting, setDeleting] = useState<string>()

  const toggleCreated = (res: Result<IncomeId>): Result<IncomeId> => {
    if (res.isSuccess) {
      toggleIncomeRow(res.value)
    }
    return res
  }

  const handleErrors = (res: Failure<unknown>) => {
    const text =
      res.statusCode === 409
        ? i18n.personProfile.income.details.conflictErrorText
        : undefined

    setErrorMessage({
      type: 'error',
      title: i18n.personProfile.income.details.updateError,
      text,
      resolveLabel: i18n.common.ok
    })
  }

  const { mutateAsync: createIncome } = useMutationResult(createIncomeMutation)
  const { mutateAsync: updateIncome } = useMutationResult(updateIncomeMutation)
  const { mutateAsync: deleteIncome } = useMutationResult(deleteIncomeMutation)

  const queryClient = useQueryClient()
  const handleCancelEdit = useCallback(() => {
    setEditing(undefined)
    // Attachments may have been added or deleted, so we need to refetch the incomes
    void queryClient.invalidateQueries(personIncomesQuery({ personId }))
  }, [personId, queryClient])

  return (
    <>
      {renderResult(
        combine(
          incomes,
          incomeTypeOptions,
          incomeNotifications,
          coefficientMultipliers
        ),
        ([
          incomes,
          incomeTypeOptions,
          incomeNotifications,
          coefficientMultipliers
        ]) => (
          <>
            {permittedActions.has('CREATE_INCOME') && (
              <AddButtonRow
                text={i18n.personProfile.income.add}
                onClick={() => {
                  toggleIncomeRow('new')
                  setEditing('new')
                }}
                disabled={!!editing}
                data-qa="add-income-button"
              />
            )}
            <Gap size="m" />
            <IncomeList
              personId={personId}
              incomes={incomes}
              incomeTypeOptions={incomeTypeOptions}
              coefficientMultipliers={coefficientMultipliers}
              incomeNotifications={incomeNotifications}
              isRowOpen={isIncomeRowOpen}
              toggleRow={toggleIncomeRow}
              editing={editing}
              setEditing={setEditing}
              deleting={deleting}
              setDeleting={setDeleting}
              createIncome={(income: IncomeRequest) =>
                createIncome({ body: { ...income, personId } }).then(
                  toggleCreated
                )
              }
              updateIncome={(incomeId: IncomeId, income: IncomeRequest) =>
                updateIncome({ incomeId, body: income })
              }
              deleteIncome={(incomeId: IncomeId) =>
                deleteIncome({ personId, incomeId })
              }
              onSuccessfulUpdate={() => {
                setEditing(undefined)
              }}
              onFailedUpdate={handleErrors}
              onCancelEdit={handleCancelEdit}
            />
          </>
        )
      )}
    </>
  )
})

const ChildIncomeStatementsContainer = styled.div`
  border-style: solid;
  border-color: ${colors.grayscale.g35};
  border-width: 1px 0 0 0;
`
