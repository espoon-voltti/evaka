/// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ReceiverGroup } from 'employee-frontend/components/messages/types'
import { UUID } from 'employee-frontend/types'

/*
    SelectorNode is a recursive type for storing a selectorId, the corresponding selected
    state and (possibly) child objects of the same type. It updates according to the following rules:

    1. If a SelectorNode is selected, each child node will also be selected.
    2. If a SelectorNode is unselected, each child node will also be unselected.
    3. If each child node of a SelectorNode is selected, it will also be selected.
    4. If a child node of a SelectorNode is unselected, it will also be unselected.
*/
export interface SelectorNode {
  selectorId: UUID
  selected: boolean
  name: string
  childNodes?: SelectorNode[]
}

export interface SelectorChange {
  selectorId: UUID
  selected: boolean
}

interface ReactSelectOption {
  label: string
  value: string
}

export const getSelectorStatus = (id: UUID, selector: SelectorNode): boolean =>
  id === selector.selectorId
    ? selector.selected
    : !!selector.childNodes
        ?.map((childNode) => getSelectorStatus(id, childNode))
        .find((x) => x === true)

export const getSelectorName = (
  id: UUID,
  selector: SelectorNode
): string | undefined =>
  id === selector.selectorId
    ? selector.name
    : selector.childNodes
        ?.map((childNode) => getSelectorName(id, childNode))
        .find((x) => x && x.length > 0)

export const updateSelector = (
  selector: SelectorNode,
  selectorChange: SelectorChange
): SelectorNode => {
  if (selector.selectorId === selectorChange.selectorId) {
    return {
      ...selectorChange,
      name: selector.name,
      childNodes: selector.childNodes?.map((childNode) =>
        updateSelector(childNode, {
          selectorId: childNode.selectorId,
          selected: selectorChange.selected
        })
      )
    }
  } else if (selector.childNodes) {
    const updatedChildNodes = selector.childNodes.map((childNode) =>
      updateSelector(childNode, selectorChange)
    )
    const allChildNodesSelected = !updatedChildNodes
      .map((childNode) => childNode.selected)
      .includes(false)
    return {
      selectorId: selector.selectorId,
      selected: allChildNodesSelected,
      name: selector.name,
      childNodes: updatedChildNodes
    }
  } else {
    return selector
  }
}

export const getReceiverOptions = (
  selectorNode: SelectorNode
): ReactSelectOption[] => {
  return (selectorNode.childNodes
    ? [{ label: selectorNode.name, value: selectorNode.selectorId }]
    : []
  ).concat(
    selectorNode.childNodes && !selectorNode.selected
      ? selectorNode.childNodes.flatMap((childNode) =>
          getReceiverOptions(childNode)
        )
      : []
  )
}

export const deselectAll = (selectorNode: SelectorNode): SelectorNode => {
  return {
    ...selectorNode,
    selected: false,
    childNodes: selectorNode.childNodes?.map((childNode) =>
      deselectAll(childNode)
    )
  }
}

export const unitAsSelectorNode = (
  { id, name }: { id: UUID; name: string },
  receiverGroups: ReceiverGroup[]
): SelectorNode => ({
  selectorId: id,
  selected: false,
  name,
  childNodes: receiverGroups.map((group: ReceiverGroup) => ({
    selectorId: group.groupId,
    selected: false,
    name: group.groupName,
    childNodes: group.receivers.map(
      ({ childId, childFirstName, childLastName, receiverPersons }) => ({
        selectorId: childId,
        selected: false,
        name: `${childFirstName} ${childLastName}`,
        childNodes: receiverPersons.map(
          ({ accountId, receiverFirstName, receiverLastName }) => ({
            selectorId: accountId,
            selected: false,
            name: `${receiverFirstName} ${receiverLastName}`
          })
        )
      })
    )
  }))
})

export const getSelectedBottomElements = (selector: SelectorNode): UUID[] => {
  if (selector.childNodes) {
    return [...new Set(selector.childNodes.flatMap(getSelectedBottomElements))]
  } else {
    return selector.selected ? [selector.selectorId] : []
  }
}

export const getSelected = (selector: SelectorNode): ReactSelectOption[] => {
  if (!selector.selected) {
    return selector.childNodes ? selector.childNodes.flatMap(getSelected) : []
  } else {
    return [{ value: selector.selectorId, label: selector.name }]
  }
}
