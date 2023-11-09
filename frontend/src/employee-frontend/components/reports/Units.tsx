// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { StaticCheckBox } from 'lib-components/atoms/form/Checkbox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { TableScrollable } from './common'
import { unitsReportQuery } from './queries'

const CheckboxTh = styled(Th)``

export default React.memo(function MissingHeadOfFamily() {
  const { i18n } = useTranslation()

  const rows = useQueryResult(unitsReportQuery()).map((units) =>
    sortBy(units, (u) => u.name)
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.units.title}</Title>

        {renderResult(rows, (rows) => (
          <>
            <ReportDownload
              data={rows}
              headers={[
                { label: i18n.reports.units.name, key: 'name' },
                { label: i18n.reports.units.careAreaName, key: 'careAreaName' },
                {
                  label: i18n.reports.units.careTypeCentre,
                  key: 'careTypeCentre'
                },
                {
                  label: i18n.reports.units.careTypeFamilyStr,
                  key: 'careTypeFamily'
                },
                {
                  label: i18n.reports.units.careTypeGroupFamilyStr,
                  key: 'careTypeGroupFamily'
                },
                { label: i18n.reports.units.careTypeClub, key: 'careTypeClub' },
                {
                  label: i18n.reports.units.careTypePreschool,
                  key: 'careTypePreschool'
                },
                {
                  label: i18n.reports.units.careTypePreparatoryEducation,
                  key: 'careTypePreparatoryEducation'
                },
                { label: i18n.reports.units.clubApplyStr, key: 'clubApply' },
                {
                  label: i18n.reports.units.daycareApplyStr,
                  key: 'daycareApply'
                },
                {
                  label: i18n.reports.units.preschoolApplyStr,
                  key: 'preschoolApply'
                },
                { label: i18n.reports.units.providerType, key: 'providerType' },
                {
                  label: i18n.reports.units.uploadToVarda,
                  key: 'uploadToVarda'
                },
                {
                  label: i18n.reports.units.uploadChildrenToVarda,
                  key: 'uploadChildrenToVarda'
                },
                {
                  label: i18n.reports.units.uploadToKoski,
                  key: 'uploadToKoski'
                },
                {
                  label: i18n.reports.units.invoicedByMunicipality,
                  key: 'invoicedByMunicipality'
                },
                { label: i18n.reports.units.costCenter, key: 'costCenter' },
                {
                  label: i18n.reports.units.unitManagerName,
                  key: 'unitManagerName'
                }
              ]}
              filename="Yksiköt.csv"
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.units.name}</Th>
                  <Th>{i18n.reports.units.careAreaName}</Th>
                  <CheckboxTh>{i18n.reports.units.careTypeCentre}</CheckboxTh>
                  <CheckboxTh>{i18n.reports.units.careTypeFamily}</CheckboxTh>
                  <CheckboxTh>
                    {i18n.reports.units.careTypeGroupFamily}
                  </CheckboxTh>
                  <CheckboxTh>{i18n.reports.units.careTypeClub}</CheckboxTh>
                  <CheckboxTh>
                    {i18n.reports.units.careTypePreschool}
                  </CheckboxTh>
                  <CheckboxTh>
                    {i18n.reports.units.careTypePreparatoryEducation}
                  </CheckboxTh>
                  <CheckboxTh>{i18n.reports.units.clubApply}</CheckboxTh>
                  <CheckboxTh>{i18n.reports.units.daycareApply}</CheckboxTh>
                  <CheckboxTh>{i18n.reports.units.preschoolApply}</CheckboxTh>
                  <Th>{i18n.reports.units.providerType}</Th>
                  <CheckboxTh>{i18n.reports.units.uploadToVarda}</CheckboxTh>
                  <CheckboxTh>
                    {i18n.reports.units.uploadChildrenToVarda}
                  </CheckboxTh>
                  <CheckboxTh>{i18n.reports.units.uploadToKoski}</CheckboxTh>
                  <CheckboxTh>
                    {i18n.reports.units.invoicedByMunicipality}
                  </CheckboxTh>
                  <Th>{i18n.reports.units.costCenter}</Th>
                  <Th>{i18n.reports.units.unitManagerName}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.map((row) => (
                  <Tr key={row.id}>
                    <Td>
                      <Link to={`/units/${row.id}`}>{row.name}</Link>
                    </Td>
                    <Td>{row.careAreaName}</Td>
                    <Td>
                      <StaticCheckBox checked={row.careTypeCentre} />
                    </Td>
                    <Td>
                      <StaticCheckBox checked={row.careTypeFamily} />
                    </Td>
                    <Td>
                      <StaticCheckBox checked={row.careTypeGroupFamily} />
                    </Td>
                    <Td>
                      <StaticCheckBox checked={row.careTypeClub} />
                    </Td>
                    <Td>
                      <StaticCheckBox checked={row.careTypePreschool} />
                    </Td>
                    <Td>
                      <StaticCheckBox
                        checked={row.careTypePreparatoryEducation}
                      />
                    </Td>
                    <Td>
                      <StaticCheckBox checked={row.clubApply} />
                    </Td>
                    <Td>
                      <StaticCheckBox checked={row.daycareApply} />
                    </Td>
                    <Td>
                      <StaticCheckBox checked={row.preschoolApply} />
                    </Td>
                    <Td>{i18n.common.providerType[row.providerType]}</Td>
                    <Td>
                      <StaticCheckBox checked={row.uploadToVarda} />
                    </Td>
                    <Td>
                      <StaticCheckBox checked={row.uploadChildrenToVarda} />
                    </Td>
                    <Td>
                      <StaticCheckBox checked={row.uploadToKoski} />
                    </Td>
                    <Td>
                      <StaticCheckBox checked={row.invoicedByMunicipality} />
                    </Td>
                    <Td>{row.costCenter}</Td>
                    <Td>{row.unitManagerName}</Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
