// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { range } from 'lodash'
import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { Loading, Result, Success } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { getInvoiceGeneratorDiffReport } from '../../api/reports'
import { useTranslation } from '../../state/i18n'
import { FlexRow } from '../common/styled/containers'
import { FilterLabel, FilterRow } from './common'

const Wrapper = styled.div`
  width: 100%;
`

const monthOptions = range(1, 13)
const yearOptions = range(
  LocalDate.today().year,
  LocalDate.today().year - 4,
  -1
)

interface InvoiceGeneratorDiffFilters {
  year: number
  month: number
}

function InvoiceGeneratorDiff() {
  const { i18n } = useTranslation()
  const [report, setReport] = useState<Result<string>>(Success.of('{}'))
  const today = LocalDate.today()
  const [filters, setFilters] = useState<InvoiceGeneratorDiffFilters>({
    year: today.year,
    month: today.month
  })

  useEffect(() => {
    setReport(Loading.of())
    void getInvoiceGeneratorDiffReport(filters).then(setReport)
  }, [filters])

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.invoiceGeneratorDiff.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <Wrapper>
              <Combobox
                items={monthOptions}
                selectedItem={filters.month}
                onChange={(month) => {
                  if (month !== null) {
                    setFilters({ ...filters, month })
                  }
                }}
                placeholder={i18n.common.month}
                getItemLabel={(month) => i18n.datePicker.months[month - 1]}
              />
            </Wrapper>
            <Wrapper>
              <Combobox
                items={yearOptions}
                selectedItem={filters.year}
                onChange={(year) => {
                  if (year !== null) {
                    setFilters({ ...filters, year })
                  }
                }}
                placeholder={i18n.common.year}
              />
            </Wrapper>
          </FlexRow>
        </FilterRow>
        {report.isLoading && <Loader />}
        {report.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {report.isSuccess && (
          <>
            <h1>{i18n.reports.invoiceGeneratorDiff.report}</h1>
            <p>{JSON.stringify(report.value, null, 2)}</p>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default InvoiceGeneratorDiff
