// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { faEuroSign, faFileAlt } from 'lib-icons'
import { Gap } from 'lib-components/white-space'
import Loader from 'lib-components/atoms/Loader'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import IncomeList from './income/IncomeList'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { PersonContext } from '../../state/person'
import { combine, Loading, Result } from 'lib-common/api'
import {
  createIncome,
  deleteIncome,
  getIncomes,
  updateIncome
} from '../../api/income'
import { Income, IncomeId, PartialIncome } from '../../types/income'
import { UUID } from '../../types'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { getMissingIncomePeriodsString } from './income/missingIncomePeriodUtils'
import {
  getIncomeStatements,
  setIncomeStatementHandled
} from '../../api/income-statement'
import { H3 } from 'lib-components/typography'
import { Link } from 'react-router-dom'
import { IncomeStatement } from 'lib-common/api-types/incomeStatement'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { formatDate } from 'lib-common/date'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { featureFlags } from 'lib-customizations/employee'

interface Props {
  id: UUID
  open: boolean
}

const PersonIncome = React.memo(function PersonIncome({ id, open }: Props) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)
  const { incomes, setIncomes, reloadFamily } = useContext(PersonContext)
  const [incomeDataChanged, setIncomeDataChanged] = useState<boolean>(false)

  const loadData = React.useCallback(() => {
    setIncomes(Loading.of())
    void Promise.all([getIncomes(id), getIncomeStatements(id)])
      .then((results) => combine(...results))
      .then(setIncomes)
      .then(() => setIncomeDataChanged(true))
  }, [setIncomes, setIncomeDataChanged, id])

  // FIXME: This component shouldn't know about family's dependency on its data
  const reload = React.useCallback(() => {
    loadData()
    reloadFamily(id)
  }, [loadData, reloadFamily, id])

  useEffect(loadData, [loadData])
  useEffect(() => {
    if (incomeDataChanged && incomes.isSuccess) {
      const [inc] = incomes.value
      const missingIncomePeriodsString = getMissingIncomePeriodsString(
        inc,
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
  }, [i18n, incomes, incomeDataChanged, setErrorMessage])

  return (
    <CollapsibleSection
      icon={faEuroSign}
      title={i18n.personProfile.income.title}
      data-qa="person-income-collapsible"
      startCollapsed={!open}
    >
      {incomes.mapAll({
        loading() {
          return <Loader />
        },
        failure() {
          return <div>{i18n.personProfile.income.error}</div>
        },
        success([incomes, incomeStatements]) {
          return (
            <>
              {featureFlags.experimental?.incomeStatements ? (
                <IncomeStatements
                  personId={id}
                  incomeStatements={incomeStatements}
                  onSuccessfulUpdate={loadData}
                />
              ) : null}
              <Incomes
                personId={id}
                incomes={incomes}
                onSuccessfulUpdate={reload}
              />
            </>
          )
        }
      })}
    </CollapsibleSection>
  )
})

function IncomeStatements({
  personId,
  incomeStatements,
  onSuccessfulUpdate
}: {
  personId: UUID
  incomeStatements: IncomeStatement[]
  onSuccessfulUpdate: () => void
}) {
  const i18n = useTranslation().i18n.personProfile.incomeStatement
  return (
    <>
      <H3>{i18n.title}</H3>
      {incomeStatements.length === 0 ? (
        <div>{i18n.noIncomeStatements}</div>
      ) : (
        <Table>
          <Thead>
            <Tr>
              <Th>{i18n.incomeStatementHeading}</Th>
              <Th>{i18n.createdHeading}</Th>
              <Th>{i18n.handleHeading}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {incomeStatements.map((incomeStatement) => (
              <IncomeStatementRow
                key={incomeStatement.id}
                personId={personId}
                incomeStatement={incomeStatement}
                onSuccessfulUpdate={onSuccessfulUpdate}
              />
            ))}
          </Tbody>
        </Table>
      )}
    </>
  )
}

function IncomeStatementRow({
  personId,
  incomeStatement,
  onSuccessfulUpdate
}: {
  personId: UUID
  incomeStatement: IncomeStatement
  onSuccessfulUpdate: () => void
}) {
  const i18n = useTranslation().i18n.personProfile.incomeStatement

  const incomeStatementId = incomeStatement.id
  const [loading, setLoading] = React.useState(false)

  const setHandled = React.useCallback(
    async (handled: boolean) => {
      setLoading(true)
      try {
        await setIncomeStatementHandled(incomeStatementId, handled)
      } finally {
        setLoading(false)
      }
      onSuccessfulUpdate()
    },
    [incomeStatementId, onSuccessfulUpdate]
  )

  return (
    <Tr key={incomeStatement.id}>
      <Td verticalAlign="middle">
        <Link
          to={`/profile/${personId}/income-statement/${incomeStatement.id}`}
        >
          <FontAwesomeIcon icon={faFileAlt} /> {i18n.open}
        </Link>
      </Td>
      <Td verticalAlign="middle">{formatDate(incomeStatement.created)}</Td>
      <Td>
        <Checkbox
          label={i18n.handled}
          checked={incomeStatement.handlerName !== null}
          disabled={loading}
          onChange={setHandled}
        />
      </Td>
    </Tr>
  )
}

function Incomes({
  personId,
  incomes,
  onSuccessfulUpdate
}: {
  personId: UUID
  incomes: Income[]
  onSuccessfulUpdate: () => void
}) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)

  const [toggledIncome, setToggledIncome] = useState<IncomeId[]>([])
  const toggleIncome = (incomeId: IncomeId) =>
    setToggledIncome((prev) => toggleIncomeItem(incomeId, prev))

  function toggleIncomeItem(item: string, items: string[]): string[] {
    return items.includes(item)
      ? items.filter((el) => el !== item)
      : [item, ...items]
  }

  const [editing, setEditing] = useState<string>()
  const [deleting, setDeleting] = useState<string>()

  const toggleCreated = (res: Result<string>): Result<string> => {
    if (res.isSuccess) {
      toggleIncome(res.value)
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
          toggleIncome('new')
          setEditing('new')
        }}
        disabled={!!editing}
        data-qa="add-income-button"
      />
      <Gap size="m" />
      <IncomeList
        incomes={incomes}
        toggled={toggledIncome}
        toggle={toggleIncome}
        editing={editing}
        setEditing={setEditing}
        deleting={deleting}
        setDeleting={setDeleting}
        createIncome={(income: PartialIncome) =>
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
