// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import { UUID } from '~types'
import { useTranslation } from '~state/i18n'
import { useEffect } from 'react'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import { useContext } from 'react'
import { PersonContext } from '~state/person'
import { formatName } from '~utils'
import { Collapsible, Loader, Table } from '~components/shared/alpha'
import { Parentship } from '~types/fridge'
import * as _ from 'lodash'
import { faChild, faQuestion } from 'icon-set'
import { UIContext } from '~state/ui'
import FridgeChildModal from '~components/person-profile/person-fridge-child/FridgeChildModal'
import InfoModal from '~components/common/InfoModal'
import { Link } from 'react-router-dom'
import {
  getParentshipsByHeadOfChild,
  removeParentship,
  retryParentship
} from '~api/parentships'
import { ButtonsTd, DateTd, NameTd } from '~components/PersonProfile'
import { AddButtonRow } from 'components/shared/atoms/buttons/AddButton'
import Toolbar from 'components/shared/molecules/Toolbar'
import { getAge } from '@evaka/lib-common/src/utils/local-date'

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
  const [toggled, setToggled] = useState(open)
  const toggle = useCallback(() => setToggled((toggled) => !toggled), [
    setToggled
  ])

  const loadData = () => {
    setParentships(Loading())
    void getParentshipsByHeadOfChild(id).then(setParentships)
  }

  // FIXME: This component shouldn't know about family's dependency on its data
  const reload = () => {
    loadData()
    reloadFamily(id)
  }

  useEffect(loadData, [id, setParentships])

  const getFridgeChildById = (id: UUID) => {
    return isSuccess(parentships)
      ? parentships.data.find((child) => child.id === id)
      : undefined
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
          resolveLabel={i18n.common.remove}
          rejectLabel={i18n.common.cancel}
          icon={faQuestion}
          reject={() => clearUiMode()}
          resolve={() =>
            removeParentship(selectedParentshipId).then((res: Result<null>) => {
              clearUiMode()
              if (isFailure(res)) {
                setErrorMessage({
                  type: 'error',
                  title: i18n.personProfile.fridgeChild.error.remove.title,
                  text: i18n.common.tryAgain
                })
              } else {
                reload()
              }
            })
          }
        />
      )
    }
    return
  }

  const renderFridgeChildren = () =>
    isSuccess(parentships)
      ? _.orderBy(
          parentships.data,
          ['startDate', 'endDate'],
          ['desc', 'desc']
        ).map((fridgeChild: Parentship, i: number) => {
          return (
            <Table.Row
              key={`${fridgeChild.child.id}-${i}`}
              dataQa="table-fridge-child-row"
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
              <Table.Td>
                {fridgeChild.child.socialSecurityNumber ??
                  fridgeChild.child.dateOfBirth.format()}
              </Table.Td>
              <Table.Td dataQa="child-age">
                {getAge(fridgeChild.child.dateOfBirth)}
              </Table.Td>
              <DateTd>{fridgeChild.startDate.format()}</DateTd>
              <DateTd>{fridgeChild.endDate?.format()}</DateTd>
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
                />
              </ButtonsTd>
            </Table.Row>
          )
        })
      : null

  return (
    <div>
      {renderFridgeChildModal()}
      <Collapsible
        icon={faChild}
        title={i18n.personProfile.fridgeChildOfHead}
        open={toggled}
        onToggle={toggle}
        dataQa="person-children-collapsible"
      >
        <AddButtonRow
          text={i18n.personProfile.fridgeChildAdd}
          onClick={() => {
            toggleUiMode('add-fridge-child')
          }}
          data-qa="add-child-button"
        />
        <Table.Table dataQa="table-of-children">
          <Table.Head>
            <Table.Row>
              <Table.Th>{i18n.common.form.name}</Table.Th>
              <Table.Th>{i18n.common.form.socialSecurityNumber}</Table.Th>
              <Table.Th>{i18n.common.form.age}</Table.Th>
              <Table.Th>{i18n.common.form.startDate}</Table.Th>
              <Table.Th>{i18n.common.form.endDate}</Table.Th>
              <Table.Th />
            </Table.Row>
          </Table.Head>
          <Table.Body>{renderFridgeChildren()}</Table.Body>
        </Table.Table>
        {isLoading(parentships) && <Loader />}
        {isFailure(parentships) && <div>{i18n.common.loadingFailed}</div>}
      </Collapsible>
    </div>
  )
})

export default PersonFridgeChild
