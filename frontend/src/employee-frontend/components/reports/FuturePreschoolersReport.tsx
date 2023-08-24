// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'

import { Loading, Result } from 'lib-common/api'
import {
  FuturePreschoolersReportRow,
  PreschoolGroupsReportRow
} from 'lib-common/generated/api-types/reports'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { defaultMargins } from 'lib-components/white-space'

import {
  getFuturePreschoolersReport,
  getPreschoolGroupsReport
} from '../../api/reports'
import { useTranslation } from '../../state/i18n'

import ReportDownload from './ReportDownload'

const DownloadWrapper = styled.div`
  margin-top: calc(-2 * ${defaultMargins.m});
`

export default React.memo(function FuturePreschoolersReport() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<FuturePreschoolersReportRow[]>>(
    Loading.of()
  )
  const [municipalRows, setMunicipalRows] = useState<
    Result<PreschoolGroupsReportRow[]>
  >(Loading.of())
  const [voucherRows, setVoucherRows] = useState<
    Result<PreschoolGroupsReportRow[]>
  >(Loading.of())

  useEffect(() => {
    setRows(Loading.of())
    void getFuturePreschoolersReport().then(setRows)
  }, [])

  useEffect(() => {
    setMunicipalRows(Loading.of())
    void getPreschoolGroupsReport(true).then(setMunicipalRows)
  }, [])

  useEffect(() => {
    setVoucherRows(Loading.of())
    void getPreschoolGroupsReport(false).then(setVoucherRows)
  }, [])

  const reportRows: FuturePreschoolersReportRow[] = useMemo(
    () => rows.getOrElse([]),
    [rows]
  )

  const municipalGroupRows: PreschoolGroupsReportRow[] = useMemo(
    () => municipalRows.getOrElse([]),
    [municipalRows]
  )

  const privateVoucherGroupRows: PreschoolGroupsReportRow[] = useMemo(
    () => voucherRows.getOrElse([]),
    [voucherRows]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.futurePreschoolers.title}</Title>

        <Title size={4}>
          {i18n.reports.futurePreschoolers.futurePreschoolersCount(
            reportRows.length
          )}
        </Title>
        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
          <DownloadWrapper>
            <ReportDownload
              data={reportRows}
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
        )}

        <Title size={4}>
          {i18n.reports.futurePreschoolers.municipalGroupCount(
            municipalGroupRows.length
          )}
        </Title>
        {municipalRows.isLoading && <Loader />}
        {municipalRows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {municipalRows.isSuccess && (
          <DownloadWrapper>
            <ReportDownload
              data={municipalGroupRows}
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
        )}

        <Title size={4}>
          {i18n.reports.futurePreschoolers.privateVoucherGroupCount(
            privateVoucherGroupRows.length
          )}
        </Title>
        {voucherRows.isLoading && <Loader />}
        {voucherRows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {voucherRows.isSuccess && (
          <DownloadWrapper>
            <ReportDownload
              data={privateVoucherGroupRows}
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
        )}
      </ContentArea>
    </Container>
  )
})
