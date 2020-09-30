// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import {
  Button,
  Container,
  ContentArea,
  Loader,
  Table,
  Title
} from '~components/shared/alpha'
import { RouteComponentProps } from 'react-router'
import { UUID } from '~types'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import { CaretakerAmount, CaretakersResponse } from '~types/caretakers'
import { deleteCaretakers, getCaretakers } from '~api/caretakers'
import { TitleContext, TitleState } from '~state/title'
import { capitalizeFirstLetter } from '~utils'
import { getStatusLabelByDateRange } from '~utils/date'
import StatusLabel from '~components/common/StatusLabel'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPen, faQuestion, faTrash } from 'icon-set'
import GroupCaretakersModal from '~components/group-caretakers/GroupCaretakersModal'
import InfoModal from '~components/common/InfoModal'
import { useTranslation } from '~state/i18n'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'

const NarrowContainer = styled.div`
  max-width: 900px;
`

const Td = styled(Table.Td)`
  vertical-align: middle !important;
  padding: 8px 16px !important;
`

const StatusTd = styled(Td)`
  width: 30%;

  > div {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
`

const FlexRow = styled.div`
  display: flex;
`

const FlexRowRightAlign = styled(FlexRow)`
  justify-content: flex-end;
`

const NarrowButton = styled(Button)`
  min-width: unset;
`

type Props = RouteComponentProps<{ unitId: UUID; groupId: UUID }>

function GroupCaretakers({
  match: {
    params: { unitId, groupId }
  }
}: Props) {
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)
  const [caretakers, setCaretakers] = useState<Result<CaretakersResponse>>(
    Loading()
  )
  const [modalOpen, setModalOpen] = useState<boolean>(false)
  const [rowToDelete, setRowToDelete] = useState<CaretakerAmount | null>(null)
  const [rowToEdit, setRowToEdit] = useState<CaretakerAmount | null>(null)

  const loadData = () => {
    setCaretakers(Loading())
    void getCaretakers(unitId, groupId).then((response) => {
      setCaretakers(response)
      if (isSuccess(response)) setTitle(response.data.groupName)
    })
  }

  useEffect(() => {
    loadData()
  }, [unitId, groupId])

  const deleteRow = (id: UUID) => {
    void deleteCaretakers(unitId, groupId, id).then(loadData)
    setRowToDelete(null)
  }

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        {isLoading(caretakers) && <Loader />}
        {isFailure(caretakers) && <span>{i18n.common.error.unknown}</span>}
        {isSuccess(caretakers) && (
          <NarrowContainer>
            <Title size={2}>{i18n.groupCaretakers.title}</Title>
            <Title size={3}>
              {caretakers.data.unitName} |{' '}
              {capitalizeFirstLetter(caretakers.data.groupName)}
            </Title>
            <p>{i18n.groupCaretakers.info}</p>
            <FlexRowRightAlign>
              <Button plain onClick={() => setModalOpen(true)}>
                {i18n.groupCaretakers.create}
              </Button>
            </FlexRowRightAlign>
            <Table.Table>
              <Table.Head>
                <Table.Row>
                  <Table.Th>{i18n.groupCaretakers.startDate}</Table.Th>
                  <Table.Th>{i18n.groupCaretakers.endDate}</Table.Th>
                  <Table.Th>{i18n.groupCaretakers.amount}</Table.Th>
                  <Table.Th>{i18n.groupCaretakers.status}</Table.Th>
                </Table.Row>
              </Table.Head>
              <Table.Body>
                {caretakers.data.caretakers.map((row) => (
                  <Table.Row key={row.id}>
                    <Td>{row.startDate.format()}</Td>
                    <Td>{row.endDate ? row.endDate.format() : ''}</Td>
                    <Td>
                      {row.amount.toLocaleString()}{' '}
                      {i18n.groupCaretakers.amountUnit}
                    </Td>
                    <StatusTd>
                      <div>
                        <StatusLabel status={getStatusLabelByDateRange(row)} />
                        <FlexRow>
                          <NarrowButton
                            plain
                            onClick={() => {
                              setRowToEdit(row)
                              setModalOpen(true)
                            }}
                          >
                            <FontAwesomeIcon icon={faPen} size="lg" />
                          </NarrowButton>
                          <NarrowButton
                            plain
                            onClick={() => setRowToDelete(row)}
                          >
                            <FontAwesomeIcon icon={faTrash} size="lg" />
                          </NarrowButton>
                        </FlexRow>
                      </div>
                    </StatusTd>
                  </Table.Row>
                ))}
              </Table.Body>
            </Table.Table>

            {modalOpen && (
              <GroupCaretakersModal
                unitId={unitId}
                groupId={groupId}
                existing={rowToEdit}
                onSuccess={() => {
                  loadData()
                  setModalOpen(false)
                  setRowToEdit(null)
                }}
                onReject={() => {
                  setModalOpen(false)
                  setRowToEdit(null)
                }}
              />
            )}

            {rowToDelete && (
              <InfoModal
                iconColour={'orange'}
                title={i18n.groupCaretakers.confirmDelete}
                resolveLabel={i18n.common.remove}
                rejectLabel={i18n.common.cancel}
                icon={faQuestion}
                resolve={() => deleteRow(rowToDelete?.id)}
                reject={() => setRowToDelete(null)}
              />
            )}
          </NarrowContainer>
        )}
      </ContentArea>
    </Container>
  )
}

export default GroupCaretakers
