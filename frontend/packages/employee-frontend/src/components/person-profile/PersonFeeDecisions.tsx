// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import _ from 'lodash'
import { Link } from 'react-router-dom'

import { faChild, faPlus } from '@evaka/lib-icons'
import LocalDate from '@evaka/lib-common/src/local-date'
import { UUID } from '~types'
import { useTranslation, Translations } from '~state/i18n'
import { UIContext } from '~state/ui'
import { Loading, Result } from '~api'
import CollapsibleSection from 'components/shared/molecules/CollapsibleSection'
import { Table, Tbody, Td, Th, Thead, Tr } from 'components/shared/layout/Table'
import Loader from '~components/shared/atoms/Loader'
import { AddButtonRow } from 'components/shared/atoms/buttons/AddButton'
import FormModal from '~components/common/FormModal'
import { formatDate } from '~utils/date'
import {
  getPersonFeeDecisions,
  createRetroactiveDecisions
} from '~api/invoicing'
import { FeeDecision } from '~types/invoicing'
import { DateTd, StatusTd } from '~components/PersonProfile'
import { formatCents } from '~utils/money'
import { EspooColours } from '~utils/colours'
import { DatePicker } from '~components/common/DatePicker'

interface Props {
  id: UUID
  open: boolean
}

const PersonFeeDecisions = React.memo(function PersonFeeDecisions({
  id,
  open
}: Props) {
  const { i18n } = useTranslation()
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const [feeDecisions, setFeeDecisions] = useState<Result<FeeDecision[]>>(
    Loading.of()
  )

  const loadDecisions = useCallback(() => {
    setFeeDecisions(Loading.of())
    void getPersonFeeDecisions(id).then((response) => {
      setFeeDecisions(response)
    })
  }, [id, setFeeDecisions])

  useEffect(loadDecisions, [id, setFeeDecisions])

  const renderFeeDecisions = () =>
    feeDecisions.isSuccess
      ? _.orderBy(feeDecisions.value, ['createdAt'], ['desc']).map(
          (feeDecision: FeeDecision) => {
            return (
              <Tr key={`${feeDecision.id}`} data-qa="table-fee-decision-row">
                <Td>
                  <Link to={`/finance/fee-decisions/${feeDecision.id}`}>
                    Maksupäätös{' '}
                    {`${feeDecision.validFrom.format()} - ${
                      feeDecision.validTo?.format() ?? ''
                    }`}
                  </Link>
                </Td>
                <Td>{formatCents(feeDecision.totalFee)}</Td>
                <Td>{feeDecision.decisionNumber}</Td>
                <DateTd>{formatDate(feeDecision.createdAt)}</DateTd>
                <DateTd>{formatDate(feeDecision.sentAt)}</DateTd>
                <StatusTd>
                  {i18n.feeDecision.status[feeDecision.status]}
                </StatusTd>
              </Tr>
            )
          }
        )
      : null

  return (
    <div>
      <CollapsibleSection
        icon={faChild}
        title={i18n.personProfile.feeDecisions.title}
        dataQa="person-fee-decisions-collapsible"
        startCollapsed={!open}
      >
        {uiMode === 'create-retroactive-fee-decision' ? (
          <Modal
            i18n={i18n}
            headOfFamily={id}
            clear={clearUiMode}
            loadDecisions={loadDecisions}
          />
        ) : null}
        <AddButtonRow
          text={i18n.personProfile.feeDecisions.button}
          onClick={() => {
            toggleUiMode('create-retroactive-fee-decision')
          }}
          disabled={!!uiMode}
          dataQa="create-retroactive-fee-decision-button"
        />
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
          <Tbody>{renderFeeDecisions()}</Tbody>
        </Table>
        {feeDecisions.isLoading && <Loader />}
        {feeDecisions.isFailure && <div>{i18n.common.loadingFailed}</div>}
      </CollapsibleSection>
    </div>
  )
})

const ModalContent = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
`

const InputContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
`

const Label = styled.label`
  font-weight: 600;
  margin-right: 20px;
  padding-bottom: calc(12px + 0.575em + 2px);
`

const ErrorMessage = styled.div`
  color: ${EspooColours.red};
`

const Modal = React.memo(function Modal({
  i18n,
  headOfFamily,
  clear,
  loadDecisions
}: {
  i18n: Translations
  headOfFamily: string
  clear: () => void
  loadDecisions: () => void
}) {
  const [date, setDate] = useState<LocalDate>()
  const [error, setError] = useState(false)

  const resolve = useCallback(() => {
    if (date) {
      setError(false)
      void createRetroactiveDecisions(headOfFamily, date)
        .then(clear)
        .then(loadDecisions)
        .catch(() => setError(true))
    }
  }, [clear, date])

  return (
    <FormModal
      size="lg"
      icon={faPlus}
      iconColour={'blue'}
      title={i18n.personProfile.feeDecisions.modalTitle}
      reject={clear}
      rejectLabel={i18n.common.cancel}
      resolve={resolve}
      resolveLabel={i18n.common.create}
      resolveDisabled={date === undefined}
    >
      <ModalContent>
        <InputContainer>
          <Label>Alkaen</Label>
          <DatePicker
            type={'full-width'}
            date={date ?? undefined}
            onChange={(value) => setDate(value)}
          />
        </InputContainer>
        {error ? (
          <ErrorMessage>{i18n.common.error.unknown}</ErrorMessage>
        ) : null}
      </ModalContent>
    </FormModal>
  )
})

export default PersonFeeDecisions
