// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { useBoolean } from 'lib-common/form/hooks'
import { ServiceNeedOptionVoucherValueRange } from 'lib-common/generated/api-types/invoicing'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H4 } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'

export type ServiceNeedItemProps = {
  serviceNeed: string
  voucherValuesList: ServiceNeedOptionVoucherValueRange[]
}
export default React.memo(function ServiceNeedItem({
  serviceNeed,
  voucherValuesList
}: ServiceNeedItemProps) {
  const { i18n } = useTranslation()
  const [open, useOpen] = useBoolean(false)

  return (
    <>
      <CollapsibleContentArea
        opaque
        title={<H4>{serviceNeed}</H4>}
        open={open}
        toggleOpen={useOpen.toggle}
        paddingHorizontal="0"
        paddingVertical="0"
      >
        <H4>{i18n.financeBasics.serviceNeeds.voucherValues}</H4>
        <Table>
          <Thead>
            <Tr>
              <Th>{i18n.financeBasics.serviceNeeds.validity}</Th>
              <Th>{i18n.financeBasics.serviceNeeds.baseValue}</Th>
              <Th>{i18n.financeBasics.serviceNeeds.coefficient}</Th>
              <Th>{i18n.financeBasics.serviceNeeds.value}</Th>
              <Th>{i18n.financeBasics.serviceNeeds.baseValueUnder3y}</Th>
              <Th>{i18n.financeBasics.serviceNeeds.coefficientUnder3y}</Th>
              <Th>{i18n.financeBasics.serviceNeeds.valueUnder3y}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {voucherValuesList
              .sort((a, b) => b.range.start.compareTo(a.range.start))
              .map((voucherValue, i) => (
                <Tr key={i} data-qa="voucher-value-row">
                  <Td data-qa="validity">
                    {voucherValue.range.format('dd.MM.yyyy')}
                  </Td>
                  <Td data-qa="base-value">
                    {(voucherValue.baseValue / 100).toFixed(2)}
                  </Td>
                  <Td data-qa="coefficient">{voucherValue.coefficient}</Td>
                  <Td data-qa="value">
                    {(voucherValue.value / 100).toFixed(2)}
                  </Td>
                  <Td data-qa="base-value-under-3y">
                    {(voucherValue.baseValueUnder3y / 100).toFixed(2)}
                  </Td>
                  <Td data-qa="coefficient-under-3y">
                    {voucherValue.coefficientUnder3y}
                  </Td>
                  <Td data-qa="value-under-3y">
                    {(voucherValue.valueUnder3y / 100).toFixed(2)}
                  </Td>
                </Tr>
              ))}
          </Tbody>
        </Table>
      </CollapsibleContentArea>
      <HorizontalLine dashed={true} slim={true} />
    </>
  )
})
