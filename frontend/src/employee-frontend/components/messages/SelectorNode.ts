/// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ReceiverGroup } from 'employee-frontend/components/messages/types'
import { UUID } from 'employee-frontend/types'
import LocalDate from 'lib-common/local-date'

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
  childNodes: SelectorNode[]
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
    : selector.childNodes.some((childNode) => getSelectorStatus(id, childNode))

export const getSelectorName = (
  id: UUID,
  selector: SelectorNode
): string | undefined =>
  id === selector.selectorId
    ? selector.name
    : selector.childNodes
        .map((childNode) => getSelectorName(id, childNode))
        .find((x) => x)

export const updateSelector = (
  selector: SelectorNode,
  selectorChange: SelectorChange
): SelectorNode => {
  if (selector.selectorId === selectorChange.selectorId) {
    return {
      ...selector,
      selected: selectorChange.selected,
      childNodes: selector.childNodes.map((childNode) =>
        updateSelector(childNode, {
          selectorId: childNode.selectorId,
          selected: selectorChange.selected
        })
      )
    }
  } else if (selector.childNodes.length > 0) {
    const updatedChildNodes = selector.childNodes.map((childNode) =>
      updateSelector(childNode, selectorChange)
    )
    const allChildNodesSelected = updatedChildNodes.every(
      ({ selected }) => selected
    )
    return {
      ...selector,
      selected: allChildNodesSelected,
      childNodes: updatedChildNodes
    }
  } else {
    return selector
  }
}

export const getReceiverOptions = (
  selectorNode: SelectorNode
): ReactSelectOption[] => {
  return !selectorNode.selected && selectorNode.childNodes.length > 0
    ? [{ label: selectorNode.name, value: selectorNode.selectorId }].concat(
        selectorNode.childNodes.flatMap((childNode: SelectorNode) =>
          getReceiverOptions(childNode)
        )
      )
    : []
}

export const deselectAll = (selectorNode: SelectorNode): SelectorNode => {
  return {
    ...selectorNode,
    selected: false,
    childNodes: selectorNode.childNodes.map((childNode) =>
      deselectAll(childNode)
    )
  }
}

export interface ChildSelectorNode extends SelectorNode {
  dateOfBirth: LocalDate
}
export const isChildSelectorNode = (
  node: SelectorNode
): node is ChildSelectorNode => 'dateOfBirth' in node

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
    childNodes: group.receivers.map<ChildSelectorNode>(
      ({
        childId,
        childFirstName,
        childLastName,
        childDateOfBirth,
        receiverPersons
      }) => ({
        selectorId: childId,
        selected: false,
        name: `${childFirstName} ${childLastName}`,
        dateOfBirth: childDateOfBirth,
        childNodes: receiverPersons.map(
          ({ accountId, receiverFirstName, receiverLastName }) => ({
            selectorId: accountId,
            selected: false,
            name: `${receiverFirstName} ${receiverLastName}`,
            childNodes: []
          })
        )
      })
    )
  }))
})

export const getSelectedBottomElements = (selector: SelectorNode): UUID[] => {
  if (selector.childNodes.length > 0) {
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

export const getSubTree = (
  selector: SelectorNode,
  selectorId: UUID
): SelectorNode | undefined => {
  if (selector.selectorId === selectorId) {
    return selector
  } else {
    for (const child of selector.childNodes) {
      const out = getSubTree(child, selectorId)
      if (out) {
        return out
      }
    }
  }
  return undefined
}
