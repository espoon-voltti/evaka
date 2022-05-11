// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { orderBy } from 'lodash'
import React, { useCallback, useContext, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { formatDate } from 'lib-common/date'
import LocalDate from 'lib-common/local-date'
import { formatCents } from 'lib-common/money'
import { useApiState } from 'lib-common/utils/useRestApi'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faChild, faPlus } from 'lib-icons'

import {
  createRetroactiveValueDecisions,
  getPersonVoucherValueDecisions
} from '../../api/invoicing'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { renderResult } from '../async-rendering'

interface Props {
  id: string
  open: boolean
}

export default React.memo(function PersonVoucherValueDecisions({
  id,
  open
}: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const [voucherValueDecisions, reloadDecisions] = useApiState(
    () => getPersonVoucherValueDecisions(id),
    [id]
  )

  const openRetroactiveDecisionsModal = useCallback(
    () => toggleUiMode('create-retroactive-value-decisions'),
    [toggleUiMode]
  )

  return (
    <CollapsibleSection
      icon={faChild}
      title={i18n.personProfile.voucherValueDecisions.title}
      data-qa="person-voucher-value-decisions-collapsible"
      startCollapsed={!open}
    >
      {uiMode === 'create-retroactive-value-decisions' ? (
        <Modal
          headOfFamily={id}
          clear={clearUiMode}
          loadDecisions={reloadDecisions}
        />
      ) : null}
      <AddButtonRow
        text={i18n.personProfile.voucherValueDecisions.createRetroactive}
        onClick={openRetroactiveDecisionsModal}
        disabled={!!uiMode}
      />
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
            ).map((decision) => (
              <Tr
                key={`${decision.id}`}
                data-qa="table-voucher-value-decision-row"
              >
                <Td>
                  {decision.child.lastName} {decision.child.firstName}
                  <br />
                  <Link to={`/finance/value-decisions/${decision.id}`}>
                    {`${decision.validFrom.format()} - ${
                      decision.validTo?.format() ?? ''
                    }`}
                  </Link>
                </Td>
                <Td>{decision.decisionNumber}</Td>
                <Td>{formatCents(decision.voucherValue)}</Td>
                <Td>{formatCents(decision.finalCoPayment)}</Td>
                <Td>{formatDate(decision.created)}</Td>
                <Td data-qa="voucher-value-decision-sent-at">
                  {formatDate(decision.sentAt)}
                </Td>
                <Td>{i18n.valueDecision.status[decision.status]}</Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      ))}
    </CollapsibleSection>
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
  const [date, setDate] = useState<string>('')
  const dateIsValid = LocalDate.parseFiOrNull(date)

  const resolve = useCallback(() => {
    if (dateIsValid) {
      return createRetroactiveValueDecisions(
        headOfFamily,
        LocalDate.parseFiOrThrow(date)
      )
    }
    return
  }, [headOfFamily, date, dateIsValid])

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
      resolveDisabled={!dateIsValid}
      onSuccess={onSuccess}
      rejectAction={clear}
      rejectLabel={i18n.common.cancel}
    >
      <FixedSpaceRow justifyContent="center">
        <StyledLabel>{i18n.common.form.startDate}</StyledLabel>
        <DatePicker
          locale={lang}
          date={date}
          onChange={setDate}
          info={
            !dateIsValid
              ? {
                  status: 'warning',
                  text: i18n.validationError.dateRange
                }
              : undefined
          }
          hideErrorsBeforeTouched
          openAbove
        />
      </FixedSpaceRow>
    </AsyncFormModal>
  )
})
