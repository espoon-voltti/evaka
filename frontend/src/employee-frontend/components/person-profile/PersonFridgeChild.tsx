// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext, useState } from 'react'
import { Link } from 'wouter'

import type { ParentshipWithPermittedActions } from 'lib-common/generated/api-types/pis'
import type {
  ParentshipId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { getAge } from 'lib-common/utils/local-date'
import Tooltip from 'lib-components/atoms/Tooltip'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { faQuestion } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { formatName } from '../../utils'
import { renderResult } from '../async-rendering'
import Toolbar from '../common/Toolbar'

import { ButtonsTd, DateTd, NameTd } from './common'
import FridgeChildModal from './person-fridge-child/FridgeChildModal'
import {
  deleteParentshipMutation,
  parentshipsQuery,
  retryParentshipMutation
} from './queries'
import { PersonContext } from './state'

interface Props {
  id: PersonId
}

export default React.memo(function PersonFridgeChild({ id }: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext(PersonContext)
  const { uiMode, toggleUiMode, clearUiMode, setErrorMessage } =
    useContext(UIContext)
  const [selectedParentshipId, setSelectedParentshipId] =
    useState<ParentshipId>()

  const fridgeChildren = useQueryResult(parentshipsQuery({ headOfChildId: id }))

  const getFridgeChildById = (id: ParentshipId | undefined) =>
    fridgeChildren
      .map((ps) => ps.find(({ data }) => data.id === id)?.data)
      .getOrElse(undefined)

  const { mutateAsync: deleteParentship } = useMutationResult(
    deleteParentshipMutation
  )
  const { mutateAsync: retryParentship } = useMutationResult(
    retryParentshipMutation
  )

  return (
    <div>
      {uiMode === 'add-fridge-child' ? (
        <FridgeChildModal headPersonId={id} />
      ) : uiMode === `edit-fridge-child-${selectedParentshipId}` ? (
        <FridgeChildModal
          parentship={getFridgeChildById(selectedParentshipId)}
          headPersonId={id}
        />
      ) : uiMode === `remove-fridge-child-${selectedParentshipId}` ? (
        <InfoModal
          type="warning"
          title={i18n.personProfile.fridgeChild.removeChild}
          text={i18n.personProfile.fridgeChild.confirmText}
          icon={faQuestion}
          reject={{ action: () => clearUiMode(), label: i18n.common.cancel }}
          resolve={{
            action: () =>
              deleteParentship({
                headOfChildId: id,
                id: selectedParentshipId!
              }).then((res) => {
                clearUiMode()
                if (res.isFailure) {
                  setErrorMessage({
                    type: 'error',
                    title: i18n.personProfile.fridgeChild.error.remove.title,
                    text: i18n.common.tryAgain,
                    resolveLabel: i18n.common.ok
                  })
                }
              }),
            label: i18n.common.remove
          }}
        />
      ) : null}
      <AddButtonRow
        text={i18n.personProfile.fridgeChildAdd}
        onClick={() => {
          toggleUiMode('add-fridge-child')
        }}
        data-qa="add-child-button"
        disabled={!permittedActions.has('CREATE_PARENTSHIP')}
      />
      {renderResult(fridgeChildren, (parentships) => (
        <Table data-qa="table-of-children">
          <Thead>
            <Tr>
              <Th>{i18n.common.form.name}</Th>
              <Th>{i18n.common.form.socialSecurityNumber}</Th>
              <Th>{i18n.common.form.age}</Th>
              <Th>{i18n.common.form.startDate}</Th>
              <Th>{i18n.common.form.endDate}</Th>
              <Th>{i18n.common.form.lastModified}</Th>
              <Th />
            </Tr>
          </Thead>
          <Tbody>
            {orderBy(
              parentships,
              ['startDate', 'endDate'],
              ['desc', 'desc']
            ).map(
              (
                {
                  data: fridgeChild,
                  permittedActions
                }: ParentshipWithPermittedActions,
                i: number
              ) => {
                const modifiedAt =
                  fridgeChild.creationModificationMetadata.modifiedAt ||
                  fridgeChild.creationModificationMetadata.createdAt
                const modifiedByName =
                  fridgeChild.creationModificationMetadata.modifiedByName ||
                  fridgeChild.creationModificationMetadata.createdByName

                return (
                  <Tr
                    key={`${fridgeChild.child.id}-${i}`}
                    data-qa="table-fridge-child-row"
                  >
                    <NameTd>
                      <Link to={`/child-information/${fridgeChild.child.id}`}>
                        {formatName(
                          fridgeChild.child.firstName,
                          fridgeChild.child.lastName,
                          i18n,
                          true
                        )}
                      </Link>
                    </NameTd>
                    <Td>
                      {fridgeChild.child.socialSecurityNumber ??
                        fridgeChild.child.dateOfBirth.format()}
                    </Td>
                    <Td data-qa="child-age">
                      {getAge(fridgeChild.child.dateOfBirth)}
                    </Td>
                    <DateTd>{fridgeChild.startDate.format()}</DateTd>
                    <DateTd>{fridgeChild.endDate.format()}</DateTd>
                    <Td>
                      {modifiedAt ? (
                        <Tooltip
                          tooltip={
                            modifiedByName
                              ? i18n.common.form.lastModifiedBy(modifiedByName)
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
                        dateRange={fridgeChild}
                        onRetry={
                          fridgeChild.conflict
                            ? () => {
                                void retryParentship({
                                  headOfChildId: id,
                                  id: fridgeChild.id
                                })
                              }
                            : undefined
                        }
                        editable={permittedActions.includes('UPDATE')}
                        onEdit={() => {
                          setSelectedParentshipId(fridgeChild.id)
                          toggleUiMode(`edit-fridge-child-${fridgeChild.id}`)
                        }}
                        deletable={
                          fridgeChild.conflict
                            ? permittedActions.includes(
                                'DELETE_CONFLICTED_PARENTSHIP'
                              )
                            : permittedActions.includes('DELETE')
                        }
                        onDelete={() => {
                          setSelectedParentshipId(fridgeChild.id)
                          toggleUiMode(`remove-fridge-child-${fridgeChild.id}`)
                        }}
                        conflict={fridgeChild.conflict}
                      />
                    </ButtonsTd>
                  </Tr>
                )
              }
            )}
          </Tbody>
        </Table>
      ))}
    </div>
  )
})
