// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import _ from 'lodash'
import { faChild, faPlus } from '@evaka/icons'
import LocalDate from '@evaka/lib-common/src/local-date'
import { UUID } from '~types'
import { useTranslation, Translations } from '~state/i18n'
import { UIContext } from '~state/ui'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import { Link } from 'react-router-dom'
import { Collapsible, Loader, Table } from '~components/shared/alpha'
import { AddButtonRow } from 'components/shared/atoms/buttons/AddButton'
import DateInput from '~components/common/DateInput'
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
    Loading()
  )
  const [toggled, setToggled] = useState(open)
  const toggle = useCallback(() => setToggled((toggled) => !toggled), [
    setToggled
  ])

  const loadDecisions = useCallback(() => {
    setFeeDecisions(Loading())
    void getPersonFeeDecisions(id).then((response) => {
      setFeeDecisions(response)
    })
  }, [id, setFeeDecisions])

  useEffect(loadDecisions, [id, setFeeDecisions])

  const renderFeeDecisions = () =>
    isSuccess(feeDecisions)
      ? _.orderBy(feeDecisions.data, ['createdAt'], ['desc']).map(
          (feeDecision: FeeDecision) => {
            return (
              <Table.Row
                key={`${feeDecision.id}`}
                dataQa="table-fee-decision-row"
              >
                <Table.Td>
                  <Link to={`/fee-decisions/${feeDecision.id}`}>
                    Maksupäätös{' '}
                    {`${feeDecision.validFrom.format()} - ${
                      feeDecision.validTo?.format() ?? ''
                    }`}
                  </Link>
                </Table.Td>
                <Table.Td>{formatCents(feeDecision.totalFee)}</Table.Td>
                <Table.Td>{feeDecision.decisionNumber}</Table.Td>
                <DateTd>{formatDate(feeDecision.createdAt)}</DateTd>
                <DateTd>{formatDate(feeDecision.sentAt)}</DateTd>
                <StatusTd>
                  {i18n.feeDecision.status[feeDecision.status]}
                </StatusTd>
              </Table.Row>
            )
          }
        )
      : null

  return (
    <div>
      <Collapsible
        icon={faChild}
        title={i18n.personProfile.feeDecisions.title}
        open={toggled}
        onToggle={toggle}
        dataQa="person-fee-decisions-collapsible"
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
        <Table.Table dataQa="table-of-fee-decisions">
          <Table.Head>
            <Table.Row>
              <Table.Th>{i18n.feeDecisions.table.validity}</Table.Th>
              <Table.Th>{i18n.feeDecisions.table.price}</Table.Th>
              <Table.Th>{i18n.feeDecisions.table.number}</Table.Th>
              <Table.Th>{i18n.feeDecisions.table.createdAt}</Table.Th>
              <Table.Th>{i18n.feeDecisions.table.sentAt}</Table.Th>
              <Table.Th>{i18n.feeDecisions.table.status}</Table.Th>
            </Table.Row>
          </Table.Head>
          <Table.Body>{renderFeeDecisions()}</Table.Body>
        </Table.Table>
        {isLoading(feeDecisions) && <Loader />}
        {isFailure(feeDecisions) && <div>{i18n.common.loadingFailed}</div>}
      </Collapsible>
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
          <DateInput
            initial={undefined}
            onChange={(date?: LocalDate | 'invalid') =>
              date === 'invalid' ? setDate(undefined) : setDate(date)
            }
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
