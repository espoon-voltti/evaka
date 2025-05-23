// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'wouter'

import type { FeeDecision } from 'lib-common/generated/api-types/invoicing'
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

import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { renderResult } from '../async-rendering'

import { DateTd, StatusTd } from './common'
import {
  generateRetroactiveFeeDecisionsMutation,
  headOfFamilyFeeDecisionsQuery
} from './queries'
import { PersonContext } from './state'

interface Props {
  id: PersonId
}

export default React.memo(function PersonFeeDecisions({ id }: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const { permittedActions } = useContext(PersonContext)
  const feeDecisions = useQueryResult(headOfFamilyFeeDecisionsQuery({ id }))

  const openRetroactiveDecisionsModal = useCallback(
    () => toggleUiMode('create-retroactive-fee-decision'),
    [toggleUiMode]
  )

  return (
    <>
      {uiMode === 'create-retroactive-fee-decision' ? (
        <Modal headOfFamily={id} clear={clearUiMode} />
      ) : null}
      {permittedActions.has('GENERATE_RETROACTIVE_FEE_DECISIONS') && (
        <AddButtonRow
          text={i18n.personProfile.feeDecisions.createRetroactive}
          onClick={openRetroactiveDecisionsModal}
          disabled={!!uiMode}
          data-qa="create-retroactive-fee-decision-button"
        />
      )}
      {renderResult(feeDecisions, (feeDecisions) => (
        <Table data-qa="table-of-fee-decisions">
          <Thead>
            <Tr>
              <Th>{i18n.feeDecisions.table.validity}</Th>
              <Th>{i18n.feeDecisions.table.price}</Th>
              <Th>{i18n.feeDecisions.table.number}</Th>
              <Th>{i18n.feeDecisions.table.createdAt}</Th>
              <Th>{i18n.feeDecisions.table.sentAt}</Th>
              <Th>{i18n.feeDecisions.table.status}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {orderBy(
              feeDecisions,
              ['validFrom', 'sentAt'],
              ['desc', 'desc']
            ).map((feeDecision: FeeDecision) => (
              <Tr key={feeDecision.id} data-qa="table-fee-decision-row">
                <Td>
                  <Link to={`/finance/fee-decisions/${feeDecision.id}`}>
                    Maksupäätös {feeDecision.validDuring.format()}
                  </Link>
                </Td>
                <Td>{formatCents(feeDecision.totalFee)}</Td>
                <Td>{feeDecision.decisionNumber}</Td>
                <DateTd>{feeDecision.created.toLocalDate().format()}</DateTd>
                <DateTd data-qa="fee-decision-sent-at">
                  {feeDecision.sentAt?.toLocalDate().format() ?? ''}
                </DateTd>
                <StatusTd>
                  {i18n.feeDecision.status[feeDecision.status]}
                </StatusTd>
              </Tr>
            ))}
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
      title={i18n.personProfile.feeDecisions.createRetroactive}
      resolveMutation={generateRetroactiveFeeDecisionsMutation}
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
          locale={lang}
          date={date}
          onChange={setDate}
          hideErrorsBeforeTouched
          data-qa="retroactive-fee-decision-start-date"
        />
      </FixedSpaceRow>
    </MutateFormModal>
  )
})
