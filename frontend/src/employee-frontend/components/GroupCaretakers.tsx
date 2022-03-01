// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import Button from 'lib-components/atoms/buttons/Button'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { faPen, faQuestion, faTrash } from 'lib-icons'

import { deleteCaretakers, getCaretakers } from '../api/caretakers'
import StatusLabel from '../components/common/StatusLabel'
import GroupCaretakersModal from '../components/group-caretakers/GroupCaretakersModal'
import { useTranslation } from '../state/i18n'
import { TitleContext, TitleState } from '../state/title'
import { CaretakerAmount, CaretakersResponse } from '../types/caretakers'
import { capitalizeFirstLetter } from '../utils'
import { getStatusLabelByDateRange } from '../utils/date'

const NarrowContainer = styled.div`
  max-width: 900px;
`

const StyledTd = styled(Td)`
  vertical-align: middle !important;
  padding: 8px 16px !important;
`

const StatusTd = styled(StyledTd)`
  width: 30%;
  vertical-align: middle !important;
  padding: 8px 16px !important;

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

export default React.memo(function GroupCaretakers() {
  const { unitId, groupId } = useParams<{ unitId: UUID; groupId: UUID }>()
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)
  const [caretakers, setCaretakers] = useState<Result<CaretakersResponse>>(
    Loading.of()
  )
  const [modalOpen, setModalOpen] = useState<boolean>(false)
  const [rowToDelete, setRowToDelete] = useState<CaretakerAmount | null>(null)
  const [rowToEdit, setRowToEdit] = useState<CaretakerAmount | null>(null)

  const loadData = () => {
    setCaretakers(Loading.of())
    void getCaretakers(unitId, groupId).then((response) => {
      setCaretakers(response)
      if (response.isSuccess) setTitle(response.value.groupName)
    })
  }

  useEffect(() => {
    loadData()
  }, [unitId, groupId]) // eslint-disable-line react-hooks/exhaustive-deps

  const deleteRow = (id: UUID) => {
    void deleteCaretakers(unitId, groupId, id).then(loadData)
    setRowToDelete(null)
  }

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        {caretakers.isLoading && <Loader />}
        {caretakers.isFailure && <span>{i18n.common.error.unknown}</span>}
        {caretakers.isSuccess && (
          <NarrowContainer>
            <Title size={2}>{i18n.groupCaretakers.title}</Title>
            <Title size={3}>
              {caretakers.value.unitName} |{' '}
              {capitalizeFirstLetter(caretakers.value.groupName)}
            </Title>
            <p>{i18n.groupCaretakers.info}</p>
            <FlexRowRightAlign>
              <Button
                onClick={() => setModalOpen(true)}
                text={i18n.groupCaretakers.create}
              />
            </FlexRowRightAlign>
            <Table>
              <Thead>
                <Tr>
                  <Th>{i18n.groupCaretakers.startDate}</Th>
                  <Th>{i18n.groupCaretakers.endDate}</Th>
                  <Th>{i18n.groupCaretakers.amount}</Th>
                  <Th>{i18n.groupCaretakers.status}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {caretakers.value.caretakers.map((row) => (
                  <Tr key={row.id}>
                    <StyledTd>{row.startDate.format()}</StyledTd>
                    <StyledTd>
                      {row.endDate ? row.endDate.format() : ''}
                    </StyledTd>
                    <StyledTd>
                      {row.amount.toLocaleString()}{' '}
                      {i18n.groupCaretakers.amountUnit}
                    </StyledTd>
                    <StatusTd>
                      <div>
                        <StatusLabel status={getStatusLabelByDateRange(row)} />
                        <FixedSpaceRow>
                          <IconButton
                            onClick={() => {
                              setRowToEdit(row)
                              setModalOpen(true)
                            }}
                            icon={faPen}
                          />
                          <IconButton
                            onClick={() => setRowToDelete(row)}
                            icon={faTrash}
                          />
                        </FixedSpaceRow>
                      </div>
                    </StatusTd>
                  </Tr>
                ))}
              </Tbody>
            </Table>

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
                type="warning"
                title={i18n.groupCaretakers.confirmDelete}
                icon={faQuestion}
                reject={{
                  action: () => setRowToDelete(null),
                  label: i18n.common.cancel
                }}
                resolve={{
                  action: () => deleteRow(rowToDelete?.id),
                  label: i18n.common.remove
                }}
              />
            )}
          </NarrowContainer>
        )}
      </ContentArea>
    </Container>
  )
})
