// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'
import { UUID } from '~types'
import { useTranslation } from '~state/i18n'
import { useEffect } from 'react'
import { isFailure, isSuccess, isLoading, Loading, Result } from '~api'
import { useContext } from 'react'
import { PersonContext } from '~state/person'
import { Collapsible, Loader, Table } from '~components/shared/alpha'
import InfoModal from '~components/common/InfoModal'
import { Partnership } from '~types/fridge'
import * as _ from 'lodash'
import { UIContext } from '~state/ui'
import FridgePartnerModal from '~components/person-profile/person-fridge-partner/FridgePartnerModal'
import { Link } from 'react-router-dom'
import { formatName } from '~utils'
import { faQuestion, faUser } from '@evaka/icons'
import { PersonDetails } from '~types/person'
import {
  getPartnerships,
  removePartnership,
  retryPartnership
} from '~api/partnerships'
import { ButtonsTd, DateTd, NameTd } from '~components/PersonProfile'
import Toolbar from 'components/shared/molecules/Toolbar'
import AddButton from 'components/shared/atoms/buttons/AddButton'

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
  const [toggled, setToggled] = useState(open)
  const toggle = useCallback(() => setToggled((toggled) => !toggled), [
    setToggled
  ])

  const loadData = () => {
    setPartnerships(Loading())
    void getPartnerships(id).then(setPartnerships)
  }

  // FIXME: This component shouldn't know about family's dependency on its data
  const reload = () => {
    loadData()
    reloadFamily(id)
  }

  useEffect(loadData, [id, setPartnerships])

  const getPartnershipById = (id: UUID) => {
    return isSuccess(partnerships)
      ? partnerships.data.find((partner) => partner.id === id)
      : undefined
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
          resolveLabel={i18n.common.remove}
          rejectLabel={i18n.common.cancel}
          icon={faQuestion}
          reject={() => clearUiMode()}
          resolve={() =>
            removePartnership(selectedPartnershipId).then(
              (res: Result<null>) => {
                clearUiMode()
                if (isFailure(res)) {
                  setErrorMessage({
                    type: 'error',
                    title: i18n.personProfile.fridgePartner.error.remove.title,
                    text: i18n.common.tryAgain
                  })
                } else {
                  reload()
                }
              }
            )
          }
        />
      )
    }
    return
  }

  const renderFridgePartners = () =>
    isSuccess(partnerships)
      ? _.orderBy(
          partnerships.data,
          ['startDate', 'endDate'],
          ['desc', 'desc']
        ).map((fridgePartner: Partnership, i: number) => {
          return fridgePartner.partners
            .filter((p) => p.id !== id)
            .map((partner: PersonDetails) => {
              return (
                <Table.Row
                  key={`${partner.id}-${i}`}
                  dataQa="table-fridge-partner-row"
                >
                  <NameTd>
                    <Link to={`/profile/${partner.id}`}>
                      {formatName(partner.firstName, partner.lastName, i18n)}
                    </Link>
                  </NameTd>
                  <Table.Td>{partner.socialSecurityNumber}</Table.Td>
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
                </Table.Row>
              )
            })
        })
      : null

  return (
    <div>
      {renderFridgePartnerModal()}
      <Collapsible
        icon={faUser}
        title={i18n.personProfile.partner}
        open={toggled}
        onToggle={toggle}
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
        <Table.Table dataQa="table-of-partners">
          <Table.Head>
            <Table.Row>
              <Table.Th>{i18n.common.form.name}</Table.Th>
              <Table.Th>{i18n.common.form.socialSecurityNumber}</Table.Th>
              <Table.Th>{i18n.common.form.startDate}</Table.Th>
              <Table.Th>{i18n.common.form.endDate}</Table.Th>
              <Table.Th />
              <Table.Th />
            </Table.Row>
          </Table.Head>
          <Table.Body>{renderFridgePartners()}</Table.Body>
        </Table.Table>
        {isLoading(partnerships) && <Loader />}
        {isFailure(partnerships) && <div>{i18n.common.loadingFailed}</div>}
      </Collapsible>
    </div>
  )
})

export default PersonFridgePartner
