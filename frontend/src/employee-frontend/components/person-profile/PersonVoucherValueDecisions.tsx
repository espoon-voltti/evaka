// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext, useState } from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import type { PersonId } from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'
import { formatCents } from 'lib-common/money'
import { cancelMutation, useQueryResult } from 'lib-common/query'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faPlus } from 'lib-icons'

import { PersonContext } from '../../components/person-profile/state'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { renderResult } from '../async-rendering'

import {
  generateRetroactiveVoucherValueDecisionsMutation,
  headOfFamilyVoucherValueDecisionsQuery
} from './queries'

interface Props {
  id: PersonId
}

export default React.memo(function PersonVoucherValueDecisions({ id }: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const { permittedActions } = useContext(PersonContext)
  const voucherValueDecisions = useQueryResult(
    headOfFamilyVoucherValueDecisionsQuery({ headOfFamilyId: id })
  )

  const openRetroactiveDecisionsModal = useCallback(
    () => toggleUiMode('create-retroactive-value-decisions'),
    [toggleUiMode]
  )

  return (
    <>
      {uiMode === 'create-retroactive-value-decisions' ? (
        <Modal headOfFamily={id} clear={clearUiMode} />
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
              ['validFrom', 'sentAt'],
              ['desc', 'desc']
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
    </>
  )
})

const StyledLabel = styled(Label)`
  margin-top: ${defaultMargins.xs};
`

const Modal = React.memo(function Modal({
  headOfFamily,
  clear
}: {
  headOfFamily: PersonId
  clear: () => void
}) {
  const { i18n, lang } = useTranslation()
  const [date, setDate] = useState<LocalDate | null>(null)

  return (
    <MutateFormModal
      icon={faPlus}
      type="info"
      title={i18n.personProfile.voucherValueDecisions.createRetroactive}
      resolveMutation={generateRetroactiveVoucherValueDecisionsMutation}
      resolveAction={() =>
        date !== null
          ? {
              id: headOfFamily,
              body: { from: date }
            }
          : cancelMutation
      }
      resolveLabel={i18n.common.create}
      resolveDisabled={!date}
      onSuccess={clear}
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
        />
      </FixedSpaceRow>
    </MutateFormModal>
  )
})
