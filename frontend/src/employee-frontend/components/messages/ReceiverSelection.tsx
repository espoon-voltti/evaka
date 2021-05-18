import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import {
  getSelectorStatus,
  SelectorChange,
  SelectorNode
} from 'employee-frontend/components/messages/SelectorNode'
import { useTranslation } from 'employee-frontend/state/i18n'
import { UUID } from 'employee-frontend/types'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Table, Thead, Tr, Th, Tbody, Td } from 'lib-components/layout/Table'
import { H1, H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faAngleUp, faAngleDown } from 'lib-icons'
import React, { useState } from 'react'
import styled from 'styled-components'

interface Props {
  selectedReceivers: SelectorNode
  setSelectorChange: (change: SelectorChange) => void
}

export default React.memo(function ReceiverSelection({
  selectedReceivers,
  setSelectorChange
}: Props) {
  const { i18n } = useTranslation()

  const [collapsedGroups, setCollapsedGroups] = useState<UUID[]>([])

  const isCollapsed = (groupId: UUID) => collapsedGroups.indexOf(groupId) > -1

  const setCollapsed = (groupId: UUID) =>
    setCollapsedGroups((old: UUID[]) => [...old, groupId])

  const setNotCollapsed = (groupId: UUID) =>
    setCollapsedGroups((old: UUID[]) =>
      old.filter((id: string) => id !== groupId)
    )

  const toggleCollapsed = (groupId: UUID) =>
    isCollapsed(groupId) ? setNotCollapsed(groupId) : setCollapsed(groupId)

  const unitName = selectedReceivers.name
  const unitId = selectedReceivers.selectorId

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
                checked={getSelectorStatus(unitId, selectedReceivers)}
                onChange={(checked: boolean) =>
                  setSelectorChange({ selectorId: unitId, selected: checked })
                }
              />
            </Th>
          </Tr>
        </Thead>
        {selectedReceivers.childNodes?.map((receiverGroup: SelectorNode) => (
          <Tbody key={receiverGroup.selectorId}>
            <Tr>
              <Td
                colSpan={3}
                onClick={() => toggleCollapsed(receiverGroup.selectorId)}
              >
                <IconWrapper>
                  <FontAwesomeIcon
                    icon={
                      isCollapsed(receiverGroup.selectorId)
                        ? faAngleUp
                        : faAngleDown
                    }
                  />
                </IconWrapper>
                <strong>{receiverGroup.name}</strong>
              </Td>
              <Td>
                <Checkbox
                  label={''}
                  checked={getSelectorStatus(
                    receiverGroup.selectorId,
                    selectedReceivers
                  )}
                  onChange={(checked: boolean) =>
                    setSelectorChange({
                      selectorId: receiverGroup.selectorId,
                      selected: checked
                    })
                  }
                />
              </Td>
            </Tr>
            {receiverGroup.childNodes &&
              !isCollapsed(receiverGroup.selectorId) &&
              receiverGroup?.childNodes.map((receiverChild: SelectorNode) => (
                <Tr key={receiverChild.selectorId}>
                  <Td>{receiverChild.name}</Td>
                  <Td>
                    <Checkbox
                      label={''}
                      checked={getSelectorStatus(
                        receiverChild.selectorId,
                        selectedReceivers
                      )}
                      onChange={(checked: boolean) =>
                        setSelectorChange({
                          selectorId: receiverChild.selectorId,
                          selected: checked
                        })
                      }
                      data-qa={`check-receiver-${receiverChild.selectorId}`}
                    />
                  </Td>
                </Tr>
              ))}
          </Tbody>
        ))}
      </Table>
    </Container>
  )
})

const Container = styled.div`
  flex-grow: 1;
  min-height: 500px;
  overflow-y: auto;
  padding: ${defaultMargins.m};
  display: flex;
  flex-direction: column;
`

const IconWrapper = styled.span`
  margin-right: ${defaultMargins.s};
`
