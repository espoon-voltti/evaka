import React from 'react'

import { IncompleteIncomeDbRow } from 'lib-common/generated/api-types/reports'
import { useQueryResult } from 'lib-common/query'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import { useTranslation } from '../../state/i18n'

import { TableScrollable } from './common'
import { incompleteIncomeReportQuery } from './queries'
import ReportDownload from './ReportDownload'

export default React.memo(function IncompleteIncomes() {
  const { i18n } = useTranslation()
  const report = useQueryResult(incompleteIncomeReportQuery())

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.incompleteIncomes.title}</Title>
        {report.isLoading && <Loader />}
        {report.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {report.isSuccess && (
          <>
            <ReportDownload<IncompleteIncomeDbRow>
              data={report.value}
              headers={[
                { label: 'Etunimi', key: 'firstName' },
                { label: 'Sukunimi', key: 'lastName' },
                { label: 'Alkupäivämäärä', key: 'validFrom' },
                { label: 'Päiväkoti', key: 'daycareName' },
                { label: 'Palvelualue', key: 'careareaName' }
              ]}
              filename="Puuttuvat_tulotiedot.csv"
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.incompleteIncomes.firstName}</Th>
                  <Th>{i18n.reports.incompleteIncomes.lastName}</Th>
                  <Th>{i18n.reports.incompleteIncomes.validFrom}</Th>
                  <Th>{i18n.reports.incompleteIncomes.daycareName}</Th>
                  <Th>{i18n.reports.incompleteIncomes.careareaName}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {report.value.map((row: IncompleteIncomeDbRow) => (
                  // eslint-disable-next-line react/jsx-key
                  <Tr>
                    <Td>{row.firstName}</Td>
                    <Td>{row.lastName}</Td>
                    <Td>{row.validFrom.toString()}</Td>
                    <Td>{row.daycareName}</Td>
                    <Td>{row.careareaName}</Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
})
