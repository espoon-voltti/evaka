// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import LocalDate from '@evaka/lib-common/local-date'
import { useTranslation } from '../../state/i18n'
import { H3 } from '@evaka/lib-components/typography'
import { faChevronLeft, faChevronRight } from '@evaka/lib-icons'
import IconButton from '@evaka/lib-components/atoms/buttons/IconButton'
import { Gap } from '@evaka/lib-components/white-space'

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
        gray
      />
      <Gap horizontal size={'L'} />
      <PeriodTitle data-qa="period-picker-month" noMargin>
        {i18n.datePicker.months[date.getMonth() - 1]} {date.getYear()}
      </PeriodTitle>
      <Gap horizontal size={'L'} />
      <IconButton
        onClick={increase}
        data-qa="period-picker-next-month"
        icon={faChevronRight}
        gray
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

const PeriodTitle = styled(H3)`
  text-transform: capitalize;
  min-width: 12ch;
  text-align: center;
`
