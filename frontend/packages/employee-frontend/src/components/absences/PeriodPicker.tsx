// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import { useTranslation } from '~state/i18n'
import './PeriodPicker.scss'
import { Title } from '~components/shared/alpha'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faChevronLeft, faChevronRight } from '@evaka/icons'

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
      <button
        onClick={decrease}
        className={'is-plain'}
        data-qa="period-picker-previous-month"
      >
        <FontAwesomeIcon icon={faChevronLeft} />
      </button>
      <Title size={3} dataQa="period-picker-month">
        {i18n.datePicker.months[date.getMonth() - 1]} {date.getYear()}
      </Title>
      <button onClick={increase} data-qa="period-picker-next-month">
        <FontAwesomeIcon icon={faChevronRight} />
      </button>
    </div>
  )
}

export default PeriodPicker
