// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { PersonContext } from 'employee-frontend/state/person'
import { wrapResult } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { formatCents } from 'lib-common/money'
import { useApiState } from 'lib-common/utils/useRestApi'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faPlus } from 'lib-icons'

import {
  generateRetroactiveVoucherValueDecisions,
  getHeadOfFamilyVoucherValueDecisions
} from '../../generated/api-clients/invoicing'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { renderResult } from '../async-rendering'

const getHeadOfFamilyVoucherValueDecisionsResult = wrapResult(
  getHeadOfFamilyVoucherValueDecisions
)
const generateRetroactiveVoucherValueDecisionsResult = wrapResult(
  generateRetroactiveVoucherValueDecisions
)

interface Props {
  id: string
  open: boolean
}

export default React.memo(function PersonVoucherValueDecisions({
  id,
  open: startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const { permittedActions } = useContext(PersonContext)
  const [open, setOpen] = useState(startOpen)
  const [voucherValueDecisions, reloadDecisions] = useApiState(
    () => getHeadOfFamilyVoucherValueDecisionsResult({ headOfFamilyId: id }),
    [id]
  )

  const openRetroactiveDecisionsModal = useCallback(
    () => toggleUiMode('create-retroactive-value-decisions'),
    [toggleUiMode]
  )

  return (
    <CollapsibleContentArea
      title={<H2>{i18n.personProfile.voucherValueDecisions.title}</H2>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-qa="person-voucher-value-decisions-collapsible"
    >
      {uiMode === 'create-retroactive-value-decisions' ? (
        <Modal
          headOfFamily={id}
          clear={clearUiMode}
          loadDecisions={reloadDecisions}
        />
      ) : null}
      {permittedActions.has('GENERATE_RETROACTIVE_VOUCHER_VALUE_DECISIONS') && (
        <AddButtonRow
          data-qa="create-retroactive-value-decisions"
          text={i18n.personProfile.voucherValueDecisions.createRetroactive}
          onClick={openRetroactiveDecisionsModal}
          disabled={!!uiMode}
        />
      )}
      {renderResult(voucherValueDecisions, (voucherValueDecisions) => (
        <Table data-qa="table-of-voucher-value-decisions">
          <Thead>
            <Tr>
              <Th>{i18n.valueDecisions.table.validity}</Th>
              <Th>{i18n.valueDecisions.table.number}</Th>
              <Th>{i18n.valueDecisions.table.totalValue}</Th>
              <Th>{i18n.valueDecisions.table.totalCoPayment}</Th>
              <Th>{i18n.valueDecisions.table.createdAt}</Th>
              <Th>{i18n.valueDecisions.table.sentAt}</Th>
              <Th>{i18n.valueDecisions.table.status}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {orderBy(
              voucherValueDecisions,
              ['sentAt', 'validFrom'],
              ['desc']
            ).map((decision) => {
              const formattedRange = `${decision.validFrom.format()} - ${
                decision.validTo?.format() ?? ''
              }`

              return (
                <Tr
                  key={decision.id}
                  data-qa="table-voucher-value-decision-row"
                >
                  <Td>
                    {decision.child.lastName} {decision.child.firstName}
                    <br />
                    {decision.annullingDecision ? (
                      <span>
                        {i18n.valueDecisions.table.annullingDecision}{' '}
                        {formattedRange}
                      </span>
                    ) : (
                      <Link to={`/finance/value-decisions/${decision.id}`}>
                        {formattedRange}
                      </Link>
                    )}
                  </Td>
                  <Td>{decision.decisionNumber}</Td>
                  <Td>{formatCents(decision.voucherValue)}</Td>
                  <Td>{formatCents(decision.finalCoPayment)}</Td>
                  <Td>{decision.created.toLocalDate().format()}</Td>
                  <Td data-qa="voucher-value-decision-sent-at">
                    {decision.sentAt?.toLocalDate().format() ?? ''}
                  </Td>
                  <Td>{i18n.valueDecision.status[decision.status]}</Td>
                </Tr>
              )
            })}
          </Tbody>
        </Table>
      ))}
    </CollapsibleContentArea>
  )
})

const StyledLabel = styled(Label)`
  margin-top: ${defaultMargins.xs};
`

const Modal = React.memo(function Modal({
  headOfFamily,
  clear,
  loadDecisions
}: {
  headOfFamily: string
  clear: () => void
  loadDecisions: () => void
}) {
  const { i18n, lang } = useTranslation()
  const [date, setDate] = useState<LocalDate | null>(null)

  const resolve = useCallback(() => {
    if (date) {
      return generateRetroactiveVoucherValueDecisionsResult({
        id: headOfFamily,
        body: { from: date }
      })
    }
    return
  }, [headOfFamily, date])

  const onSuccess = useCallback(() => {
    clear()
    loadDecisions()
  }, [clear, loadDecisions])

  return (
    <AsyncFormModal
      icon={faPlus}
      type="info"
      title={i18n.personProfile.voucherValueDecisions.createRetroactive}
      resolveAction={resolve}
      resolveLabel={i18n.common.create}
      resolveDisabled={!date}
      onSuccess={onSuccess}
      rejectAction={clear}
      rejectLabel={i18n.common.cancel}
    >
      <FixedSpaceRow justifyContent="center">
        <StyledLabel>{i18n.common.form.startDate}</StyledLabel>
        <DatePicker
          data-qa="retroactive-value-decisions-from-date"
          locale={lang}
          date={date}
          onChange={setDate}
          hideErrorsBeforeTouched
          openAbove
        />
      </FixedSpaceRow>
    </AsyncFormModal>
  )
})
