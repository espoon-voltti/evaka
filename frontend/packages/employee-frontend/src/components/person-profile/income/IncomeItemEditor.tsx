// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import LocalDate from '@evaka/lib-common/src/local-date'
import {
  Button,
  Buttons,
  Checkbox,
  Field,
  Radio,
  RadioGroup,
  Title
} from '~components/shared/alpha'
import DateRangeInput from '../../common/DateRangeInput'
import IncomeItemDetails from './IncomeItemDetails'
import IncomeTable from './IncomeTable'
import { useTranslation } from '~state/i18n'
import { incomeEffects, Income, PartialIncome } from '~types/income'

const ButtonsContainer = styled(Buttons)`
  margin: 20px 0;
`

const emptyIncome: PartialIncome = {
  effect: 'INCOME',
  data: {} as Partial<Income['data']>,
  isEntrepreneur: false,
  worksAtECHA: false,
  notes: '',
  validFrom: LocalDate.today(),
  validTo: undefined
}

interface Props {
  baseIncome?: Income
  cancel: () => void
  update: (income: Income) => void
  create: (income: PartialIncome) => void
}

const IncomeItemEditor = React.memo(function IncomeItemEditor({
  baseIncome,
  cancel,
  update,
  create
}: Props) {
  const { i18n } = useTranslation()

  const [editedIncome, setEditedIncome] = useState<PartialIncome>(
    baseIncome || emptyIncome
  )

  const [validationErrors, setValidationErrors] = useState<
    Partial<{ [K in keyof Income | 'dates']: boolean }>
  >({})

  return (
    <>
      <Field
        label={i18n.personProfile.income.details.dateRange}
        dataQa="income-date-range"
      >
        <DateRangeInput
          start={editedIncome.validFrom}
          end={editedIncome.validTo}
          onChange={(from: LocalDate, to?: LocalDate) =>
            setEditedIncome((prev) => ({
              ...prev,
              validFrom: from,
              validTo: to
            }))
          }
          onValidationResult={(hasErrors) =>
            setValidationErrors((prev) => ({ ...prev, dates: hasErrors }))
          }
          nullableEndDate
        />
      </Field>
      <RadioGroup
        label={i18n.personProfile.income.details.effect}
        dataQa="income-effect"
      >
        {incomeEffects.map((effect) => (
          <Radio
            key={effect}
            id={effect}
            label={i18n.personProfile.income.details.effectOptions[effect]}
            value={effect}
            model={editedIncome.effect}
            onChange={(v) =>
              setEditedIncome((prev) => ({ ...prev, effect: v }))
            }
            dataQa={`income-effect-${effect}`}
          />
        ))}
      </RadioGroup>

      <RadioGroup label={i18n.personProfile.income.details.miscTitle}>
        <Checkbox
          label={i18n.personProfile.income.details.echa}
          name="works-at-echa"
          checked={editedIncome.worksAtECHA}
          onChange={() =>
            setEditedIncome((prev) => ({
              ...prev,
              worksAtECHA: !prev.worksAtECHA
            }))
          }
        />
        <Checkbox
          label={i18n.personProfile.income.details.entrepreneur}
          name="is-entrepreneur"
          checked={editedIncome.isEntrepreneur}
          onChange={() =>
            setEditedIncome((prev) => ({
              ...prev,
              isEntrepreneur: !prev.isEntrepreneur
            }))
          }
        />
      </RadioGroup>
      {baseIncome ? <IncomeItemDetails income={baseIncome} editing /> : null}
      {editedIncome.effect === 'INCOME' ? (
        <>
          <div className="separator" />
          <Title size={4}>
            {i18n.personProfile.income.details.incomeTitle}
          </Title>
          <IncomeTable
            data={editedIncome.data}
            editing
            setData={(data) =>
              setEditedIncome((prev) => ({
                ...prev,
                data: { ...prev.data, ...data }
              }))
            }
            setValidationError={(v) =>
              setValidationErrors((prev) => ({ ...prev, data: v }))
            }
            type="income"
          />
          <Title size={4}>
            {i18n.personProfile.income.details.expensesTitle}
          </Title>
          <IncomeTable
            data={editedIncome.data}
            editing
            setData={(data) =>
              setEditedIncome((prev) => ({
                ...prev,
                data: { ...prev.data, ...data }
              }))
            }
            setValidationError={(v) =>
              setValidationErrors((prev) => ({ ...prev, data: v }))
            }
            type="expenses"
          />
        </>
      ) : null}
      <ButtonsContainer centered>
        <Button plain onClick={cancel}>
          {i18n.personProfile.income.details.cancel}
        </Button>
        <Button
          primary
          disabled={Object.values(validationErrors).some(Boolean)}
          onClick={() =>
            !baseIncome
              ? create(editedIncome)
              : update({ ...baseIncome, ...editedIncome })
          }
          dataQa="save-income"
        >
          {i18n.personProfile.income.details.save}
        </Button>
      </ButtonsContainer>
    </>
  )
})

export default IncomeItemEditor
