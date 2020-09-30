// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { LabelValueList, LabelValueListItem } from '~components/shared/alpha'
import { useTranslation } from '~state/i18n'
import { Income } from '~types/income'
import { formatDate } from '~utils/date'

interface Props {
  income: Income
  editing?: boolean
}

const IncomeItemDetails = React.memo(function IncomeItemDetails({
  income,
  editing
}: Props) {
  const { i18n } = useTranslation()

  return (
    <LabelValueList>
      {!editing ? (
        <>
          <LabelValueListItem
            label={i18n.personProfile.income.details.dateRange}
            value={`${income.validFrom.format()} - ${
              income.validTo ? income.validTo.format() : ''
            }`}
            dataQa="income-date-range"
          />
          <LabelValueListItem
            label={i18n.personProfile.income.details.effect}
            value={
              i18n.personProfile.income.details.effectOptions[income.effect]
            }
            dataQa="income-effect"
          />
          <LabelValueListItem
            label={i18n.personProfile.income.details.echa}
            value={income.worksAtECHA ? i18n.common.yes : i18n.common.no}
            dataQa="income-echa"
          />
          <LabelValueListItem
            label={i18n.personProfile.income.details.entrepreneur}
            value={income.isEntrepreneur ? i18n.common.yes : i18n.common.no}
            dataQa="income-entrepreneur"
          />
        </>
      ) : null}
      <LabelValueListItem
        label={i18n.personProfile.income.details.updated}
        value={formatDate(income.updatedAt)}
        dataQa="income-updated"
      />
      <LabelValueListItem
        label={i18n.personProfile.income.details.handler}
        value={income.updatedBy}
        dataQa="income-handler"
      />
    </LabelValueList>
  )
})

export default IncomeItemDetails
