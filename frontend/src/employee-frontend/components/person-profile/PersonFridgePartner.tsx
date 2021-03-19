// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import { UUID } from '../../types'
import { useTranslation } from '../../state/i18n'
import { useEffect } from 'react'
import { Loading, Result } from 'lib-common/api'
import { useContext } from 'react'
import { PersonContext } from '../../state/person'
import {
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from 'lib-components/layout/Table'
import Loader from 'lib-components/atoms/Loader'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Partnership } from '../../types/fridge'
import * as _ from 'lodash'
import { UIContext } from '../../state/ui'
import FridgePartnerModal from '../../components/person-profile/person-fridge-partner/FridgePartnerModal'
import { Link } from 'react-router-dom'
import { formatName } from '../../utils'
import { faQuestion, faUser } from 'lib-icons'
import { PersonDetails } from '../../types/person'
import {
  getPartnerships,
  removePartnership,
  retryPartnership
} from '../../api/partnerships'
import { ButtonsTd, DateTd, NameTd } from '../../components/PersonProfile'
import Toolbar from '../../components/common/Toolbar'
import AddButton from 'lib-components/atoms/buttons/AddButton'

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
  const { partnerships, setPartnerships, reloadFamily } = useContext(
    PersonContext
  )
  const { uiMode, toggleUiMode, clearUiMode, setErrorMessage } = useContext(
    UIContext
  )
  const [selectedPartnershipId, setSelectedPartnershipId] = useState('')

  const loadData = () => {
    setPartnerships(Loading.of())
    void getPartnerships(id).then(setPartnerships)
  }

  // FIXME: This component shouldn't know about family's dependency on its data
  const reload = () => {
    loadData()
    reloadFamily(id)
  }

  useEffect(loadData, [id, setPartnerships])

  const getPartnershipById = (id: UUID) => {
    return partnerships
      .map((ps) => ps.find((partner) => partner.id === id))
      .getOrElse(undefined)
  }

  const renderFridgePartnerModal = () => {
    if (uiMode === 'add-fridge-partner') {
      return <FridgePartnerModal headPersonId={id} onSuccess={reload} />
    } else if (uiMode === `edit-fridge-partner-${selectedPartnershipId}`) {
      return (
        <FridgePartnerModal
          partnership={getPartnershipById(selectedPartnershipId)}
          headPersonId={id}
          onSuccess={reload}
        />
      )
    } else if (uiMode === `remove-fridge-partner-${selectedPartnershipId}`) {
      return (
        <InfoModal
          iconColour={'orange'}
          title={i18n.personProfile.fridgePartner.removePartner}
          text={i18n.personProfile.fridgePartner.confirmText}
          icon={faQuestion}
          reject={{ action: () => clearUiMode(), label: i18n.common.cancel }}
          resolve={{
            action: () =>
              removePartnership(selectedPartnershipId).then(
                (res: Result<null>) => {
                  clearUiMode()
                  if (res.isFailure) {
                    setErrorMessage({
                      type: 'error',
                      title:
                        i18n.personProfile.fridgePartner.error.remove.title,
                      text: i18n.common.tryAgain,
                      resolveLabel: i18n.common.ok
                    })
                  } else {
                    reload()
                  }
                }
              ),
            label: i18n.common.remove
          }}
        />
      )
    }
    return
  }

  const renderFridgePartners = () =>
    partnerships.isSuccess
      ? _.orderBy(
          partnerships.value,
          ['startDate', 'endDate'],
          ['desc', 'desc']
        ).map((fridgePartner: Partnership, i: number) => {
          return fridgePartner.partners
            .filter((p) => p.id !== id)
            .map((partner: PersonDetails) => {
              return (
                <Tr
                  key={`${partner.id}-${i}`}
                  data-qa="table-fridge-partner-row"
                >
                  <NameTd>
                    <Link to={`/profile/${partner.id}`}>
                      {formatName(partner.firstName, partner.lastName, i18n)}
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
                              void retryPartnership(fridgePartner.id).then(() =>
                                reload()
                              )
                            }
                          : undefined
                      }
                      onEdit={() => {
                        setSelectedPartnershipId(fridgePartner.id)
                        toggleUiMode(`edit-fridge-partner-${fridgePartner.id}`)
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
        })
      : null

  return (
    <div>
      {renderFridgePartnerModal()}
      <CollapsibleSection
        icon={faUser}
        title={i18n.personProfile.partner}
        startCollapsed={!open}
        dataQa="person-partners-collapsible"
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
          />
        </TopBar>
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
          <Tbody>{renderFridgePartners()}</Tbody>
        </Table>
        {partnerships.isLoading && <Loader />}
        {partnerships.isFailure && <div>{i18n.common.loadingFailed}</div>}
      </CollapsibleSection>
    </div>
  )
})

export default PersonFridgePartner
