// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext, useState } from 'react'
import { Link } from 'react-router-dom'

import { wrapResult } from 'lib-common/api'
import { ParentshipWithPermittedActions } from 'lib-common/generated/api-types/pis'
import { UUID } from 'lib-common/types'
import { getAge } from 'lib-common/utils/local-date'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2 } from 'lib-components/typography'
import { faQuestion } from 'lib-icons'

import Toolbar from '../../components/common/Toolbar'
import FridgeChildModal from '../../components/person-profile/person-fridge-child/FridgeChildModal'
import {
  deleteParentship,
  retryParentship
} from '../../generated/api-clients/pis'
import { useTranslation } from '../../state/i18n'
import { PersonContext } from '../../state/person'
import { UIContext } from '../../state/ui'
import { formatName } from '../../utils'
import { ButtonsTd, DateTd, NameTd } from '../PersonProfile'
import { renderResult } from '../async-rendering'

const deleteParentshipResult = wrapResult(deleteParentship)
const retryParentshipResult = wrapResult(retryParentship)

interface Props {
  id: UUID
  open: boolean
}

export default React.memo(function PersonFridgeChild({
  id,
  open: startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { fridgeChildren, reloadFridgeChildren, permittedActions } =
    useContext(PersonContext)
  const { uiMode, toggleUiMode, clearUiMode, setErrorMessage } =
    useContext(UIContext)
  const [open, setOpen] = useState(startOpen)
  const [selectedParentshipId, setSelectedParentshipId] = useState('')

  const getFridgeChildById = (id: UUID) =>
    fridgeChildren
      .map((ps) => ps.find(({ data }) => data.id === id)?.data)
      .getOrElse(undefined)

  return (
    <div>
      {uiMode === 'add-fridge-child' ? (
        <FridgeChildModal headPersonId={id} onSuccess={reloadFridgeChildren} />
      ) : uiMode === `edit-fridge-child-${selectedParentshipId}` ? (
        <FridgeChildModal
          parentship={getFridgeChildById(selectedParentshipId)}
          headPersonId={id}
          onSuccess={reloadFridgeChildren}
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
              deleteParentshipResult({ id: selectedParentshipId }).then(
                (res) => {
                  clearUiMode()
                  if (res.isFailure) {
                    setErrorMessage({
                      type: 'error',
                      title: i18n.personProfile.fridgeChild.error.remove.title,
                      text: i18n.common.tryAgain,
                      resolveLabel: i18n.common.ok
                    })
                  } else {
                    reloadFridgeChildren()
                  }
                }
              ),
            label: i18n.common.remove
          }}
        />
      ) : null}
      <CollapsibleContentArea
        title={<H2>{i18n.personProfile.fridgeChildOfHead}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="person-children-collapsible"
      >
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
                ) => (
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
                    <ButtonsTd>
                      <Toolbar
                        dateRange={fridgeChild}
                        onRetry={
                          fridgeChild.conflict
                            ? () => {
                                void retryParentshipResult({
                                  id: fridgeChild.id
                                }).then(reloadFridgeChildren)
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
              )}
            </Tbody>
          </Table>
        ))}
      </CollapsibleContentArea>
    </div>
  )
})
