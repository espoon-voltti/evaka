// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext, useMemo, useState } from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import { PersonJSON } from 'lib-common/generated/api-types/pis'
import { PartnershipId, PersonId } from 'lib-common/generated/api-types/shared'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import Tooltip from 'lib-components/atoms/Tooltip'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { faQuestion } from 'lib-icons'

import Toolbar from '../../components/common/Toolbar'
import FridgePartnerModal from '../../components/person-profile/person-fridge-partner/FridgePartnerModal'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { formatName } from '../../utils'
import { renderResult } from '../async-rendering'

import { ButtonsTd, DateTd, NameTd } from './common'
import {
  deletePartnershipMutation,
  partnershipsQuery,
  retryPartnershipMutation
} from './queries'
import { PersonContext } from './state'

const TopBar = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`

interface Props {
  id: PersonId
}

const PersonFridgePartner = React.memo(function PersonFridgePartner({
  id
}: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext(PersonContext)
  const { uiMode, toggleUiMode, clearUiMode, setErrorMessage } =
    useContext(UIContext)

  const partnerships = useQueryResult(partnershipsQuery({ personId: id }))
  const [selectedPartnershipId, setSelectedPartnershipId] =
    useState<PartnershipId>()

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

  const { mutateAsync: deletePartnership } = useMutationResult(
    deletePartnershipMutation
  )
  const { mutateAsync: retryPartnership } = useMutationResult(
    retryPartnershipMutation
  )

  return (
    <div>
      {uiMode === 'add-fridge-partner' ? (
        <FridgePartnerModal headPersonId={id} />
      ) : uiMode === `edit-fridge-partner-${selectedPartnershipId}` ? (
        <FridgePartnerModal
          partnership={selectedPartnership}
          headPersonId={id}
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
              deletePartnership({
                personId: id,
                partnershipId: selectedPartnershipId!
              }).then((res) => {
                clearUiMode()
                if (res.isFailure) {
                  setErrorMessage({
                    type: 'error',
                    title: i18n.personProfile.fridgePartner.error.remove.title,
                    text: i18n.common.tryAgain,
                    resolveLabel: i18n.common.ok
                  })
                }
              }),
            label: i18n.common.remove
          }}
        />
      ) : null}
      <TopBar>
        <span className="subtitle">({i18n.personProfile.partnerInfo})</span>
        <AddButton
          flipped
          text={i18n.personProfile.partnerAdd}
          onClick={() => {
            setSelectedPartnershipId(undefined)
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
              <Th>{i18n.common.form.lastModified}</Th>
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
                .map((partner: PersonJSON) => {
                  const modifiedAt =
                    fridgePartner?.creationModificationMetadata?.modifiedAt ||
                    fridgePartner?.creationModificationMetadata?.createdAt
                  const modifiedByName =
                    fridgePartner?.creationModificationMetadata
                      ?.modifiedByName ||
                    fridgePartner?.creationModificationMetadata?.createdByName

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
                      <Td>
                        {modifiedAt ? (
                          <Tooltip
                            tooltip={
                              modifiedByName
                                ? i18n.common.form.lastModifiedBy(
                                    modifiedByName
                                  )
                                : null
                            }
                            position="left"
                          >
                            {modifiedAt.format()}
                          </Tooltip>
                        ) : (
                          i18n.common.unknown
                        )}
                      </Td>
                      <ButtonsTd>
                        <Toolbar
                          dateRange={fridgePartner}
                          conflict={fridgePartner.conflict}
                          onRetry={
                            fridgePartner.conflict
                              ? () => {
                                  void retryPartnership({
                                    personId: id,
                                    partnershipId: fridgePartner.id
                                  })
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
                  )
                })
            )}
          </Tbody>
        </Table>
      ))}
    </div>
  )
})

export default PersonFridgePartner
