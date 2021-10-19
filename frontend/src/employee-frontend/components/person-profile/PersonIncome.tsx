// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { combine, Result } from 'lib-common/api'
import { IncomeStatement } from 'lib-common/api-types/incomeStatement'
import { formatDate } from 'lib-common/date'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import Pagination from 'lib-components/Pagination'
import { Dimmed, H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faEuroSign, faFileAlt } from 'lib-icons'
import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  createIncome,
  deleteIncome,
  getIncomes,
  IncomeTypeOptions,
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

interface Props {
  id: UUID
  open: boolean
}

const PersonIncome = React.memo(function PersonIncome({ id, open }: Props) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)
  const { incomes, setIncomes, reloadFamily } = useContext(PersonContext)

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

  const loadIncomeData = useRestApi(getIncomes, setIncomes)
  const loadIncomes = useCallback(
    (personId: UUID) => loadIncomeData(personId),
    [loadIncomeData]
  )

  // FIXME: This component shouldn't know about family's dependency on its data
  const reloadIncomes = useCallback(() => {
    loadIncomes(id)
    reloadFamily(id)
  }, [id, loadIncomes, reloadFamily])

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

  return (
    <CollapsibleSection
      icon={faEuroSign}
      title={i18n.personProfile.income.title}
      data-qa="person-income-collapsible"
      startCollapsed={!open}
    >
      <IncomeStatements personId={id} />
      <Gap />
      {renderResult(
        combine(incomes, incomeTypeOptions),
        ([incomes, incomeTypeOptions]) => (
          <Incomes
            personId={id}
            incomes={incomes}
            incomeTypeOptions={incomeTypeOptions}
            isIncomeRowOpen={isIncomeRowOpen}
            toggleIncomeRow={toggleIncomeRow}
            onSuccessfulUpdate={reloadIncomes}
          />
        )
      )}
    </CollapsibleSection>
  )
})

function IncomeStatements({ personId }: { personId: UUID }) {
  const { i18n } = useTranslation()
  const { incomeStatements, setIncomeStatements } = useContext(PersonContext)
  const [page, setPage] = useState(1)
  const loadData = useRestApi(getIncomeStatements, setIncomeStatements)

  useEffect(() => loadData(personId, page), [loadData, page, personId])

  return renderResult(incomeStatements, ({ data, pages }) => (
    <>
      <H3>{i18n.personProfile.incomeStatement.title}</H3>
      <IncomeStatementsTable personId={personId} incomeStatements={data} />
      <Pagination
        pages={pages}
        currentPage={page}
        setPage={setPage}
        label={i18n.common.page}
      />
    </>
  ))
}

function IncomeStatementsTable({
  personId,
  incomeStatements
}: {
  personId: UUID
  incomeStatements: IncomeStatement[]
}) {
  const i18n = useTranslation().i18n.personProfile.incomeStatement
  return incomeStatements.length === 0 ? (
    <div>{i18n.noIncomeStatements}</div>
  ) : (
    <Table>
      <Thead>
        <Tr>
          <Th>{i18n.incomeStatementHeading}</Th>
          <Th>{i18n.createdHeading}</Th>
          <Th>{i18n.handledHeading}</Th>
        </Tr>
      </Thead>
      <Tbody data-qa="income-statements">
        {incomeStatements.map((incomeStatement) => (
          <IncomeStatementRow
            key={incomeStatement.id}
            personId={personId}
            incomeStatement={incomeStatement}
          />
        ))}
      </Tbody>
    </Table>
  )
}

function IncomeStatementRow({
  personId,
  incomeStatement
}: {
  personId: UUID
  incomeStatement: IncomeStatement
}) {
  const { i18n } = useTranslation()

  return (
    <Tr key={incomeStatement.id} data-qa="income-statement-row">
      <Td verticalAlign="middle">
        <Link
          to={`/profile/${personId}/income-statement/${incomeStatement.id}`}
        >
          <FontAwesomeIcon icon={faFileAlt} />{' '}
          {incomeStatement.startDate.format()}
          {' - '}
          {incomeStatement.endDate?.format()}
        </Link>
      </Td>
      <Td verticalAlign="middle">{formatDate(incomeStatement.created)}</Td>
      <Td>
        <Checkbox
          data-qa="is-handled-checkbox"
          label={i18n.personProfile.incomeStatement.handled}
          hiddenLabel
          checked={incomeStatement.handled}
          disabled
        />
        {incomeStatement.handlerNote && (
          <>
            <Gap size={'xxs'} />
            <Dimmed>{incomeStatement.handlerNote}</Dimmed>
          </>
        )}
      </Td>
    </Tr>
  )
}

function Incomes({
  personId,
  incomes,
  incomeTypeOptions,
  isIncomeRowOpen,
  toggleIncomeRow,
  onSuccessfulUpdate
}: {
  personId: UUID
  incomes: Income[]
  incomeTypeOptions: IncomeTypeOptions
  isIncomeRowOpen: (id: IncomeId) => boolean
  toggleIncomeRow: (id: IncomeId) => void
  onSuccessfulUpdate: () => void
}) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)

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
      <H3>{i18n.personProfile.income.title}</H3>
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
          createIncome(personId, income).then(toggleCreated).then(handleErrors)
        }
        updateIncome={(incomeId: UUID, income: Income) =>
          updateIncome(incomeId, income).then(handleErrors)
        }
        deleteIncome={(incomeId: UUID) =>
          deleteIncome(incomeId).then(handleErrors)
        }
        onSuccessfulUpdate={() => {
          setEditing(undefined)
          onSuccessfulUpdate()
        }}
      />
    </>
  )
}

export default PersonIncome
