{
  /*
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import { faCopy, faPen } from 'lib-icons'
import { Loading, Result } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { H2, H3, H4 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Th, Thead, Td, Tr } from 'lib-components/layout/Table'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import Spinner from 'lib-components/atoms/state/Spinner'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { getFeeThresholds } from '../../api/finance-basics'
import {
  familySizes,
  FeeThresholds,
  FeeThresholdsWithId
} from '../../types/finance-basics'
import { Translations, useTranslation } from '../../state/i18n'
import { formatCents } from '../../utils/money'
import StatusLabel from '../common/StatusLabel'
import FeeThresholdsItemEditor from './FeeThresholdsItemEditor'

export default React.memo(function FeesSection() {
  const { i18n } = useTranslation()

  const [open, setOpen] = useState(true)
  const toggleOpen = useCallback(() => setOpen((isOpen) => !isOpen), [setOpen])

  const [data, setData] = useState<Result<FeeThresholdsWithId[]>>(Loading.of())
  const loadData = useRestApi(getFeeThresholds, setData)
  useEffect(() => {
    void loadData()
  }, [loadData])

  const [editorState, setEditorState] = useState<EditorState>({})
  const closeEditor = useCallback(() => setEditorState({}), [setEditorState])

  const lastThresholdsEndDate = useMemo(
    () =>
      data
        .map((ps) => ps[0]?.thresholds.validDuring.end ?? undefined)
        .getOrElse(undefined),
    [data]
  )

  const createNewThresholds = useCallback(
    () =>
      setEditorState({
        editing: 'new',
        form: emptyForm(lastThresholdsEndDate)
      }),
    [setEditorState, lastThresholdsEndDate]
  )

  const copyThresholds = useCallback(
    (item: FeeThresholds) =>
      setEditorState({
        editing: 'new',
        form: {
          ...copyForm(item),
          validFrom: lastThresholdsEndDate?.format() ?? '',
          validTo: ''
        }
      }),
    [setEditorState, lastThresholdsEndDate]
  )

  const editThresholds = useCallback(
    (id: string, item: FeeThresholds) =>
      setEditorState({ editing: id, form: copyForm(item) }),
    [setEditorState]
  )

  return (
    <CollapsibleContentArea
      opaque
      title={<H2 noMargin>{i18n.financeBasics.fees.title}</H2>}
      open={open}
      toggleOpen={toggleOpen}
    >
      <AddButtonRow
        onClick={createNewThresholds}
        text={i18n.financeBasics.fees.add}
        disabled={'editing' in editorState}
      />
      {editorState.editing === 'new' ? (
        <FeeThresholdsItemEditor
          i18n={i18n}
          id={undefined}
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
              {feeThresholdsList.map((feeThresholds) =>
                editorState.editing === feeThresholds.id ? (
                  <FeeThresholdsItemEditor
                    key={feeThresholds.id}
                    i18n={i18n}
                    id={feeThresholds.id}
                    initialState={editorState.form}
                    close={closeEditor}
                    reloadData={loadData}
                  />
                ) : (
                  <FeeThresholdsItem
                    key={feeThresholds.id}
                    i18n={i18n}
                    id={feeThresholds.id}
                    feeThresholds={feeThresholds.thresholds}
                    copyThresholds={copyThresholds}
                    editThresholds={editThresholds}
                    editing={!!editorState.editing}
                  />
                )
              )}
            </>
          )
        }
      })}
    </CollapsibleContentArea>
  )
})

const FeeThresholdsItem = React.memo(function FeeThresholdsItem({
  i18n,
  id,
  feeThresholds,
  copyThresholds,
  editThresholds,
  editing
}: {
  i18n: Translations
  id: string
  feeThresholds: FeeThresholds
  copyThresholds: (feeThresholds: FeeThresholds) => void
  editThresholds: (id: string, feeThresholds: FeeThresholds) => void
  editing: boolean
}) {
  return (
    <>
      <div className="separator large" />
      <TitleContainer>
        <H3>
          {i18n.financeBasics.fees.validDuring}{' '}
          {feeThresholds.validDuring.format()}
        </H3>
        <FixedSpaceRow>
          <IconButton
            icon={faCopy}
            onClick={() => copyThresholds(feeThresholds)}
            disabled={editing}
          />
          <IconButton
            icon={faPen}
            onClick={() => editThresholds(id, feeThresholds)}
            disabled={editing}
          />
          <StatusLabel dateRange={feeThresholds.validDuring} />
        </FixedSpaceRow>
      </TitleContainer>
      <H4>{i18n.financeBasics.fees.thresholds} </H4>
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
      <H4>{i18n.financeBasics.fees.siblingDiscounts}</H4>
      <RowWithMargin spacing="XL">
        <FixedSpaceColumn>
          <Label>{i18n.financeBasics.fees.siblingDiscount2}</Label>
          <Indent>{feeThresholds.siblingDiscount2 * 100} %</Indent>
        </FixedSpaceColumn>
        <FixedSpaceColumn>
          <Label>{i18n.financeBasics.fees.siblingDiscount2Plus}</Label>
          <Indent>{feeThresholds.siblingDiscount2Plus * 100} %</Indent>
        </FixedSpaceColumn>
      </RowWithMargin>
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
  [k in keyof Omit<FeeThresholds, 'validDuring'>]: string
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
  incomeThresholdIncrease6Plus: '',
  siblingDiscount2: '',
  siblingDiscount2Plus: ''
})

const formatMulti = (multi: number) =>
  (multi * 100).toString().replace('.', ',')

const copyForm = (feeThresholds: FeeThresholds): FormState => ({
  validFrom: feeThresholds.validDuring.start.format() ?? '',
  validTo: feeThresholds.validDuring.end?.format() ?? '',
  maxFee: formatCents(feeThresholds.maxFee) ?? '',
  minFee: formatCents(feeThresholds.minFee) ?? '',
  minIncomeThreshold2: formatCents(feeThresholds.minIncomeThreshold2) ?? '',
  minIncomeThreshold3: formatCents(feeThresholds.minIncomeThreshold3) ?? '',
  minIncomeThreshold4: formatCents(feeThresholds.minIncomeThreshold4) ?? '',
  minIncomeThreshold5: formatCents(feeThresholds.minIncomeThreshold5) ?? '',
  minIncomeThreshold6: formatCents(feeThresholds.minIncomeThreshold6) ?? '',
  maxIncomeThreshold2: formatCents(feeThresholds.maxIncomeThreshold2) ?? '',
  maxIncomeThreshold3: formatCents(feeThresholds.maxIncomeThreshold3) ?? '',
  maxIncomeThreshold4: formatCents(feeThresholds.maxIncomeThreshold4) ?? '',
  maxIncomeThreshold5: formatCents(feeThresholds.maxIncomeThreshold5) ?? '',
  maxIncomeThreshold6: formatCents(feeThresholds.maxIncomeThreshold6) ?? '',
  incomeMultiplier2: formatMulti(feeThresholds.incomeMultiplier2),
  incomeMultiplier3: formatMulti(feeThresholds.incomeMultiplier3),
  incomeMultiplier4: formatMulti(feeThresholds.incomeMultiplier4),
  incomeMultiplier5: formatMulti(feeThresholds.incomeMultiplier5),
  incomeMultiplier6: formatMulti(feeThresholds.incomeMultiplier6),
  incomeThresholdIncrease6Plus:
    formatCents(feeThresholds.incomeThresholdIncrease6Plus) ?? '',
  siblingDiscount2: formatMulti(feeThresholds.siblingDiscount2),
  siblingDiscount2Plus: formatMulti(feeThresholds.siblingDiscount2Plus)
})
