// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'

import type { CaretakerAmount } from 'lib-common/generated/api-types/daycare'
import type { DaycareId, GroupId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { capitalizeFirstLetter } from 'lib-common/string'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { faPen, faTrash } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import type { TitleState } from '../../../state/title'
import { TitleContext } from '../../../state/title'
import { getStatusLabelByDateRange } from '../../../utils/date'
import { renderResult } from '../../async-rendering'
import StatusLabel from '../../common/StatusLabel'

import GroupCaretakersModal from './GroupCaretakersModal'
import { caretakersQuery, removeCaretakersMutation } from './queries'

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
  const unitId = useIdRouteParam<DaycareId>('unitId')
  const groupId = useIdRouteParam<GroupId>('groupId')
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)
  const caretakers = useQueryResult(
    caretakersQuery({ daycareId: unitId, groupId })
  )
  const [modalOpen, setModalOpen] = useState<boolean>(false)
  const [rowToEdit, setRowToEdit] = useState<CaretakerAmount | null>(null)

  useEffect(() => {
    if (caretakers.isSuccess) {
      setTitle(caretakers.value.groupName)
    }
  }, [caretakers, setTitle])

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        {renderResult(caretakers, (caretakers) => (
          <NarrowContainer>
            <Title size={2}>{i18n.titles.groupCaretakers}</Title>
            <Title size={3}>
              {caretakers.unitName} |{' '}
              {capitalizeFirstLetter(caretakers.groupName)}
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
                {caretakers.caretakers.map((row) => (
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
                          <IconOnlyButton
                            onClick={() => {
                              setRowToEdit(row)
                              setModalOpen(true)
                            }}
                            icon={faPen}
                            aria-label={i18n.common.edit}
                          />
                          <ConfirmedMutation
                            buttonStyle="ICON"
                            icon={faTrash}
                            confirmationTitle={
                              i18n.groupCaretakers.confirmDelete
                            }
                            mutation={removeCaretakersMutation}
                            onClick={() => ({
                              daycareId: unitId,
                              groupId,
                              id: row.id
                            })}
                            buttonAltText={i18n.common.remove}
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
                  setModalOpen(false)
                  setRowToEdit(null)
                }}
                onReject={() => {
                  setModalOpen(false)
                  setRowToEdit(null)
                }}
              />
            )}
          </NarrowContainer>
        ))}
      </ContentArea>
    </Container>
  )
})
