// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'
import IncomeItemHeader from './IncomeItemHeader'
import IncomeItem from './IncomeItem'
import { useTranslation } from '~state/i18n'
import { UIContext } from '~state/ui'
import { Income, PartialIncome, IncomeId } from '~types/income'
import { UUID } from '~types'
import InfoModal from '~components/common/InfoModal'
import { faQuestion } from 'icon-set'

const IncomeListItem = styled.div`
  margin-bottom: 20px;
`

interface Props {
  incomes: Income[]
  toggled: IncomeId[]
  toggle: (v: IncomeId) => void
  createIncome: (income: PartialIncome) => void
  updateIncome: (incomeId: UUID, income: Income) => void
  deleteIncome: (incomeId: UUID) => void
}

const IncomeList = React.memo(function IncomeList({
  incomes,
  toggled,
  toggle,
  createIncome,
  updateIncome,
  deleteIncome
}: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)

  const editable = !uiMode
  const editingNew = uiMode === 'edit-person-income-new'
  const editingExisting = (id: UUID) => uiMode === `edit-person-income-${id}`
  const startEditing = (id: UUID) => () =>
    toggleUiMode(`edit-person-income-${id}`)
  const isToggled = (id: UUID) => toggled.includes(id)
  const startDeleting = (id: UUID) => () =>
    toggleUiMode(`delete-person-income-${id}`)
  const deleting = (id: UUID) => uiMode === `delete-person-income-${id}`

  const renderDeleteModal = (income: Income) => {
    const confirmText = `${
      i18n.personProfile.income.deleteModal.confirmText
    } ${income.validFrom.format()} - ${income.validTo?.format() ?? ''}?`
    return (
      <InfoModal
        iconColour={'orange'}
        title={i18n.personProfile.income.deleteModal.title}
        text={confirmText}
        resolveLabel={i18n.common.remove}
        rejectLabel={i18n.common.cancel}
        icon={faQuestion}
        reject={() => clearUiMode()}
        resolve={() => {
          clearUiMode()
          deleteIncome(income.id)
        }}
      />
    )
  }

  return (
    <>
      {editingNew && (
        <IncomeListItem key="new">
          <IncomeItemHeader
            title={i18n.personProfile.income.itemHeaderNew}
            toggled={isToggled('new')}
            toggle={() => toggle('new')}
            editable={editable}
            startEditing={() => undefined}
            startDeleting={() => undefined}
          />
          <IncomeItem
            editing={editingNew}
            cancel={() => {
              clearUiMode()
            }}
            createIncome={createIncome}
            updateIncome={() => undefined}
          />
        </IncomeListItem>
      )}
      {incomes.map((item: Income) => (
        <IncomeListItem
          key={`${item.validFrom.formatIso()}-${
            item.validTo?.formatIso() ?? ''
          }`}
        >
          {deleting(item.id) ? renderDeleteModal(item) : null}
          <IncomeItemHeader
            title={`${
              i18n.personProfile.income.itemHeader
            } ${item.validFrom.format()} - ${item.validTo?.format() ?? ''}`}
            toggled={isToggled(item.id)}
            toggle={() => toggle(item.id)}
            editable={editable}
            startEditing={startEditing(item.id)}
            startDeleting={startDeleting(item.id)}
          />
          {isToggled(item.id) ? (
            editingExisting(item.id) ? (
              <IncomeItem
                income={item}
                editing
                cancel={clearUiMode}
                createIncome={() => undefined}
                updateIncome={(income) => updateIncome(item.id, income)}
              />
            ) : (
              <IncomeItem income={item} />
            )
          ) : null}
        </IncomeListItem>
      ))}
    </>
  )
})

export default IncomeList
