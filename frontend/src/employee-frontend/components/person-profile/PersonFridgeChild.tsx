// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as _ from 'lodash'
import React, { useContext, useState } from 'react'
import { Link } from 'react-router-dom'

import { Parentship } from 'lib-common/generated/api-types/pis'
import { UUID } from 'lib-common/types'
import { getAge } from 'lib-common/utils/local-date'
import { useApiState } from 'lib-common/utils/useRestApi'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { faChild, faQuestion } from 'lib-icons'

import {
  getParentshipsByHeadOfChild,
  removeParentship,
  retryParentship
} from '../../api/parentships'
import Toolbar from '../../components/common/Toolbar'
import FridgeChildModal from '../../components/person-profile/person-fridge-child/FridgeChildModal'
import { useTranslation } from '../../state/i18n'
import { PersonContext } from '../../state/person'
import { UIContext } from '../../state/ui'
import { formatName } from '../../utils'
import { ButtonsTd, DateTd, NameTd } from '../PersonProfile'
import { renderResult } from '../async-rendering'

interface Props {
  id: UUID
  open: boolean
}

export default React.memo(function PersonFridgeChild({ id, open }: Props) {
  const { i18n } = useTranslation()
  const { reloadFamily } = useContext(PersonContext)
  const { uiMode, toggleUiMode, clearUiMode, setErrorMessage } =
    useContext(UIContext)
  const [parentships, loadData] = useApiState(
    () => getParentshipsByHeadOfChild(id),
    [id]
  )
  const [selectedParentshipId, setSelectedParentshipId] = useState('')

  // FIXME: This component shouldn't know about family's dependency on its data
  const reload = () => {
    loadData()
    reloadFamily()
  }

  const getFridgeChildById = (id: UUID) => {
    return parentships
      .map((ps) => ps.find((child) => child.id === id))
      .getOrElse(undefined)
  }

  return (
    <div>
      {uiMode === 'add-fridge-child' ? (
        <FridgeChildModal headPersonId={id} onSuccess={reload} />
      ) : uiMode === `edit-fridge-child-${selectedParentshipId}` ? (
        <FridgeChildModal
          parentship={getFridgeChildById(selectedParentshipId)}
          headPersonId={id}
          onSuccess={reload}
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
              removeParentship(selectedParentshipId).then((res) => {
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
              }),
            label: i18n.common.remove
          }}
        />
      ) : null}
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
        {renderResult(parentships, (parentships) => (
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
              {_.orderBy(
                parentships,
                ['startDate', 'endDate'],
                ['desc', 'desc']
              ).map((fridgeChild: Parentship, i: number) => (
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
              ))}
            </Tbody>
          </Table>
        ))}
      </CollapsibleSection>
    </div>
  )
})
