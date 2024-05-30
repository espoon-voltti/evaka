// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { Title } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faChevronLeft, faChevronRight } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

interface Props {
  onChange: (date: LocalDate) => void
  date: LocalDate
}

export default React.memo(function PeriodPicker({ onChange, date }: Props) {
  const { i18n } = useTranslation()
  const increase = () => {
    onChange(date.addMonths(1).withDate(1))
  }

  const decrease = () => {
    onChange(date.subMonths(1).withDate(1))
  }

  return (
    <Container>
      <IconButton
        onClick={decrease}
        data-qa="period-picker-previous-month"
        icon={faChevronLeft}
        color="gray"
        aria-label={i18n.common.datePicker.previousMonthLabel}
      />
      <Gap horizontal size="L" />
      <PeriodTitle primary centered data-qa="period-picker-month">
        {i18n.datePicker.months[date.getMonth() - 1]} {date.getYear()}
      </PeriodTitle>
      <Gap horizontal size="L" />
      <IconButton
        onClick={increase}
        data-qa="period-picker-next-month"
        icon={faChevronRight}
        color="gray"
        aria-label={i18n.common.datePicker.nextMonthLabel}
      />
    </Container>
  )
})

const Container = styled.div`
  display: flex;
  justify-content: center;
  margin-bottom: 1rem;
  align-items: center;

  button {
    @media print {
      display: none;
    }
  }
`

const PeriodTitle = styled(Title)`
  text-transform: capitalize;
  min-width: 14ch;
`
