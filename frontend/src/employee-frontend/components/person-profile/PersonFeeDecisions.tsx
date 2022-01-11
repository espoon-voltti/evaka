// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import _ from 'lodash'
import React, { useCallback, useContext, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import { formatDate } from 'lib-common/date'
import { FeeDecision } from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { formatCents } from 'lib-common/money'
import { UUID } from 'lib-common/types'
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
  getPersonFeeDecisions,
  createRetroactiveFeeDecisions
} from '../../api/invoicing'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { DateTd, StatusTd } from '../PersonProfile'
import { renderResult } from '../async-rendering'

interface Props {
  id: UUID
  open: boolean
}

export default React.memo(function PersonFeeDecisions({ id, open }: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const [feeDecisions, reloadFeeDecisions] = useApiState(
    () => getPersonFeeDecisions(id),
    [id]
  )

  const openRetroactiveDecisionsModal = useCallback(
    () => toggleUiMode('create-retroactive-fee-decision'),
    [toggleUiMode]
  )

  return (
    <CollapsibleSection
      icon={faChild}
      title={i18n.personProfile.feeDecisions.title}
      data-qa="person-fee-decisions-collapsible"
      startCollapsed={!open}
    >
      {uiMode === 'create-retroactive-fee-decision' ? (
        <Modal
          headOfFamily={id}
          clear={clearUiMode}
          loadDecisions={reloadFeeDecisions}
        />
      ) : null}
      <AddButtonRow
        text={i18n.personProfile.feeDecisions.createRetroactive}
        onClick={openRetroactiveDecisionsModal}
        disabled={!!uiMode}
        data-qa="create-retroactive-fee-decision-button"
      />
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
            {_.orderBy(feeDecisions, ['sentAt', 'validFrom'], ['desc']).map(
              (feeDecision: FeeDecision) => (
                <Tr key={`${feeDecision.id}`} data-qa="table-fee-decision-row">
                  <Td>
                    <Link to={`/finance/fee-decisions/${feeDecision.id}`}>
                      Maksupäätös {feeDecision.validDuring.format()}
                    </Link>
                  </Td>
                  <Td>{formatCents(feeDecision.totalFee)}</Td>
                  <Td>{feeDecision.decisionNumber}</Td>
                  <DateTd>{formatDate(feeDecision.created)}</DateTd>
                  <DateTd data-qa="fee-decision-sent-at">
                    {formatDate(feeDecision.sentAt)}
                  </DateTd>
                  <StatusTd>
                    {i18n.feeDecision.status[feeDecision.status]}
                  </StatusTd>
                </Tr>
              )
            )}
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
      return createRetroactiveFeeDecisions(
        headOfFamily,
        LocalDate.parseFiOrThrow(date)
      )
    }
    return Promise.resolve()
  }, [headOfFamily, date, dateIsValid])

  const onSuccess = useCallback(() => {
    clear()
    loadDecisions()
  }, [clear, loadDecisions])

  return (
    <AsyncFormModal
      icon={faPlus}
      iconColor="blue"
      title={i18n.personProfile.feeDecisions.createRetroactive}
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
          data-qa="retroactive-fee-decision-start-date"
        />
      </FixedSpaceRow>
    </AsyncFormModal>
  )
})
