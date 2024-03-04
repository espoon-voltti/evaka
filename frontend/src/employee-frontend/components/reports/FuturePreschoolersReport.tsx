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
import { futurePreschoolersQuery, preschoolGroupsQuery } from './queries'

const DownloadWrapper = styled.div`
  margin-top: calc(-2 * ${defaultMargins.m});
`

export default React.memo(function FuturePreschoolersReport() {
  const { i18n } = useTranslation()
  const preschoolerRows = useQueryResult(futurePreschoolersQuery())
  const municipalGroupRows = useQueryResult(
    preschoolGroupsQuery({ municipal: true })
  )
  const privateVoucherGroupRows = useQueryResult(
    preschoolGroupsQuery({ municipal: false })
  )

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
                  { label: 'lapsen hetu', key: 'childSsn' },
                  { label: 'lapsen sukunimi', key: 'childLastName' },
                  { label: 'lapsen etunimi', key: 'childFirstName' },
                  { label: 'lapsen osoite', key: 'childAddress' },
                  { label: 'postinumero', key: 'childPostalCode' },
                  { label: 'toimipaikka', key: 'childPostOffice' },
                  { label: 'toimintayksikkö', key: 'unitName' },
                  { label: 'toimintayksikön osoite', key: 'unitAddress' },
                  { label: 'postinumero', key: 'unitPostalCode' },
                  { label: 'toimipaikka', key: 'unitPostOffice' },
                  { label: 'oppilaaksiottoalue', key: 'unitArea' },
                  { label: 'huoltaja 1 sukunimi', key: 'guardian1LastName' },
                  { label: 'huoltaja 1 etunimi', key: 'guardian1FirstName' },
                  { label: 'huoltaja 1 osoite', key: 'guardian1Address' },
                  {
                    label: 'huoltaja 1 postinumero',
                    key: 'guardian1PostalCode'
                  },
                  {
                    label: 'huoltaja 1 postitoimipaikka',
                    key: 'guardian1PostOffice'
                  },
                  { label: 'huoltaja 1 matkapuhelin', key: 'guardian1Phone' },
                  {
                    label: 'huoltaja 1 sähköpostiosoite',
                    key: 'guardian1Email'
                  },
                  { label: 'huoltaja 2 sukunimi', key: 'guardian2LastName' },
                  { label: 'huoltaja 2 etunimi', key: 'guardian2FirstName' },
                  { label: 'huoltaja 2 osoite', key: 'guardian2Address' },
                  {
                    label: 'huoltaja 2 postinumero',
                    key: 'guardian2PostalCode'
                  },
                  {
                    label: 'huoltaja 2 postitoimipaikka',
                    key: 'guardian2PostOffice'
                  },
                  { label: 'huoltaja 2 matkapuhelin', key: 'guardian2Phone' },
                  {
                    label: 'huoltaja 2 sahköpostiosoite',
                    key: 'guardian2Email'
                  },
                  { label: 'vuorohoidon tarve', key: 'shiftCare' },
                  { label: 'kielikylvyn ryhmä', key: 'languageEmphasisGroup' },
                  {
                    label: 'kaksivuotisessa esiopetuksessa',
                    key: 'twoYearPreschool'
                  }
                ]}
                filename="Esiopetusoppilaat_rakenne.csv"
              />
            </DownloadWrapper>
          </>
        ))}

        {renderResult(municipalGroupRows, (rows) => (
          <>
            <Title size={4}>
              {i18n.reports.futurePreschoolers.municipalGroupCount(rows.length)}
            </Title>
            <DownloadWrapper>
              <ReportDownload
                data={rows}
                headers={[
                  { label: 'Yksikön nimi', key: 'unitName' },
                  { label: 'Ryhmän nimi', key: 'groupName' },
                  { label: 'Lähiosoite', key: 'address' },
                  { label: 'Postinumero', key: 'postalCode' },
                  { label: 'Postitoimipaikka', key: 'postOffice' },
                  { label: 'Paikkojen määrä', key: 'groupSize' },
                  { label: 'Koulun yhteydessä', key: 'amongSchool' },
                  { label: 'vuorohoito', key: 'shiftCare' },
                  { label: 'kielikylpy', key: 'languageEmphasis' }
                ]}
                filename="Esiopetusryhmat_yksikot.csv"
              />
            </DownloadWrapper>
          </>
        ))}

        {renderResult(privateVoucherGroupRows, (rows) => (
          <>
            <Title size={4}>
              {i18n.reports.futurePreschoolers.privateVoucherGroupCount(
                rows.length
              )}
            </Title>
            <DownloadWrapper>
              <ReportDownload
                data={rows}
                headers={[
                  { label: 'Yksikön nimi', key: 'unitName' },
                  { label: 'Ryhmän nimi', key: 'groupName' },
                  { label: 'Lähiosoite', key: 'address' },
                  { label: 'Postinumero', key: 'postalCode' },
                  { label: 'Postitoimipaikka', key: 'postOffice' },
                  { label: 'Paikkojen määrä', key: 'groupSize' },
                  { label: 'Koulun yhteydessä', key: 'amongSchool' },
                  { label: 'vuorohoito', key: 'shiftCare' },
                  { label: 'kielikylpy', key: 'languageEmphasis' }
                ]}
                filename="Esiopetusryhmat_palveluseteli.csv"
              />
            </DownloadWrapper>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
