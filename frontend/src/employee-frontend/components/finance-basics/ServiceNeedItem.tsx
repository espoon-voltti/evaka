// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faTrash } from 'Icons'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { useBoolean } from 'lib-common/form/hooks'
import {
  ServiceNeedOptionVoucherValueRange,
  ServiceNeedOptionVoucherValueRangeWithId
} from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { H4 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { getStatusLabelByDateRange } from '../../utils/date'
import StatusLabel from '../common/StatusLabel'

import VoucherValueEditor from './VoucherValueEditor'
import { deleteVoucherValueMutation } from './queries'

export type ServiceNeedItemProps = {
  serviceNeedId: UUID
  serviceNeedName: string
  serviceNeedValidityStart: LocalDate
  serviceNeedValidityEnd: LocalDate | null
  voucherValuesList: ServiceNeedOptionVoucherValueRangeWithId[]
  'data-qa'?: string
}

const JoinedPlacement = styled.div`
  display: inline-flex;
  align-items: baseline;
  gap: ${defaultMargins.s};
`

export default React.memo(function ServiceNeedItem({
  serviceNeedId,
  serviceNeedName,
  serviceNeedValidityStart,
  serviceNeedValidityEnd,
  voucherValuesList,
  'data-qa': dataQa
}: ServiceNeedItemProps) {
  const { i18n } = useTranslation()
  const [open, useOpen] = useBoolean(false)

  const [editorState, setEditorState] = useState<EditorState>({})

  const closeEditor = useCallback(() => setEditorState({}), [setEditorState])

  const previousCoeffient = useMemo(
    () =>
      voucherValuesList.length > 0
        ? voucherValuesList[0].voucherValues.coefficient
        : 1,
    [voucherValuesList]
  )

  const previousCoeffientUnder3y = useMemo(
    () =>
      voucherValuesList.length > 0
        ? voucherValuesList[0].voucherValues.coefficientUnder3y
        : 1,
    [voucherValuesList]
  )

  const createNewVoucherValue = useCallback(
    () =>
      setEditorState({
        editing: 'new',
        form: emptyForm(
          serviceNeedId,
          previousCoeffient,
          previousCoeffientUnder3y
        )
      }),
    [setEditorState, serviceNeedId, previousCoeffient, previousCoeffientUnder3y]
  )

  return (
    <>
      <CollapsibleContentArea
        opaque
        title={
          <JoinedPlacement>
            <H4>{serviceNeedName}</H4>
            <StatusLabel
              status={getStatusLabelByDateRange({
                startDate: serviceNeedValidityStart,
                endDate: serviceNeedValidityEnd
              })}
            />
          </JoinedPlacement>
        }
        open={open}
        toggleOpen={useOpen.toggle}
        paddingHorizontal="0"
        paddingVertical="0"
        data-qa={dataQa}
      >
        <H4>{i18n.financeBasics.serviceNeeds.voucherValues}</H4>
        <AddButtonRow
          onClick={createNewVoucherValue}
          text={i18n.financeBasics.serviceNeeds.add}
          disabled={'editing' in editorState}
          data-qa="create-new-voucher-values"
        />
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
            {editorState.editing == 'new' ? (
              <VoucherValueEditor
                i18n={i18n}
                initialState={editorState.form}
                close={closeEditor}
              />
            ) : null}
            {voucherValuesList
              .sort((a, b) =>
                b.voucherValues.range.start.compareTo(
                  a.voucherValues.range.start
                )
              )
              .map((voucherValue, i) => (
                <Tr key={i} data-qa={`voucher-value-row-${i}`}>
                  <Td data-qa="validity">
                    {voucherValue.voucherValues.range.format('dd.MM.yyyy')}
                  </Td>
                  <Td data-qa="base-value">
                    {(voucherValue.voucherValues.baseValue / 100).toFixed(2)}
                  </Td>
                  <Td data-qa="coefficient">
                    {voucherValue.voucherValues.coefficient}
                  </Td>
                  <Td data-qa="value">
                    {(voucherValue.voucherValues.value / 100).toFixed(2)}
                  </Td>
                  <Td data-qa="base-value-under-3y">
                    {(
                      voucherValue.voucherValues.baseValueUnder3y / 100
                    ).toFixed(2)}
                  </Td>
                  <Td data-qa="coefficient-under-3y">
                    {voucherValue.voucherValues.coefficientUnder3y}
                  </Td>
                  <Td data-qa="value-under-3y">
                    {(voucherValue.voucherValues.valueUnder3y / 100).toFixed(2)}
                  </Td>
                  <Td data-qa="delete-btn">
                    {voucherValue.voucherValues.range.end == null && (
                      <ConfirmedMutation
                        buttonStyle="INLINE"
                        data-qa="btn-delete"
                        icon={faTrash}
                        buttonText=""
                        mutation={deleteVoucherValueMutation}
                        onClick={() => ({ id: voucherValue.id })}
                        confirmationTitle={
                          i18n.financeBasics.modals.deleteVoucherValue.title
                        }
                      />
                    )}
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

type EditorState =
  | Record<string, never>
  | {
      editing: string
      form: FormState
    }

export type FormState = {
  [k in keyof Omit<ServiceNeedOptionVoucherValueRange, 'range'>]: string
} & {
  validFrom: LocalDate | null
  validTo: LocalDate | null
}

const emptyForm = (
  serviceNeedOptionId: UUID,
  coefficient: number,
  coefficientUnder3y: number
): FormState => ({
  serviceNeedOptionId: serviceNeedOptionId,
  validFrom: null,
  validTo: null,
  baseValue: '0.00',
  coefficient: coefficient.toString(),
  value: '0.00',
  baseValueUnder3y: '0.00',
  coefficientUnder3y: coefficientUnder3y.toString(),
  valueUnder3y: '0.00'
})
