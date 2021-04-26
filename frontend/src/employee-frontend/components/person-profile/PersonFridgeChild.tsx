// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { UUID } from '../../types'
import { useTranslation } from '../../state/i18n'
import { useEffect } from 'react'
import { Loading, Result } from 'lib-common/api'
import { useContext } from 'react'
import { PersonContext } from '../../state/person'
import { formatName } from '../../utils'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import Loader from 'lib-components/atoms/Loader'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { Parentship } from '../../types/fridge'
import * as _ from 'lodash'
import { faChild, faQuestion } from 'lib-icons'
import { UIContext } from '../../state/ui'
import FridgeChildModal from '../../components/person-profile/person-fridge-child/FridgeChildModal'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Link } from 'react-router-dom'
import {
  getParentshipsByHeadOfChild,
  removeParentship,
  retryParentship
} from '../../api/parentships'
import { ButtonsTd, DateTd, NameTd } from '../../components/PersonProfile'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import Toolbar from '../../components/common/Toolbar'
import { getAge } from 'lib-common/utils/local-date'

interface Props {
  id: UUID
  open: boolean
}
const PersonFridgeChild = React.memo(function PersonFridgeChild({
  id,
  open
}: Props) {
  const { i18n } = useTranslation()
  const { parentships, setParentships, reloadFamily } = useContext(
    PersonContext
  )
  const { uiMode, toggleUiMode, clearUiMode, setErrorMessage } = useContext(
    UIContext
  )
  const [selectedParentshipId, setSelectedParentshipId] = useState('')

  const loadData = () => {
    setParentships(Loading.of())
    void getParentshipsByHeadOfChild(id).then(setParentships)
  }

  // FIXME: This component shouldn't know about family's dependency on its data
  const reload = () => {
    loadData()
    reloadFamily(id)
  }

  useEffect(loadData, [id, setParentships])

  const getFridgeChildById = (id: UUID) => {
    return parentships
      .map((ps) => ps.find((child) => child.id === id))
      .getOrElse(undefined)
  }

  const renderFridgeChildModal = () => {
    if (uiMode === 'add-fridge-child') {
      return <FridgeChildModal headPersonId={id} onSuccess={reload} />
    } else if (uiMode === `edit-fridge-child-${selectedParentshipId}`) {
      return (
        <FridgeChildModal
          parentship={getFridgeChildById(selectedParentshipId)}
          headPersonId={id}
          onSuccess={reload}
        />
      )
    } else if (uiMode === `remove-fridge-child-${selectedParentshipId}`) {
      return (
        <InfoModal
          iconColour={'orange'}
          title={i18n.personProfile.fridgeChild.removeChild}
          text={i18n.personProfile.fridgeChild.confirmText}
          icon={faQuestion}
          reject={{ action: () => clearUiMode(), label: i18n.common.cancel }}
          resolve={{
            action: () =>
              removeParentship(selectedParentshipId).then(
                (res: Result<null>) => {
                  clearUiMode()
                  if (res.isFailure) {
                    setErrorMessage({
                      type: 'error',
                      title: i18n.personProfile.fridgeChild.error.remove.title,
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

  const renderFridgeChildren = () =>
    parentships.isSuccess
      ? _.orderBy(
          parentships.value,
          ['startDate', 'endDate'],
          ['desc', 'desc']
        ).map((fridgeChild: Parentship, i: number) => {
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
                    i18n
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
                          void retryParentship(fridgeChild.id).then(() =>
                            reload()
                          )
                        }
                      : undefined
                  }
                  onEdit={() => {
                    setSelectedParentshipId(fridgeChild.id)
                    toggleUiMode(`edit-fridge-child-${fridgeChild.id}`)
                  }}
                  onDelete={() => {
                    setSelectedParentshipId(fridgeChild.id)
                    toggleUiMode(`remove-fridge-child-${fridgeChild.id}`)
                  }}
                  conflict={fridgeChild.conflict}
                  deletableFor={
                    fridgeChild.conflict
                      ? ['ADMIN', 'FINANCE_ADMIN', 'UNIT_SUPERVISOR']
                      : ['ADMIN', 'FINANCE_ADMIN']
                  }
                />
              </ButtonsTd>
            </Tr>
          )
        })
      : null

  return (
    <div>
      {renderFridgeChildModal()}
      <CollapsibleSection
        icon={faChild}
        title={i18n.personProfile.fridgeChildOfHead}
        startCollapsed={!open}
        data-qa="person-children-collapsible"
      >
        <AddButtonRow
          text={i18n.personProfile.fridgeChildAdd}
          onClick={() => {
            toggleUiMode('add-fridge-child')
          }}
          data-qa="add-child-button"
        />
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
          <Tbody>{renderFridgeChildren()}</Tbody>
        </Table>
        {parentships.isLoading && <Loader />}
        {parentships.isFailure && <div>{i18n.common.loadingFailed}</div>}
      </CollapsibleSection>
    </div>
  )
})

export default PersonFridgeChild
