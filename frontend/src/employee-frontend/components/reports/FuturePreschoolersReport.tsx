// SPDX-FileCopyrightText: foobar
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'

import { Loading, Result } from 'lib-common/api'
import {
  FuturePreschoolersReportRow,
  PreschoolUnitReportRow
} from 'lib-common/generated/api-types/reports'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import {
  getFuturePreschoolersReport,
  getPreschoolUnitsReport
} from '../../api/reports'
import { useTranslation } from '../../state/i18n'

import ReportDownload from './ReportDownload'
import { TableScrollable } from './common'

export default React.memo(function FuturePreschoolersReport() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<FuturePreschoolersReportRow[]>>(
    Loading.of()
  )
  const [municipalRows, setMunicipalRows] = useState<
    Result<PreschoolUnitReportRow[]>
  >(Loading.of())
  const [voucherRows, setVoucherRows] = useState<
    Result<PreschoolUnitReportRow[]>
  >(Loading.of())

  useEffect(() => {
    setRows(Loading.of())
    void getFuturePreschoolersReport().then(setRows)
  }, [])

  useEffect(() => {
    setMunicipalRows(Loading.of())
    void getPreschoolUnitsReport(true).then(setMunicipalRows)
  }, [])

  useEffect(() => {
    setVoucherRows(Loading.of())
    void getPreschoolUnitsReport(false).then(setVoucherRows)
  }, [])

  const reportRows: FuturePreschoolersReportRow[] = useMemo(
    () => rows.getOrElse([]),
    [rows]
  )

  const municipalUnitRows: PreschoolUnitReportRow[] = useMemo(
    () => municipalRows.getOrElse([]),
    [municipalRows]
  )

  const voucherUnitRows: PreschoolUnitReportRow[] = useMemo(
    () => voucherRows.getOrElse([]),
    [voucherRows]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.futurePreschoolers.title}</Title>

        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
          <>
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
                { label: 'huoltaja 1 postinumero', key: 'guardian1PostalCode' },
                {
                  label: 'huoltaja 1 postitoimipaikka',
                  key: 'guardian1PostOffice'
                },
                { label: 'huoltaja 1 matkapuhelin', key: 'guardian1Phone' },
                { label: 'huoltaja 1 sähköpostiosoite', key: 'guardian1Email' },
                { label: 'huoltaja 2 sukunimi', key: 'guardian2LastName' },
                { label: 'huoltaja 2 etunimi', key: 'guardian2FirstName' },
                { label: 'huoltaja 2 osoite', key: 'guardian2Address' },
                { label: 'huoltaja 2 postinumero', key: 'guardian2PostalCode' },
                {
                  label: 'huoltaja 2 postitoimipaikka',
                  key: 'guardian2PostOffice'
                },
                { label: 'huoltaja 2 matkapuhelin', key: 'guardian2Phone' },
                { label: 'huoltaja 2 sahköpostiosoite', key: 'guardian2Email' },
                { label: 'vuorohoidon tarve', key: 'shiftCare' },
                { label: 'kielikylvyn ryhmä', key: 'languageEmphasisGroup' },
                {
                  label: 'kaksivuotisessa esiopetuksessa',
                  key: 'twoYearPreschool'
                }
              ]}
              filename="Esiopetusoppilaat_rakenne.csv"
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.futurePreschoolers.childSsn}</Th>
                  <Th>{i18n.reports.futurePreschoolers.childLastName}</Th>
                  <Th>{i18n.reports.futurePreschoolers.childFirstName}</Th>
                  <Th>{i18n.reports.futurePreschoolers.childAddress}</Th>
                  <Th>{i18n.reports.futurePreschoolers.childPostalCode}</Th>
                  <Th>{i18n.reports.futurePreschoolers.childPostOffice}</Th>
                  <Th>{i18n.reports.futurePreschoolers.unitName}</Th>
                  <Th>{i18n.reports.futurePreschoolers.unitAddress}</Th>
                  <Th>{i18n.reports.futurePreschoolers.unitPostalCode}</Th>
                  <Th>{i18n.reports.futurePreschoolers.unitPostOffice}</Th>
                  <Th>{i18n.reports.futurePreschoolers.unitArea}</Th>
                  <Th>{i18n.reports.futurePreschoolers.guardian1LastName}</Th>
                  <Th>{i18n.reports.futurePreschoolers.guardian1FirstName}</Th>
                  <Th>{i18n.reports.futurePreschoolers.guardian1Address}</Th>
                  <Th>{i18n.reports.futurePreschoolers.guardian1PostalCode}</Th>
                  <Th>{i18n.reports.futurePreschoolers.guardian1PostOffice}</Th>
                  <Th>{i18n.reports.futurePreschoolers.guardian1Phone}</Th>
                  <Th>{i18n.reports.futurePreschoolers.guardian1Email}</Th>
                  <Th>{i18n.reports.futurePreschoolers.guardian2LastName}</Th>
                  <Th>{i18n.reports.futurePreschoolers.guardian2FirstName}</Th>
                  <Th>{i18n.reports.futurePreschoolers.guardian2Address}</Th>
                  <Th>{i18n.reports.futurePreschoolers.guardian2PostalCode}</Th>
                  <Th>{i18n.reports.futurePreschoolers.guardian2PostOffice}</Th>
                  <Th>{i18n.reports.futurePreschoolers.guardian2Phone}</Th>
                  <Th>{i18n.reports.futurePreschoolers.guardian2Email}</Th>
                  <Th>{i18n.reports.futurePreschoolers.shiftCare}</Th>
                  <Th>
                    {i18n.reports.futurePreschoolers.languageEmphasisGroup}
                  </Th>
                  <Th>{i18n.reports.futurePreschoolers.twoYearPreschool}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {reportRows.map((row: FuturePreschoolersReportRow) => (
                  <Tr key={row.id}>
                    <Td>{row.childSsn}</Td>
                    <Td>{row.childLastName}</Td>
                    <Td>{row.childLastName}</Td>
                    <Td>{row.childAddress}</Td>
                    <Td>{row.childPostalCode}</Td>
                    <Td>{row.childPostOffice}</Td>
                    <Td>{row.unitName}</Td>
                    <Td>{row.unitAddress}</Td>
                    <Td>{row.unitPostalCode}</Td>
                    <Td>{row.unitPostOffice}</Td>
                    <Td>{row.unitArea}</Td>
                    <Td>{row.guardian1LastName}</Td>
                    <Td>{row.guardian1FirstName}</Td>
                    <Td>{row.guardian1Address}</Td>
                    <Td>{row.guardian1PostalCode}</Td>
                    <Td>{row.guardian1PostOffice}</Td>
                    <Td>{row.guardian1Phone}</Td>
                    <Td>{row.guardian1Email}</Td>
                    <Td>{row.guardian2LastName}</Td>
                    <Td>{row.guardian2FirstName}</Td>
                    <Td>{row.guardian2Address}</Td>
                    <Td>{row.guardian2PostalCode}</Td>
                    <Td>{row.guardian2PostOffice}</Td>
                    <Td>{row.guardian2Phone}</Td>
                    <Td>{row.guardian2Email}</Td>
                    <Td>{row.shiftCare}</Td>
                    <Td>{row.languageEmphasisGroup}</Td>
                    <Td>{row.twoYearPreschool}</Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
          </>
        )}

        <Title size={2}>{i18n.reports.preschoolUnits.titleMunicipal}</Title>
        {municipalRows.isLoading && <Loader />}
        {municipalRows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {municipalRows.isSuccess && (
          <>
            <ReportDownload
              data={municipalUnitRows}
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
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.preschoolUnits.unit}</Th>
                  <Th>{i18n.reports.preschoolUnits.group}</Th>
                  <Th>{i18n.reports.preschoolUnits.address}</Th>
                  <Th>{i18n.reports.preschoolUnits.postalCode}</Th>
                  <Th>{i18n.reports.preschoolUnits.postOffice}</Th>
                  <Th>{i18n.reports.preschoolUnits.groupSize}</Th>
                  <Th>{i18n.reports.preschoolUnits.amongSchool}</Th>
                  <Th>{i18n.reports.preschoolUnits.shiftCare}</Th>
                  <Th>{i18n.reports.preschoolUnits.languageEmphasis}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {municipalUnitRows.map((row) => (
                  <Tr key={row.id}>
                    <Td>{row.unitName}</Td>
                    <Td>{row.groupName}</Td>
                    <Td>{row.address}</Td>
                    <Td>{row.postalCode}</Td>
                    <Td>{row.postOffice}</Td>
                    <Td>{row.groupSize}</Td>
                    <Td>{row.amongSchool}</Td>
                    <Td>{row.shiftCare}</Td>
                    <Td>{row.languageEmphasis}</Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
          </>
        )}

        <Title size={2}>{i18n.reports.preschoolUnits.titleVoucher}</Title>
        {voucherRows.isLoading && <Loader />}
        {voucherRows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {voucherRows.isSuccess && (
          <>
            <ReportDownload
              data={voucherUnitRows}
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
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.preschoolUnits.unit}</Th>
                  <Th>{i18n.reports.preschoolUnits.group}</Th>
                  <Th>{i18n.reports.preschoolUnits.address}</Th>
                  <Th>{i18n.reports.preschoolUnits.postalCode}</Th>
                  <Th>{i18n.reports.preschoolUnits.postOffice}</Th>
                  <Th>{i18n.reports.preschoolUnits.groupSize}</Th>
                  <Th>{i18n.reports.preschoolUnits.amongSchool}</Th>
                  <Th>{i18n.reports.preschoolUnits.shiftCare}</Th>
                  <Th>{i18n.reports.preschoolUnits.languageEmphasis}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {voucherUnitRows.map((row) => (
                  <Tr key={row.id}>
                    <Td>{row.unitName}</Td>
                    <Td>{row.groupName}</Td>
                    <Td>{row.address}</Td>
                    <Td>{row.postalCode}</Td>
                    <Td>{row.postOffice}</Td>
                    <Td>{row.groupSize}</Td>
                    <Td>{row.amongSchool}</Td>
                    <Td>{row.shiftCare}</Td>
                    <Td>{row.languageEmphasis}</Td>
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
