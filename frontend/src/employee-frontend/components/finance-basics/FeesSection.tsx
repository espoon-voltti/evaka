import React, { useCallback, useEffect, useState } from 'react'
import styled from 'styled-components'
import { Loading, Result } from 'lib-common/api'
import { H2, H3 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Th, Thead, Td, Tr } from 'lib-components/layout/Table'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import Spinner from 'lib-components/atoms/state/Spinner'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { getPricing } from '../../api/finance-basics'
import { Pricing } from '../../types/finance-basics'
import { Translations, useTranslation } from '../../state/i18n'
import { formatCents } from '../../utils/money'

export default React.memo(function FeesSection() {
  const { i18n } = useTranslation()

  const [open, setOpen] = useState(true)
  const toggleOpen = useCallback(() => setOpen((isOpen) => !isOpen), [setOpen])

  const [data, setData] = useState<Result<Pricing[]>>(Loading.of())
  useEffect(() => {
    void getPricing().then(setData)
  }, [setData])

  return (
    <CollapsibleContentArea
      opaque
      title={<H2 noMargin>{i18n.financeBasics.fees.title}</H2>}
      open={open}
      toggleOpen={toggleOpen}
    >
      {data.mapAll({
        loading() {
          return <Spinner />
        },
        failure() {
          return <ErrorSegment title={i18n.common.error.unknown} />
        },
        success(pricings) {
          return (
            <>
              {pricings.map((pricing) => (
                <PricingItem
                  key={pricing.validDuring.start.formatIso()}
                  i18n={i18n}
                  pricing={pricing}
                />
              ))}
            </>
          )
        }
      })}
    </CollapsibleContentArea>
  )
})

const PricingItem = React.memo(function PricingItem({
  i18n,
  pricing
}: {
  i18n: Translations
  pricing: Pricing
}) {
  return (
    <>
      <div className="separator large" />
      <H3 noMargin>
        {i18n.financeBasics.fees.validDuring} {pricing.validDuring.format()}
      </H3>
      <TableWithMargin>
        <Thead>
          <Tr>
            <Th>{i18n.financeBasics.fees.familySize}</Th>
            <Th>{i18n.financeBasics.fees.minThreshold}</Th>
            <Th>{i18n.financeBasics.fees.maxThreshold}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {(['2', '3', '4', '5', '6'] as const).map((n) => {
            const prop = `minThreshold${n}` as `minThreshold${typeof n}`
            return (
              <Tr key={n}>
                <Td>{n}</Td>
                <Td>{formatCents(pricing[prop])} €</Td>
                <Td>
                  {formatCents(pricing[prop] + pricing.maxThresholdDifference)}{' '}
                  €
                </Td>
              </Tr>
            )
          })}
        </Tbody>
      </TableWithMargin>
      <LabelValuePair>
        <ExpandingInfo
          info={i18n.financeBasics.fees.thresholdIncreaseInfo}
          ariaLabel={i18n.common.openExpandingInfo}
        >
          <Label>{i18n.financeBasics.fees.thresholdIncrease}</Label>
        </ExpandingInfo>
        <Indent>{formatCents(pricing.thresholdIncrease6Plus)} €</Indent>
      </LabelValuePair>
      <LabelValuePair>
        <Label>{i18n.financeBasics.fees.multiplier}</Label>
        <Indent>{pricing.multiplier * 100} %</Indent>
      </LabelValuePair>
      <LabelValuePair>
        <Label>{i18n.financeBasics.fees.maxFee}</Label>
        <Indent>
          {formatCents(
            Math.round(
              (pricing.maxThresholdDifference * pricing.multiplier) / 100
            ) * 100
          )}{' '}
          €
        </Indent>
      </LabelValuePair>
    </>
  )
})

const TableWithMargin = styled(Table)`
  margin: ${defaultMargins.m} 0;
`

const LabelValuePair = styled(FixedSpaceColumn)`
  margin: ${defaultMargins.s} 0;
`

const Label = styled.span`
  font-weight: 600;
`

const Indent = styled.span`
  margin-left: ${defaultMargins.s};
`
