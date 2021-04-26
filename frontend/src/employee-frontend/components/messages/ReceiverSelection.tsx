{
  /*
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React, { useEffect, useState } from 'react'
import { UUID } from 'employee-frontend/types'
import styled from 'styled-components'
import { H1, H2 } from 'lib-components/typography'
import { Table, Tr, Th, Td, Thead, Tbody } from 'lib-components/layout/Table'
import { defaultMargins } from 'lib-components/white-space'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Button from 'lib-components/atoms/buttons/Button'
import colors from 'lib-components/colors'
import { ReceiverGroup } from './types'
import { useTranslation } from '../../state/i18n'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faAngleDown, faAngleUp } from 'lib-icons'
import { SelectorChange } from 'employee-frontend/components/messages/receiver-selection-utils'

interface Props {
  unitId: UUID
  unitName: string
  onCreateNew: () => void
  receivers: ReceiverGroup[]
  isSelected: (id: UUID) => boolean
  updateSelection: (selectorChange: SelectorChange) => void
}

export default React.memo(function ReceiverSelection({
  unitId,
  unitName,
  onCreateNew,
  isSelected,
  updateSelection,
  receivers
}: Props) {
  const { i18n } = useTranslation()

  const [collapsedGroups, setCollapsedGroups] = useState<UUID[]>([])

  const isCollapsed = (groupId: UUID) => collapsedGroups.indexOf(groupId) > -1

  const setCollapsed = (groupId: UUID) =>
    setCollapsedGroups((old) => [...old, groupId])

  const setNotCollapsed = (groupId: UUID) =>
    setCollapsedGroups((old) => old.filter((id) => id !== groupId))

  const toggleCollapsed = (groupId: UUID) =>
    isCollapsed(groupId) ? setNotCollapsed(groupId) : setCollapsed(groupId)

  const [selectorChange, setSelectorChange] = useState<SelectorChange>()

  useEffect(() => {
    if (selectorChange) {
      updateSelection(selectorChange)
    }
  }, [selectorChange])

  return (
    <Container>
      <H1 noMargin>{i18n.messages.receiverSelection.title}</H1>
      <H2>{unitName}</H2>
      <Table>
        <Thead>
          <Tr>
            <Th>{i18n.messages.receiverSelection.childName}</Th>
            <Th>{i18n.messages.receiverSelection.childDob}</Th>
            <Th>{i18n.messages.receiverSelection.receivers}</Th>
            <Th>
              <Checkbox
                label={''}
                checked={isSelected(unitId)}
                onChange={(checked) =>
                  setSelectorChange({ selectorId: unitId, selected: checked })
                }
              />
            </Th>
          </Tr>
        </Thead>
        {receivers.map((receiverGroup) => (
          <Tbody key={receiverGroup.groupId}>
            <Tr>
              <Td
                colSpan={3}
                onClick={() => toggleCollapsed(receiverGroup.groupId)}
              >
                <IconWrapper>
                  <FontAwesomeIcon
                    icon={
                      isCollapsed(receiverGroup.groupId)
                        ? faAngleUp
                        : faAngleDown
                    }
                  />
                </IconWrapper>
                <strong>{receiverGroup.groupName}</strong>
              </Td>
              <Td>
                <Checkbox
                  label={''}
                  checked={isSelected(receiverGroup.groupId)}
                  onChange={(checked) =>
                    setSelectorChange({
                      selectorId: receiverGroup.groupId,
                      selected: checked
                    })
                  }
                />
              </Td>
            </Tr>
            {!isCollapsed(receiverGroup.groupId) &&
              receiverGroup.receivers.map((receiverChild) => (
                <Tr key={receiverChild.childId}>
                  <Td>
                    {receiverChild.childFirstName} {receiverChild.childLastName}
                  </Td>
                  <Td>{receiverChild.childDateOfBirth.format()}</Td>
                  <Td>
                    {receiverChild.receiverPersons.map(
                      ({ receiverId, receiverFirstName, receiverLastName }) => (
                        <div key={receiverId}>
                          {receiverFirstName} {receiverLastName}
                        </div>
                      )
                    )}
                  </Td>
                  <Td>
                    <Checkbox
                      label={''}
                      checked={isSelected(receiverChild.childId)}
                      onChange={(checked) =>
                        setSelectorChange({
                          selectorId: receiverChild.childId,
                          selected: checked
                        })
                      }
                      data-qa={`check-receiver-${receiverChild.childId}`}
                    />
                  </Td>
                </Tr>
              ))}
          </Tbody>
        ))}
      </Table>
      <Button
        text={i18n.messages.receiverSelection.confirmText}
        primary
        onClick={() => onCreateNew()}
      />
    </Container>
  )
})

const Container = styled.div`
  flex-grow: 1;
  min-height: 500px;
  overflow-y: auto;
  padding: ${defaultMargins.m};
  background-color: ${colors.greyscale.white};
  display: flex;
  flex-direction: column;
`

const IconWrapper = styled.span`
  color: ${colors.greyscale.medium};
  margin-right: ${defaultMargins.s};
`
