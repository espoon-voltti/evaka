// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import * as _ from 'lodash'

import LocalDate from '@evaka/lib-common/src/local-date'
import { useTranslation } from '~state/i18n'
import './PeriodPicker.scss'
import Title from '~components/shared/atoms/Title'
import { faChevronLeft, faChevronRight } from 'icon-set'
import IconButton from '~components/shared/atoms/buttons/IconButton'
import { Gap } from '~components/shared/layout/white-space'

export enum PeriodPickerMode {
  MONTH
}

interface Props {
  mode: PeriodPickerMode
  onChange: (date: LocalDate) => void
  date: LocalDate
}

function PeriodPicker({ mode, onChange, date }: Props) {
  const { i18n } = useTranslation()
  const increase = () => {
    switch (mode) {
      case PeriodPickerMode.MONTH: {
        onChange(date.addMonths(1).withDate(1))
      }
    }
  }

  const decrease = () => {
    switch (mode) {
      case PeriodPickerMode.MONTH: {
        onChange(date.subMonths(1).withDate(1))
      }
    }
  }

  return (
    <div className={'period-picker'}>
      <IconButton
        onClick={decrease}
        className={'is-plain'}
        data-qa="period-picker-previous-month"
        icon={faChevronLeft}
        gray
      />
      <Gap horizontal size={'L'} />
      <Title size={3} data-qa="period-picker-month" noMargin>
        {_.startCase(i18n.datePicker.months[date.getMonth() - 1])}{' '}
        {date.getYear()}
      </Title>
      <Gap horizontal size={'L'} />
      <IconButton
        onClick={increase}
        data-qa="period-picker-next-month"
        icon={faChevronRight}
        gray
      />
    </div>
  )
}

export default PeriodPicker
