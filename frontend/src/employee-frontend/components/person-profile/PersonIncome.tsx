// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'

import { combine, Failure, Result, wrapResult } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'
import { IncomeRequest } from 'lib-common/generated/api-types/invoicing'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import Pagination from 'lib-components/Pagination'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2, H3, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import {
  getIncomeStatementChildren,
  getIncomeStatements
} from '../../generated/api-clients/incomestatement'
import {
  createIncome,
  deleteIncome,
  getIncomeNotifications,
  getPersonIncomes,
  updateIncome
} from '../../generated/api-clients/invoicing'
import { getChildPlacementPeriods } from '../../generated/api-clients/placement'
import { useTranslation } from '../../state/i18n'
import { PersonContext } from '../../state/person'
import { UIContext } from '../../state/ui'
import { IncomeId } from '../../types/income'
import { useIncomeTypeOptions } from '../../utils/income'
import { renderResult } from '../async-rendering'

import IncomeStatementsTable from './IncomeStatementsTable'
import IncomeList from './income/IncomeList'
import { getMissingIncomePeriodsString } from './income/missingIncomePeriodUtils'
import { incomeCoefficientMultipliersQuery } from './queries'

const getChildPlacementPeriodsResult = wrapResult(getChildPlacementPeriods)
const createIncomeResult = wrapResult(createIncome)
const getPersonIncomesResult = wrapResult(getPersonIncomes)
const updateIncomeResult = wrapResult(updateIncome)
const deleteIncomeResult = wrapResult(deleteIncome)
const getIncomeNotificationsResult = wrapResult(getIncomeNotifications)
const getIncomeStatementsResult = wrapResult(getIncomeStatements)
const getIncomeStatementChildrenResult = wrapResult(getIncomeStatementChildren)

interface Props {
  id: UUID
  open: boolean
}

export default React.memo(function PersonIncome({
  id,
  open: startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext(PersonContext)
  const [open, setOpen] = useState(startOpen)

  const [children] = useApiState(
    () => getIncomeStatementChildrenResult({ guardianId: id }),
    [id]
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
  personId: UUID
}) {
  const { i18n } = useTranslation()
  const [page, setPage] = useState(1)
  const [incomeStatements] = useApiState(
    () => getIncomeStatementsResult({ personId, page, pageSize: 10 }),
    [personId, page]
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
  personId: UUID
  permittedActions: Set<Action.Person> | Set<Action.Child | Action.Person>
}) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)
  const { reloadFamily } = useContext(PersonContext)
  const [incomes, loadIncomes] = useApiState(
    () => getPersonIncomesResult({ personId }),
    [personId]
  )
  const [incomeNotifications] = useApiState(
    () => getIncomeNotificationsResult({ personId }),
    [personId]
  )
  const [childPlacementPeriods] = useApiState(
    () => getChildPlacementPeriodsResult({ adultId: personId }),
    [personId]
  )

  const [openIncomeRows, setOpenIncomeRows] = useState<IncomeId[]>([])
  const toggleIncomeRow = useCallback((id: IncomeId) => {
    setOpenIncomeRows((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    )
  }, [])
  const isIncomeRowOpen = useCallback(
    (id: IncomeId) => openIncomeRows.includes(id),
    [openIncomeRows]
  )

  // FIXME: This component shouldn't know about family's dependency on its data
  const reloadIncomes = useCallback(() => {
    void loadIncomes()
    reloadFamily()
  }, [loadIncomes, reloadFamily])
  useEffect(reloadIncomes, [reloadIncomes])

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

  const incomeTypeOptions = useIncomeTypeOptions()
  const coefficientMultipliers = useQueryResult(
    incomeCoefficientMultipliersQuery()
  )

  const [editing, setEditing] = useState<string>()
  const [deleting, setDeleting] = useState<string>()

  const toggleCreated = (res: Result<string>): Result<string> => {
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
                createIncomeResult({ body: { ...income, personId } }).then(
                  toggleCreated
                )
              }
              updateIncome={(incomeId: UUID, income: IncomeRequest) =>
                updateIncomeResult({ incomeId, body: income })
              }
              deleteIncome={(incomeId: UUID) =>
                deleteIncomeResult({ incomeId })
              }
              onSuccessfulUpdate={() => {
                setEditing(undefined)
                reloadIncomes()
              }}
              onFailedUpdate={handleErrors}
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
