// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as _ from 'lodash'
import React, { useContext, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Partnership, PersonJSON } from 'lib-common/generated/api-types/pis'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { faQuestion, faUser } from 'lib-icons'

import {
  getPartnerships,
  removePartnership,
  retryPartnership
} from '../../api/partnerships'
import Toolbar from '../../components/common/Toolbar'
import FridgePartnerModal from '../../components/person-profile/person-fridge-partner/FridgePartnerModal'
import { useTranslation } from '../../state/i18n'
import { PersonContext } from '../../state/person'
import { UIContext } from '../../state/ui'
import { formatName } from '../../utils'
import { ButtonsTd, DateTd, NameTd } from '../PersonProfile'
import { renderResult } from '../async-rendering'

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
  open
}: Props) {
  const { i18n } = useTranslation()
  const { reloadFamily } = useContext(PersonContext)
  const { uiMode, toggleUiMode, clearUiMode, setErrorMessage } =
    useContext(UIContext)
  const [partnerships, loadData] = useApiState(() => getPartnerships(id), [id])
  const [selectedPartnershipId, setSelectedPartnershipId] = useState('')

  // FIXME: This component shouldn't know about family's dependency on its data
  const reload = () => {
    loadData()
    reloadFamily()
  }

  const selectedPartnership = useMemo(
    () =>
      partnerships
        .map((ps) => ps.find((partner) => partner.id === selectedPartnershipId))
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
              removePartnership(selectedPartnershipId).then((res) => {
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
      <CollapsibleSection
        icon={faUser}
        title={i18n.personProfile.partner}
        startCollapsed={!open}
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
              {_.orderBy(
                partnerships,
                ['startDate', 'endDate'],
                ['desc', 'desc']
              ).map((fridgePartner: Partnership, i: number) => {
                return fridgePartner.partners
                  .filter((p) => p.id !== id)
                  .map((partner: PersonJSON) => {
                    return (
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
                                    void retryPartnership(
                                      fridgePartner.id
                                    ).then(() => reload())
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
                          />
                        </ButtonsTd>
                      </Tr>
                    )
                  })
              })}
            </Tbody>
          </Table>
        ))}
      </CollapsibleSection>
    </div>
  )
})

export default PersonFridgePartner
