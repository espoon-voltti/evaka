// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { faEuroSign } from 'icon-set'
import { Collapsible, Loader } from '~components/shared/alpha'
import IncomeList from './income/IncomeList'
import { useTranslation } from '~state/i18n'
import { UIContext } from '~state/ui'
import { PersonContext } from '~state/person'
import { isFailure, isLoading, Loading, isSuccess, Result } from '~api'
import {
  getIncomes,
  createIncome,
  updateIncome,
  deleteIncome
} from '~api/income'
import { Income, PartialIncome, IncomeId } from '~types/income'
import { UUID } from '~types'
import { AddButtonRow } from 'components/shared/atoms/buttons/AddButton'

interface Props {
  id: UUID
  open: boolean
}

const PersonIncome = React.memo(function PersonIncome({ id, open }: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode, setErrorMessage } = useContext(
    UIContext
  )
  const { incomes, setIncomes, reloadFamily } = useContext(PersonContext)
  const [toggled, setToggled] = useState(open)
  const toggle = useCallback(() => setToggled((toggled) => !toggled), [
    setToggled
  ])

  const loadData = () => {
    setIncomes(Loading())
    void getIncomes(id).then(setIncomes)
  }

  // FIXME: This component shouldn't know about family's dependency on its data
  const reload = () => {
    loadData()
    reloadFamily(id)
  }

  useEffect(loadData, [id, setIncomes])

  function toggleIncomeItem(item: IncomeId, items: IncomeId[]): IncomeId[] {
    if (uiMode === `edit-person-income-${item}`) {
      setErrorMessage({
        type: 'warning',
        title: i18n.personProfile.income.details.closeWarning,
        text: i18n.personProfile.income.details.closeWarningText
      })
      return items
    }

    return items.includes(item)
      ? items.filter((el) => el !== item)
      : [item, ...items]
  }

  const [toggledIncome, setToggledIncome] = useState<IncomeId[]>([])
  const toggleIncome = (incomeId: IncomeId) =>
    setToggledIncome((prev) => toggleIncomeItem(incomeId, prev))

  const handleUpdate = (res: Result<void>) => {
    if (isSuccess(res)) {
      clearUiMode()
      reload()
    } else if (isFailure(res) && res.error.statusCode === 409) {
      setErrorMessage({
        type: 'error',
        title: i18n.personProfile.income.details.updateError,
        text: i18n.personProfile.income.details.conflictErrorText
      })
    } else {
      setErrorMessage({
        type: 'error',
        title: i18n.personProfile.income.details.updateError
      })
    }
  }

  const content = () => {
    if (isLoading(incomes)) return <Loader />
    if (isFailure(incomes)) return <div>{i18n.personProfile.income.error}</div>
    return (
      <IncomeList
        incomes={incomes.data}
        toggled={toggledIncome}
        toggle={toggleIncome}
        createIncome={(income: PartialIncome) =>
          createIncome(id, income).then(handleUpdate)
        }
        updateIncome={(incomeId: UUID, income: Income) =>
          updateIncome(incomeId, income).then(handleUpdate)
        }
        deleteIncome={(incomeId: UUID) =>
          deleteIncome(incomeId).then(handleUpdate)
        }
      />
    )
  }

  return (
    <Collapsible
      icon={faEuroSign}
      title={i18n.personProfile.income.title}
      open={toggled}
      onToggle={toggle}
      dataQa="person-income-collapsible"
    >
      <AddButtonRow
        text={i18n.personProfile.income.add}
        onClick={() => {
          toggleIncome('new')
          toggleUiMode('edit-person-income-new')
        }}
        disabled={!!uiMode}
        dataQa="add-income-button"
      />
      {content()}
    </Collapsible>
  )
})

export default PersonIncome
