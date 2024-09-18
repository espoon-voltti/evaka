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
                headers={[
                  { label: 'lapsen_id', key: 'id' },
                  { label: 'lapsen_sukunimi', key: 'childLastName' },
                  { label: 'lapsen_etunimi', key: 'childFirstName' },
                  { label: 'lapsen_syntyma_pvm', key: 'childDateOfBirth' },
                  { label: 'lapsen_kieli', key: 'childLanguage' },
                  { label: 'lapsen_osoite', key: 'childAddress' },
                  { label: 'postinumero', key: 'childPostalCode' },
                  { label: 'toimipaikka', key: 'childPostOffice' },
                  { label: 'toimintayksikon_id', key: 'unitId' },
                  { label: 'toimintayksikon_nimi', key: 'unitName' },
                  { label: 'ominaisuudet', key: 'options' }
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
                headers={[
                  { label: 'Id', key: 'id' },
                  { label: 'Nimi', key: 'unitName' },
                  { label: 'Lahiosoite', key: 'address' },
                  { label: 'Postinumero', key: 'postalCode' },
                  { label: 'Postitoimipaikka', key: 'postOffice' },
                  { label: 'Paikkojen_lkm', key: 'unitSize' },
                  { label: 'ominaisuudet', key: 'options' }
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
                headers={[
                  { label: 'Id', key: 'id' },
                  { label: 'Nimi', key: 'unitName' },
                  { label: 'Lahiosoite', key: 'address' },
                  { label: 'Postinumero', key: 'postalCode' },
                  { label: 'Postitoimipaikka', key: 'postOffice' }
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
