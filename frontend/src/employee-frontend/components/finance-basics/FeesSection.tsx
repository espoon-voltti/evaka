// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'

import { isLoading, wrapResult } from 'lib-common/api'
import { FeeThresholds } from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { formatCents } from 'lib-common/money'
import { useApiState } from 'lib-common/utils/useRestApi'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'

import { getFeeThresholds } from '../../generated/api-clients/invoicing'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import FeeThresholdsEditor from './FeeThresholdsEditor'
import { FeeThresholdsItem } from './FeeThresholdsItem'

const getFeeThresholdsResult = wrapResult(getFeeThresholds)

export default React.memo(function FeesSection() {
  const { i18n } = useTranslation()

  const [open, setOpen] = useState(true)
  const toggleOpen = useCallback(() => setOpen((isOpen) => !isOpen), [setOpen])

  const [data, loadData] = useApiState(() => getFeeThresholdsResult(), [])

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
          validFrom: lastThresholdsEndDate ?? null,
          validTo: null
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
      data-qa="fees-section"
      data-isloading={isLoading(data)}
    >
      <AddButtonRow
        onClick={createNewThresholds}
        text={i18n.financeBasics.fees.add}
        disabled={'editing' in editorState}
        data-qa="create-new-fee-thresholds"
      />
      {editorState.editing === 'new' ? (
        <FeeThresholdsEditor
          i18n={i18n}
          id={undefined}
          initialState={editorState.form}
          close={closeEditor}
          reloadData={loadData}
          existingThresholds={data}
        />
      ) : null}
      {renderResult(data, (feeThresholdsList) => (
        <>
          {feeThresholdsList.map((feeThresholds, index) =>
            editorState.editing === feeThresholds.id ? (
              <FeeThresholdsEditor
                key={feeThresholds.id}
                i18n={i18n}
                id={feeThresholds.id}
                initialState={editorState.form}
                close={closeEditor}
                reloadData={loadData}
                existingThresholds={data}
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
                data-qa={`fee-thresholds-item-${index}`}
              />
            )
          )}
        </>
      ))}
    </CollapsibleContentArea>
  )
})

type EditorState =
  | Record<string, never>
  | {
      editing: string
      form: FormState
    }

export type FormState = {
  [k in keyof Omit<FeeThresholds, 'validDuring'>]: string
} & {
  validFrom: LocalDate | null
  validTo: LocalDate | null
}

const emptyForm = (latestEndDate?: LocalDate): FormState => ({
  validFrom: latestEndDate?.addDays(1) ?? null,
  validTo: null,
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
  siblingDiscount2Plus: '',
  temporaryFee: '',
  temporaryFeePartDay: '',
  temporaryFeeSibling: '',
  temporaryFeeSiblingPartDay: ''
})

const formatMulti = (multi: number) =>
  (multi * 100).toString().replace('.', ',')

const copyForm = (feeThresholds: FeeThresholds): FormState => ({
  validFrom: feeThresholds.validDuring.start,
  validTo: feeThresholds.validDuring.end,
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
  siblingDiscount2Plus: formatMulti(feeThresholds.siblingDiscount2Plus),
  temporaryFee: formatCents(feeThresholds.temporaryFee),
  temporaryFeePartDay: formatCents(feeThresholds.temporaryFeePartDay),
  temporaryFeeSibling: formatCents(feeThresholds.temporaryFeeSibling),
  temporaryFeeSiblingPartDay: formatCents(
    feeThresholds.temporaryFeeSiblingPartDay
  )
})
