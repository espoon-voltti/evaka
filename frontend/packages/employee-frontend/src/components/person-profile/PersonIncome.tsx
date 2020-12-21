// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { faEuroSign } from '@evaka/lib-icons'
import { Gap } from '@evaka/lib-components/src/white-space'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import CollapsibleSection from '@evaka/lib-components/src/molecules/CollapsibleSection'
import IncomeList from './income/IncomeList'
import { useTranslation } from '~state/i18n'
import { UIContext } from '~state/ui'
import { PersonContext } from '~state/person'
import { Loading, Result } from '~api'
import {
  getIncomes,
  createIncome,
  updateIncome,
  deleteIncome
} from '~api/income'
import { Income, PartialIncome, IncomeId } from '~types/income'
import { UUID } from '~types'
import { AddButtonRow } from '@evaka/lib-components/src/atoms/buttons/AddButton'
import { getMissingIncomePeriodsString } from './income/missingIncomePeriodUtils'

interface Props {
  id: UUID
  open: boolean
}

const PersonIncome = React.memo(function PersonIncome({ id, open }: Props) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)
  const { incomes, setIncomes, reloadFamily } = useContext(PersonContext)
  const [editing, setEditing] = useState<string>()
  const [deleting, setDeleting] = useState<string>()
  const [incomeDataChanged, setIncomeDataChanged] = useState<boolean>(false)
  const [toggledIncome, setToggledIncome] = useState<IncomeId[]>([])
  const toggleIncome = (incomeId: IncomeId) =>
    setToggledIncome((prev) => toggleIncomeItem(incomeId, prev))
  function toggleIncomeItem(item: string, items: string[]): string[] {
    return items.includes(item)
      ? items.filter((el) => el !== item)
      : [item, ...items]
  }

  const loadData = () => {
    setIncomes(Loading.of())
    void getIncomes(id)
      .then(setIncomes)
      .then(() => setIncomeDataChanged(true))
  }

  // FIXME: This component shouldn't know about family's dependency on its data
  const reload = () => {
    loadData()
    reloadFamily(id)
  }

  useEffect(loadData, [id, setIncomes])
  useEffect(() => {
    if (incomeDataChanged && incomes.isSuccess) {
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
          )
        })
      }
    }
  }, [incomes])

  const handleErrors = (res: Result<unknown>) => {
    if (res.isFailure) {
      const text =
        res.statusCode === 409
          ? i18n.personProfile.income.details.conflictErrorText
          : undefined

      setErrorMessage({
        type: 'error',
        title: i18n.personProfile.income.details.updateError,
        text
      })

      throw res.message
    }
  }

  const toggleCreated = (res: Result<string>): Result<string> => {
    if (res.isSuccess) {
      toggleIncome(res.value)
    }
    return res
  }

  const onSuccessfulUpdate = () => {
    setEditing(undefined)
    reload()
  }

  const content = () => {
    if (incomes.isLoading) return <Loader />
    if (incomes.isFailure) return <div>{i18n.personProfile.income.error}</div>
    return (
      <IncomeList
        incomes={incomes.value}
        toggled={toggledIncome}
        toggle={toggleIncome}
        editing={editing}
        setEditing={setEditing}
        deleting={deleting}
        setDeleting={setDeleting}
        createIncome={(income: PartialIncome) =>
          createIncome(id, income).then(toggleCreated).then(handleErrors)
        }
        updateIncome={(incomeId: UUID, income: Income) =>
          updateIncome(incomeId, income).then(handleErrors)
        }
        deleteIncome={(incomeId: UUID) =>
          deleteIncome(incomeId).then(handleErrors)
        }
        onSuccessfulUpdate={onSuccessfulUpdate}
      />
    )
  }

  return (
    <CollapsibleSection
      icon={faEuroSign}
      title={i18n.personProfile.income.title}
      dataQa="person-income-collapsible"
      startCollapsed={!open}
    >
      <AddButtonRow
        text={i18n.personProfile.income.add}
        onClick={() => {
          toggleIncome('new')
          setEditing('new')
        }}
        disabled={!!editing}
        dataQa="add-income-button"
      />
      <Gap size="m" />
      {content()}
    </CollapsibleSection>
  )
})

export default PersonIncome
