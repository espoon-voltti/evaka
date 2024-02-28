// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { PersonContext } from 'employee-frontend/state/person'
import { wrapResult } from 'lib-common/api'
import { FeeDecision } from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { formatCents } from 'lib-common/money'
import { UUID } from 'lib-common/types'
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
  generateRetroactiveFeeDecisions,
  getHeadOfFamilyFeeDecisions
} from '../../generated/api-clients/invoicing'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { DateTd, StatusTd } from '../PersonProfile'
import { renderResult } from '../async-rendering'

const getHeadOfFamilyFeeDecisionsResult = wrapResult(
  getHeadOfFamilyFeeDecisions
)
const generateRetroactiveFeeDecisionsResult = wrapResult(
  generateRetroactiveFeeDecisions
)

interface Props {
  id: UUID
  open: boolean
}

export default React.memo(function PersonFeeDecisions({
  id,
  open: startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const { permittedActions } = useContext(PersonContext)
  const [open, setOpen] = useState(startOpen)
  const [feeDecisions, reloadFeeDecisions] = useApiState(
    () => getHeadOfFamilyFeeDecisionsResult({ id }),
    [id]
  )

  const openRetroactiveDecisionsModal = useCallback(
    () => toggleUiMode('create-retroactive-fee-decision'),
    [toggleUiMode]
  )

  return (
    <CollapsibleContentArea
      title={<H2>{i18n.personProfile.feeDecisions.title}</H2>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-qa="person-fee-decisions-collapsible"
    >
      {uiMode === 'create-retroactive-fee-decision' ? (
        <Modal
          headOfFamily={id}
          clear={clearUiMode}
          loadDecisions={reloadFeeDecisions}
        />
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
            {orderBy(feeDecisions, ['sentAt', 'validFrom'], ['desc']).map(
              (feeDecision: FeeDecision) => (
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
              )
            )}
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
      return generateRetroactiveFeeDecisionsResult({
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
      title={i18n.personProfile.feeDecisions.createRetroactive}
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
          locale={lang}
          date={date}
          onChange={setDate}
          hideErrorsBeforeTouched
          openAbove
          data-qa="retroactive-fee-decision-start-date"
        />
      </FixedSpaceRow>
    </AsyncFormModal>
  )
})
