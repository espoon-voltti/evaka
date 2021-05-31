import React, { useCallback, useEffect, useState } from 'react'
import styled from 'styled-components'
import { Loading, Result } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { H2, H3 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Th, Thead, Td, Tr } from 'lib-components/layout/Table'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import Spinner from 'lib-components/atoms/state/Spinner'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { getFeeThresholds } from '../../api/finance-basics'
import { familySizes, FeeThresholds } from '../../types/finance-basics'
import { Translations, useTranslation } from '../../state/i18n'
import { formatCents } from '../../utils/money'
import FeeThresholdsItemEditor from './FeeThresholdsItemEditor'

export default React.memo(function FeesSection() {
  const { i18n } = useTranslation()

  const [open, setOpen] = useState(true)
  const toggleOpen = useCallback(() => setOpen((isOpen) => !isOpen), [setOpen])

  const [data, setData] = useState<Result<FeeThresholds[]>>(Loading.of())
  const loadData = useRestApi(getFeeThresholds, setData)
  useEffect(() => {
    void loadData()
  }, [loadData])

  const [editorState, setEditorState] = useState<EditorState>({})
  const closeEditor = useCallback(() => setEditorState({}), [setEditorState])

  return (
    <CollapsibleContentArea
      opaque
      title={<H2 noMargin>{i18n.financeBasics.fees.title}</H2>}
      open={open}
      toggleOpen={toggleOpen}
    >
      <AddButtonRow
        onClick={() =>
          setEditorState({
            editing: 'new',
            form: emptyForm(
              data
                .map((ps) => ps[0]?.validDuring?.end ?? undefined)
                .getOrElse(undefined)
            )
          })
        }
        text={i18n.financeBasics.fees.add}
        disabled={'editing' in editorState}
      />
      {editorState.editing === 'new' ? (
        <FeeThresholdsItemEditor
          i18n={i18n}
          initialState={editorState.form}
          close={closeEditor}
          reloadData={loadData}
        />
      ) : null}
      {data.mapAll({
        loading() {
          return <Spinner />
        },
        failure() {
          return <ErrorSegment title={i18n.common.error.unknown} />
        },
        success(feeThresholdsList) {
          return (
            <>
              {feeThresholdsList.map((feeThresholds) => (
                <FeeThresholdsItem
                  key={feeThresholds.id}
                  i18n={i18n}
                  feeThresholds={feeThresholds}
                />
              ))}
            </>
          )
        }
      })}
    </CollapsibleContentArea>
  )
})

const FeeThresholdsItem = React.memo(function FeeThresholdsItem({
  i18n,
  feeThresholds
}: {
  i18n: Translations
  feeThresholds: FeeThresholds
}) {
  return (
    <>
      <div className="separator large" />
      <H3 fitted>
        {i18n.financeBasics.fees.validDuring}{' '}
        {feeThresholds.validDuring.format()}
      </H3>
      <RowWithMargin spacing="XL">
        <FixedSpaceColumn>
          <Label>{i18n.financeBasics.fees.maxFee}</Label>
          <Indent>{formatCents(feeThresholds.maxFee)} €</Indent>
        </FixedSpaceColumn>
        <FixedSpaceColumn>
          <Label>{i18n.financeBasics.fees.minFee}</Label>
          <Indent>{formatCents(feeThresholds.minFee)} €</Indent>
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
          {familySizes.map((n) => {
            return (
              <Tr key={n}>
                <Td>{n}</Td>
                <Td>
                  {formatCents(
                    feeThresholds[
                      `minIncomeThreshold${n}` as `minIncomeThreshold${typeof n}`
                    ]
                  )}{' '}
                  €
                </Td>
                <Td>
                  {feeThresholds[
                    `incomeMultiplier${n}` as `incomeMultiplier${typeof n}`
                  ] * 100}{' '}
                  %
                </Td>
                <Td>
                  {formatCents(
                    feeThresholds[
                      `maxIncomeThreshold${n}` as `maxIncomeThreshold${typeof n}`
                    ]
                  )}{' '}
                  €
                </Td>
              </Tr>
            )
          })}
        </Tbody>
      </TableWithMargin>
      <ColumnWithMargin>
        <ExpandingInfo
          info={i18n.financeBasics.fees.thresholdIncreaseInfo}
          ariaLabel={i18n.common.openExpandingInfo}
        >
          <Label>{i18n.financeBasics.fees.thresholdIncrease}</Label>
        </ExpandingInfo>
        <Indent>
          {formatCents(feeThresholds.incomeThresholdIncrease6Plus)} €
        </Indent>
      </ColumnWithMargin>
    </>
  )
})

const TableWithMargin = styled(Table)`
  margin: ${defaultMargins.m} 0;
`

const ColumnWithMargin = styled(FixedSpaceColumn)`
  margin: ${defaultMargins.s} 0;
`

const RowWithMargin = styled(FixedSpaceRow)`
  margin: ${defaultMargins.s} 0;
`

const Label = styled.span`
  font-weight: 600;
`

const Indent = styled.span`
  margin-left: ${defaultMargins.s};
`

type EditorState =
  | Record<string, never>
  | {
      editing: string
      form: FormState
    }

export type FormState = {
  [k in keyof Omit<
    FeeThresholds,
    'id' | 'validDuring' | 'siblingDiscount2' | 'siblingDiscount2Plus'
  >]: string
} & {
  validFrom: string
  validTo: string
}

const emptyForm = (latestEndDate?: LocalDate): FormState => ({
  validFrom: latestEndDate?.addDays(1).format() ?? '',
  validTo: '',
  maxFee: '',
  minFee: '',
  minIncomeThreshold2: '',
  minIncomeThreshold3: '',
  minIncomeThreshold4: '',
  minIncomeThreshold5: '',
  minIncomeThreshold6: '',
  maxIncomeThreshold2: '',
  maxIncomeThreshold3: '',
  maxIncomeThreshold4: '',
  maxIncomeThreshold5: '',
  maxIncomeThreshold6: '',
  incomeMultiplier2: '',
  incomeMultiplier3: '',
  incomeMultiplier4: '',
  incomeMultiplier5: '',
  incomeMultiplier6: '',
  incomeThresholdIncrease6Plus: ''
})
