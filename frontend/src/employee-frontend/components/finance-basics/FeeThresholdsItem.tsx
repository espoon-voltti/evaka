// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { FeeThresholds } from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { formatCents } from 'lib-common/money'
import { IconButton } from 'lib-components/atoms/buttons/IconButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H3, H4, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { Translations } from 'lib-customizations/employee'
import { faCopy, faPen, faQuestion } from 'lib-icons'

import { familySizes } from '../../types/finance-basics'
import StatusLabel from '../common/StatusLabel'

export const FeeThresholdsItem = React.memo(function FeeThresholdsItem({
  i18n,
  id,
  feeThresholds,
  copyThresholds,
  editThresholds,
  editing,
  'data-qa': dataQa
}: {
  i18n: Translations
  id: string
  feeThresholds: FeeThresholds
  copyThresholds: (feeThresholds: FeeThresholds) => void
  editThresholds: (id: string, feeThresholds: FeeThresholds) => void
  editing: boolean
  'data-qa': string
}) {
  const [showModal, setShowModal] = useState(false)
  return (
    <>
      <div className="separator large" />
      <div data-qa={dataQa}>
        <TitleContainer>
          <H3>
            {i18n.financeBasics.fees.validDuring}{' '}
            <span data-qa="validDuring">
              {feeThresholds.validDuring.format()}
            </span>
          </H3>
          <FixedSpaceRow>
            <IconButton
              icon={faCopy}
              onClick={() => copyThresholds(feeThresholds)}
              disabled={editing}
              data-qa="copy"
              aria-label={i18n.common.copy}
            />
            <IconButton
              icon={faPen}
              onClick={() => {
                if (
                  feeThresholds.validDuring.start.isAfter(
                    LocalDate.todayInSystemTz()
                  )
                ) {
                  editThresholds(id, feeThresholds)
                } else {
                  setShowModal(true)
                }
              }}
              disabled={editing}
              data-qa="edit"
              aria-label={i18n.common.edit}
            />
            <StatusLabel dateRange={feeThresholds.validDuring} />
          </FixedSpaceRow>
        </TitleContainer>
        <H4>{i18n.financeBasics.fees.thresholds} </H4>
        <RowWithMargin spacing="XL">
          <FixedSpaceColumn>
            <Label>{i18n.financeBasics.fees.maxFee}</Label>
            <Indent data-qa="maxFee">
              {formatCents(feeThresholds.maxFee)} €
            </Indent>
          </FixedSpaceColumn>
          <FixedSpaceColumn>
            <Label>{i18n.financeBasics.fees.minFee}</Label>
            <Indent data-qa="minFee">
              {formatCents(feeThresholds.minFee)} €
            </Indent>
          </FixedSpaceColumn>
        </RowWithMargin>
        <TableWithMargin>
          <Thead>
            <Tr>
              <Th>{i18n.financeBasics.fees.familySize}</Th>
              <Th>{i18n.financeBasics.fees.minThreshold}</Th>
              <Th>{i18n.financeBasics.fees.multiplier}</Th>
              <Th>{i18n.financeBasics.fees.maxThreshold}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {familySizes.map((n) => (
              <Tr key={n}>
                <Td>{n}</Td>
                <Td data-qa={`minIncomeThreshold${n}`}>
                  {formatCents(feeThresholds[`minIncomeThreshold${n}`])} €
                </Td>
                <Td data-qa={`incomeMultiplier${n}`}>
                  {feeThresholds[`incomeMultiplier${n}`] * 100} %
                </Td>
                <Td data-qa={`maxIncomeThreshold${n}`}>
                  {formatCents(feeThresholds[`maxIncomeThreshold${n}`])} €
                </Td>
              </Tr>
            ))}
          </Tbody>
        </TableWithMargin>
        <ColumnWithMargin>
          <ExpandingInfo info={i18n.financeBasics.fees.thresholdIncreaseInfo}>
            <Label>{i18n.financeBasics.fees.thresholdIncrease}</Label>
          </ExpandingInfo>
          <Indent data-qa="incomeThresholdIncrease6Plus">
            {formatCents(feeThresholds.incomeThresholdIncrease6Plus)} €
          </Indent>
        </ColumnWithMargin>
        <H4>{i18n.financeBasics.fees.siblingDiscounts}</H4>
        <RowWithMargin spacing="XL">
          <FixedSpaceColumn>
            <Label>{i18n.financeBasics.fees.siblingDiscount2}</Label>
            <Indent data-qa="siblingDiscount2">
              {feeThresholds.siblingDiscount2 * 100} %
            </Indent>
          </FixedSpaceColumn>
          <FixedSpaceColumn>
            <Label>{i18n.financeBasics.fees.siblingDiscount2Plus}</Label>
            <Indent data-qa="siblingDiscount2Plus">
              {feeThresholds.siblingDiscount2Plus * 100} %
            </Indent>
          </FixedSpaceColumn>
        </RowWithMargin>

        <H4>{i18n.financeBasics.fees.temporaryFees}</H4>
        <RowWithMargin spacing="XL">
          <FixedSpaceColumn>
            <Label>{i18n.financeBasics.fees.temporaryFee}</Label>
            <Indent data-qa="temporaryFee">
              {formatCents(feeThresholds.temporaryFee)} €
            </Indent>
          </FixedSpaceColumn>
          <FixedSpaceColumn>
            <Label>{i18n.financeBasics.fees.temporaryFeePartDay}</Label>
            <Indent data-qa="temporaryFeePartDay">
              {formatCents(feeThresholds.temporaryFeePartDay)} €
            </Indent>
          </FixedSpaceColumn>
          <FixedSpaceColumn>
            <Label>{i18n.financeBasics.fees.temporaryFeeSibling}</Label>
            <Indent data-qa="temporaryFeeSibling">
              {formatCents(feeThresholds.temporaryFeeSibling)} €
            </Indent>
          </FixedSpaceColumn>
          <FixedSpaceColumn>
            <Label>{i18n.financeBasics.fees.temporaryFeeSiblingPartDay}</Label>
            <Indent data-qa="temporaryFeeSiblingPartDay">
              {formatCents(feeThresholds.temporaryFeeSiblingPartDay)} €
            </Indent>
          </FixedSpaceColumn>
        </RowWithMargin>
      </div>
      {showModal ? (
        <InfoModal
          icon={faQuestion}
          type="danger"
          title={i18n.financeBasics.fees.modals.editRetroactive.title}
          text={i18n.financeBasics.fees.modals.editRetroactive.text}
          reject={{
            action: () => setShowModal(false),
            label: i18n.financeBasics.fees.modals.editRetroactive.reject
          }}
          resolve={{
            action: () => {
              setShowModal(false)
              editThresholds(id, feeThresholds)
            },
            label: i18n.financeBasics.fees.modals.editRetroactive.resolve
          }}
        />
      ) : null}
    </>
  )
})
const TitleContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
`
const TableWithMargin = styled(Table)`
  margin: ${defaultMargins.m} 0;
`
const ColumnWithMargin = styled(FixedSpaceColumn)`
  margin: ${defaultMargins.s} 0;
`
const RowWithMargin = styled(FixedSpaceRow)`
  margin: ${defaultMargins.s} 0;
`
const Indent = styled.span`
  margin-left: ${defaultMargins.s};
`
