import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import {
  getSelectorStatus,
  isChildSelectorNode,
  SelectorChange,
  SelectorNode,
  updateSelector
} from 'employee-frontend/components/messages/SelectorNode'
import { useTranslation } from 'employee-frontend/state/i18n'
import { UUID } from 'employee-frontend/types'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Table, Thead, Tr, Th, Tbody, Td } from 'lib-components/layout/Table'
import { H1, H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faAngleUp, faAngleDown } from 'lib-icons'
import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { ContentArea } from '../../../lib-components/layout/Container'

const Container = styled(ContentArea)`
  overflow-y: auto;
  flex: 1;
`

const IconWrapper = styled.span`
  margin-right: ${defaultMargins.s};
`

interface Props {
  selectedReceivers: SelectorNode
  setSelectedReceivers: React.Dispatch<
    React.SetStateAction<SelectorNode | undefined>
  >
}

export default React.memo(function ReceiverSelection({
  selectedReceivers,
  setSelectedReceivers
}: Props) {
  const { i18n } = useTranslation()

  const [collapsedGroups, setCollapsedGroups] = useState<UUID[]>([])
  const [selectorChange, setSelectorChange] = useState<SelectorChange>()

  const isCollapsed = (groupId: UUID) => collapsedGroups.indexOf(groupId) > -1

  const setCollapsed = (groupId: UUID) =>
    setCollapsedGroups((old: UUID[]) => [...old, groupId])

  const setNotCollapsed = (groupId: UUID) =>
    setCollapsedGroups((old: UUID[]) =>
      old.filter((id: string) => id !== groupId)
    )

  useEffect(() => {
    if (selectorChange) {
      setSelectedReceivers((selectedReceivers: SelectorNode | undefined) =>
        selectedReceivers
          ? updateSelector(selectedReceivers, selectorChange)
          : undefined
      )
    }
  }, [selectorChange, setSelectedReceivers])

  const toggleCollapsed = (groupId: UUID) =>
    isCollapsed(groupId) ? setNotCollapsed(groupId) : setCollapsed(groupId)

  const { name: unitName, selectorId: unitId } = selectedReceivers

  return (
    <Container opaque>
      <H1>{i18n.messages.receiverSelection.title}</H1>
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
        {selectedReceivers.childNodes.map((receiverGroup: SelectorNode) => (
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
            {!isCollapsed(receiverGroup.selectorId) &&
              receiverGroup.childNodes.map((receiverChild: SelectorNode) => {
                if (!isChildSelectorNode(receiverChild))
                  throw new Error('data mapping error: missing child data')
                return (
                  <Tr key={receiverChild.selectorId}>
                    <Td>{receiverChild.name}</Td>
                    <Td>{receiverChild.dateOfBirth.format()}</Td>
                    <Td>
                      {receiverChild.childNodes.map(({ name, selectorId }) => (
                        <div key={selectorId}>{name}</div>
                      ))}
                    </Td>
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
                )
              })}
          </Tbody>
        ))}
      </Table>
    </Container>
  )
})
