// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { combine, Result } from 'lib-common/api'
import { useApiState } from 'lib-common/utils/useRestApi'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import Pagination from 'lib-components/Pagination'
import { H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faEuroSign } from 'lib-icons'
import React, { useCallback, useContext, useEffect, useState } from 'react'
import {
  createIncome,
  deleteIncome,
  getIncomes,
  updateIncome
} from '../../api/income'
import { getIncomeStatements } from '../../api/income-statement'
import { useTranslation } from '../../state/i18n'
import { PersonContext } from '../../state/person'
import { UIContext } from '../../state/ui'
import { Income, IncomeBody, IncomeId } from '../../types/income'
import { useIncomeTypeOptions } from '../../utils/income'
import { renderResult } from '../async-rendering'
import IncomeList from './income/IncomeList'
import { getMissingIncomePeriodsString } from './income/missingIncomePeriodUtils'
import { UUID } from 'lib-common/types'
import IncomeStatementsTable from './IncomeStatementsTable'

interface Props {
  id: UUID
  open: boolean
}

export default React.memo(function PersonIncome({ id, open }: Props) {
  const { i18n } = useTranslation()
  return (
    <CollapsibleSection
      icon={faEuroSign}
      title={i18n.personProfile.income.title}
      data-qa="person-income-collapsible"
      startCollapsed={!open}
    >
      <IncomeStatements personId={id} />
      <Gap />
      <H3>{i18n.personProfile.income.title}</H3>
      <Incomes personId={id} />
    </CollapsibleSection>
  )
})

const IncomeStatements = React.memo(function IncomeStatements({
  personId
}: {
  personId: UUID
}) {
  const { i18n } = useTranslation()
  const [page, setPage] = useState(1)
  const [incomeStatements] = useApiState(
    () => getIncomeStatements(personId, page),
    [personId, page]
  )

  return (
    <>
      <H3>{i18n.personProfile.incomeStatement.title}</H3>
      {renderResult(incomeStatements, ({ data, pages }) => (
        <>
          <IncomeStatementsTable personId={personId} incomeStatements={data} />
          <Pagination
            pages={pages}
            currentPage={page}
            setPage={setPage}
            label={i18n.common.page}
          />
        </>
      ))}
    </>
  )
})

export const Incomes = React.memo(function Incomes({
  personId
}: {
  personId: UUID
}) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)
  const { reloadFamily } = useContext(PersonContext)
  const [incomes, loadIncomes] = useApiState(
    () => getIncomes(personId),
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
    loadIncomes()
    reloadFamily()
  }, [loadIncomes, reloadFamily])
  useEffect(reloadIncomes, [reloadIncomes])

  useEffect(() => {
    if (incomes.isSuccess) {
      const missingIncomePeriodsString = getMissingIncomePeriodsString(
        incomes.value,
        i18n.common.and.toLowerCase()
      )
      if (missingIncomePeriodsString.length) {
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
  }, [i18n, incomes, setErrorMessage])

  const incomeTypeOptions = useIncomeTypeOptions()

  const [editing, setEditing] = useState<string>()
  const [deleting, setDeleting] = useState<string>()

  const toggleCreated = (res: Result<string>): Result<string> => {
    if (res.isSuccess) {
      toggleIncomeRow(res.value)
    }
    return res
  }

  const handleErrors = (res: Result<unknown>) => {
    if (res.isFailure) {
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

      throw res.message
    }
  }

  return (
    <>
      {renderResult(
        combine(incomes, incomeTypeOptions),
        ([incomes, incomeTypeOptions]) => (
          <>
            <AddButtonRow
              text={i18n.personProfile.income.add}
              onClick={() => {
                toggleIncomeRow('new')
                setEditing('new')
              }}
              disabled={!!editing}
              data-qa="add-income-button"
            />
            <Gap size="m" />
            <IncomeList
              incomes={incomes}
              incomeTypeOptions={incomeTypeOptions}
              isRowOpen={isIncomeRowOpen}
              toggleRow={toggleIncomeRow}
              editing={editing}
              setEditing={setEditing}
              deleting={deleting}
              setDeleting={setDeleting}
              createIncome={(income: IncomeBody) =>
                createIncome(personId, income)
                  .then(toggleCreated)
                  .then(handleErrors)
              }
              updateIncome={(incomeId: UUID, income: Income) =>
                updateIncome(incomeId, income).then(handleErrors)
              }
              deleteIncome={(incomeId: UUID) =>
                deleteIncome(incomeId).then(handleErrors)
              }
              onSuccessfulUpdate={() => {
                setEditing(undefined)
                reloadIncomes()
              }}
            />
          </>
        )
      )}
    </>
  )
})
