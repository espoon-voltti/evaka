// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import { PersonJSON } from 'lib-common/generated/api-types/pis'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2 } from 'lib-components/typography'
import { faQuestion } from 'lib-icons'

import Toolbar from '../../components/common/Toolbar'
import FridgePartnerModal from '../../components/person-profile/person-fridge-partner/FridgePartnerModal'
import {
  deletePartnership,
  getPartnerships,
  retryPartnership
} from '../../generated/api-clients/pis'
import { useTranslation } from '../../state/i18n'
import { PersonContext } from '../../state/person'
import { UIContext } from '../../state/ui'
import { formatName } from '../../utils'
import { ButtonsTd, DateTd, NameTd } from '../PersonProfile'
import { renderResult } from '../async-rendering'

const getPartnershipsResult = wrapResult(getPartnerships)
const deletePartnershipResult = wrapResult(deletePartnership)
const retryPartnershipResult = wrapResult(retryPartnership)

const TopBar = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`

interface Props {
  id: UUID
  open: boolean
}

const PersonFridgePartner = React.memo(function PersonFridgePartner({
  id,
  open: startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { reloadFamily, permittedActions } = useContext(PersonContext)
  const { uiMode, toggleUiMode, clearUiMode, setErrorMessage } =
    useContext(UIContext)
  const [open, setOpen] = useState(startOpen)
  const [partnerships, loadData] = useApiState(
    () => getPartnershipsResult({ personId: id }),
    [id]
  )
  const [selectedPartnershipId, setSelectedPartnershipId] = useState('')

  // FIXME: This component shouldn't know about family's dependency on its data
  const reload = () => {
    void loadData()
    reloadFamily()
  }

  const selectedPartnership = useMemo(
    () =>
      partnerships
        .map((ps) =>
          ps
            .map(({ data }) => data)
            .find((partner) => partner.id === selectedPartnershipId)
        )
        .getOrElse(undefined),
    [partnerships, selectedPartnershipId]
  )

  return (
    <div>
      {uiMode === 'add-fridge-partner' ? (
        <FridgePartnerModal headPersonId={id} onSuccess={reload} />
      ) : uiMode === `edit-fridge-partner-${selectedPartnershipId}` ? (
        <FridgePartnerModal
          partnership={selectedPartnership}
          headPersonId={id}
          onSuccess={reload}
        />
      ) : uiMode === `remove-fridge-partner-${selectedPartnershipId}` ? (
        <InfoModal
          type="warning"
          title={i18n.personProfile.fridgePartner.removePartner}
          text={i18n.personProfile.fridgePartner.confirmText}
          icon={faQuestion}
          reject={{ action: () => clearUiMode(), label: i18n.common.cancel }}
          resolve={{
            action: () =>
              deletePartnershipResult({
                partnershipId: selectedPartnershipId
              }).then((res) => {
                clearUiMode()
                if (res.isFailure) {
                  setErrorMessage({
                    type: 'error',
                    title: i18n.personProfile.fridgePartner.error.remove.title,
                    text: i18n.common.tryAgain,
                    resolveLabel: i18n.common.ok
                  })
                } else {
                  reload()
                }
              }),
            label: i18n.common.remove
          }}
        />
      ) : null}
      <CollapsibleContentArea
        title={<H2>{i18n.personProfile.partner}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="person-partners-collapsible"
      >
        <TopBar>
          <span className="subtitle">({i18n.personProfile.partnerInfo})</span>
          <AddButton
            flipped
            text={i18n.personProfile.partnerAdd}
            onClick={() => {
              setSelectedPartnershipId('')
              toggleUiMode('add-fridge-partner')
            }}
            data-qa="add-partner-button"
            disabled={!permittedActions.has('CREATE_PARTNERSHIP')}
          />
        </TopBar>
        {renderResult(partnerships, (partnerships) => (
          <Table data-qa="table-of-partners">
            <Thead>
              <Tr>
                <Th>{i18n.common.form.name}</Th>
                <Th>{i18n.common.form.socialSecurityNumber}</Th>
                <Th>{i18n.common.form.startDate}</Th>
                <Th>{i18n.common.form.endDate}</Th>
                <Th />
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {orderBy(
                partnerships,
                ['startDate', 'endDate'],
                ['desc', 'desc']
              ).map(({ permittedActions, data: fridgePartner }, i) =>
                fridgePartner.partners
                  .filter((p) => p.id !== id)
                  .map((partner: PersonJSON) => (
                    <Tr
                      key={`${partner.id}-${i}`}
                      data-qa="table-fridge-partner-row"
                    >
                      <NameTd>
                        <Link to={`/profile/${partner.id}`}>
                          {formatName(
                            partner.firstName,
                            partner.lastName,
                            i18n,
                            true
                          )}
                        </Link>
                      </NameTd>
                      <Td>{partner.socialSecurityNumber}</Td>
                      <DateTd>{fridgePartner.startDate.format()}</DateTd>
                      <DateTd>{fridgePartner.endDate?.format()}</DateTd>
                      <ButtonsTd>
                        <Toolbar
                          dateRange={fridgePartner}
                          conflict={fridgePartner.conflict}
                          onRetry={
                            fridgePartner.conflict
                              ? () => {
                                  void retryPartnershipResult({
                                    partnershipId: fridgePartner.id
                                  }).then(() => reload())
                                }
                              : undefined
                          }
                          onEdit={() => {
                            setSelectedPartnershipId(fridgePartner.id)
                            toggleUiMode(
                              `edit-fridge-partner-${fridgePartner.id}`
                            )
                          }}
                          onDelete={() => {
                            setSelectedPartnershipId(fridgePartner.id)
                            toggleUiMode(
                              `remove-fridge-partner-${fridgePartner.id}`
                            )
                          }}
                          editable={permittedActions.includes('UPDATE')}
                          deletable={permittedActions.includes('DELETE')}
                        />
                      </ButtonsTd>
                    </Tr>
                  ))
              )}
            </Tbody>
          </Table>
        ))}
      </CollapsibleContentArea>
    </div>
  )
})

export default PersonFridgePartner
