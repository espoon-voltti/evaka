// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { faEuroSign } from 'icon-set'
import { Gap } from '~components/shared/layout/white-space'
import Loader from '~components/shared/atoms/Loader'
import CollapsibleSection from 'components/shared/molecules/CollapsibleSection'
import IncomeList from './income/IncomeList'
import { useTranslation } from '~state/i18n'
import { UIContext } from '~state/ui'
import { PersonContext } from '~state/person'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import {
  getIncomes,
  createIncome,
  updateIncome,
  deleteIncome
} from '~api/income'
import { Income, PartialIncome, IncomeId } from '~types/income'
import { UUID } from '~types'
import { AddButtonRow } from 'components/shared/atoms/buttons/AddButton'
import { getMissingIncomePeriodStrings } from './income/missingIncomePeriodUtils'

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
  const [incomeDataLoaded, setIncomeDataLoaded] = useState<boolean>(false)
  const [toggledIncome, setToggledIncome] = useState<IncomeId[]>([])
  const toggleIncome = (incomeId: IncomeId) =>
    setToggledIncome((prev) => toggleIncomeItem(incomeId, prev))
  function toggleIncomeItem(item: string, items: string[]): string[] {
    return items.includes(item)
      ? items.filter((el) => el !== item)
      : [item, ...items]
  }

  const loadData = () => {
    setIncomes(Loading())
    void getIncomes(id)
      .then(setIncomes)
      .then(() => setIncomeDataLoaded(true))
  }

  // FIXME: This component shouldn't know about family's dependency on its data
  const reload = () => {
    loadData()
    reloadFamily(id)
  }

  useEffect(loadData, [id, setIncomes])
  useEffect(() => {
    if (incomeDataLoaded) {
      if (!isLoading(incomes) && !isFailure(incomes)) {
        const missingIncomePeriodStrings = getMissingIncomePeriodStrings(
          incomes.data
        )
        if (missingIncomePeriodStrings.length) {
          setErrorMessage({
            type: 'warning',
            title:
              i18n.personProfile.income.details.missingIncomeDaysWarningTitle,
            text: i18n.personProfile.income.details.missingIncomeDaysWarningText(
              missingIncomePeriodStrings
            )
          })
        }
      }
    }
  }, [incomes])

  const handleErrors = (res: Result<unknown>) => {
    if (isFailure(res)) {
      const text =
        res.error.statusCode === 409
          ? i18n.personProfile.income.details.conflictErrorText
          : undefined

      setErrorMessage({
        type: 'error',
        title: i18n.personProfile.income.details.updateError,
        text
      })

      throw res.error.message
    }
  }

  const toggleCreated = (res: Result<string>): Result<string> => {
    if (isSuccess(res)) {
      toggleIncome(res.data)
    }
    return res
  }

  const onSuccessfulUpdate = () => {
    setEditing(undefined)
    reload()
  }

  const content = () => {
    if (isLoading(incomes)) return <Loader />
    if (isFailure(incomes)) return <div>{i18n.personProfile.income.error}</div>
    return (
      <IncomeList
        incomes={incomes.data}
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
