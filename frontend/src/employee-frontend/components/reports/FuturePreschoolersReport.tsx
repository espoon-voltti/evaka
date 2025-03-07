// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { defaultMargins } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import ReportDownload from './ReportDownload'
import {
  futurePreschoolersQuery,
  preschoolSourceUnitsQuery,
  preschoolUnitsQuery
} from './queries'

const DownloadWrapper = styled.div`
  margin-top: calc(-2 * ${defaultMargins.m});
`

export default React.memo(function FuturePreschoolersReport() {
  const { i18n } = useTranslation()
  const preschoolerRows = useQueryResult(futurePreschoolersQuery())
  const unitRows = useQueryResult(preschoolUnitsQuery())
  const currentUnitRows = useQueryResult(preschoolSourceUnitsQuery())

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.futurePreschoolers.title}</Title>

        {renderResult(preschoolerRows, (rows) => (
          <>
            <Title size={4}>
              {i18n.reports.futurePreschoolers.futurePreschoolersCount(
                rows.length
              )}
            </Title>

            <DownloadWrapper>
              <ReportDownload
                data={rows}
                columns={[
                  { label: 'lapsen_id', value: (row) => row.id },
                  {
                    label: 'lapsen_sukunimi',
                    value: (row) => row.childLastName
                  },
                  {
                    label: 'lapsen_etunimi',
                    value: (row) => row.childFirstName
                  },
                  {
                    label: 'lapsen_syntyma_pvm',
                    value: (row) => row.childDateOfBirth.format()
                  },
                  { label: 'lapsen_kieli', value: (row) => row.childLanguage },
                  { label: 'lapsen_osoite', value: (row) => row.childAddress },
                  { label: 'postinumero', value: (row) => row.childPostalCode },
                  { label: 'toimipaikka', value: (row) => row.childPostOffice },
                  { label: 'toimintayksikon_id', value: (row) => row.unitId },
                  {
                    label: 'toimintayksikon_nimi',
                    value: (row) => row.unitName
                  },
                  {
                    label: 'ominaisuudet',
                    value: (row) => row.options.join(', ')
                  }
                ]}
                filename="Esiopetusoppilaat_rakenne.csv"
              />
            </DownloadWrapper>
          </>
        ))}

        {renderResult(unitRows, (rows) => (
          <>
            <Title size={4}>
              {i18n.reports.futurePreschoolers.preschoolUnitCount(rows.length)}
            </Title>
            <DownloadWrapper>
              <ReportDownload
                data={rows}
                columns={[
                  { label: 'Id', value: (row) => row.id },
                  { label: 'Nimi', value: (row) => row.unitName },
                  { label: 'Lahiosoite', value: (row) => row.address },
                  { label: 'Postinumero', value: (row) => row.postalCode },
                  { label: 'Postitoimipaikka', value: (row) => row.postOffice },
                  { label: 'Paikkojen_lkm', value: (row) => row.unitSize },
                  {
                    label: 'ominaisuudet',
                    value: (row) => row.options.join(', ')
                  }
                ]}
                filename="Esiopetusyksikot.csv"
              />
            </DownloadWrapper>
          </>
        ))}

        {renderResult(currentUnitRows, (rows) => (
          <>
            <Title size={4}>
              {i18n.reports.futurePreschoolers.sourceUnitCount(rows.length)}
            </Title>
            <DownloadWrapper>
              <ReportDownload
                data={rows}
                columns={[
                  { label: 'Id', value: (row) => row.id },
                  { label: 'Nimi', value: (row) => row.unitName },
                  { label: 'Lahiosoite', value: (row) => row.address },
                  { label: 'Postinumero', value: (row) => row.postalCode },
                  { label: 'Postitoimipaikka', value: (row) => row.postOffice }
                ]}
                filename="Toimintayksikot.csv"
              />
            </DownloadWrapper>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
