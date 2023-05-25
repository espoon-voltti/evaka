// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useLang, useTranslation } from 'citizen-frontend/localization'
import DateRange, { Tense } from 'lib-common/date-range'
import { ServiceNeedSummary } from 'lib-common/generated/api-types/serviceneed'
import LocalDate from 'lib-common/local-date'
import { StaticChip } from 'lib-components/atoms/Chip'
import { Table, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import colors from 'lib-customizations/common'

const colorsByTense: Record<Tense, string> = {
  past: colors.grayscale.g15,
  present: colors.main.m1,
  future: colors.grayscale.g15
}

export default React.memo(function ServiceNeedTable({
  serviceNeeds
}: {
  serviceNeeds: ServiceNeedSummary[]
}) {
  const [lang] = useLang()
  const t = useTranslation()

  return serviceNeeds.length > 0 ? (
    <Table>
      <Thead>
        <Tr>
          <Th minimalWidth>{t.children.serviceNeed.validity}</Th>
          <Th>{t.children.serviceNeed.description}</Th>
          <Th>{t.children.serviceNeed.unit}</Th>
          <Th minimalWidth>{t.children.serviceNeed.status}</Th>
        </Tr>
      </Thead>
      {serviceNeeds
        .sort((a, b) => b.startDate.compareTo(a.startDate))
        .map((serviceNeed) => {
          const dateRange = new DateRange(
            serviceNeed.startDate,
            serviceNeed.endDate
          )
          const tense = dateRange.tenseAt(LocalDate.todayInHelsinkiTz())
          return (
            <Tr key={serviceNeed.startDate.formatIso()}>
              <Td minimalWidth>{dateRange.format()}</Td>
              <Td>
                {(lang === 'fi' && serviceNeed.option?.nameFi) ||
                  (lang === 'sv' && serviceNeed.option?.nameSv) ||
                  (lang === 'en' && serviceNeed.option?.nameEn) ||
                  ''}
              </Td>
              <Td>{serviceNeed.unitName}</Td>
              <Td minimalWidth>
                <StaticChip color={colorsByTense[tense]}>
                  {t.common.tense[tense]}
                </StaticChip>
              </Td>
            </Tr>
          )
        })}
    </Table>
  ) : (
    <>{t.children.serviceNeed.empty}</>
  )
})
