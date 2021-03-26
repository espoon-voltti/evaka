// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ReceiverTriplet } from 'employee-frontend/components/messages/types'
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
  childNodes?: SelectorNode[]
}

export const getSelectorStatus = (id: UUID, selector: SelectorNode): boolean =>
  id === selector.selectorId
    ? selector.selected
    : !!selector.childNodes
        ?.map((childNode) => getSelectorStatus(id, childNode))
        .find((x) => x === true)

export const updateSelector = (
  selector: SelectorNode,
  selectorChange: SelectorNode
): SelectorNode => {
  if (selector.selectorId === selectorChange.selectorId) {
    return {
      ...selectorChange,
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
      childNodes: updatedChildNodes
    }
  } else {
    return selector
  }
}

export const getReceiverTriplets = (
  selectorNode: SelectorNode
): ReceiverTriplet[] => {
  const traverse = (node: SelectorNode, acc: UUID[]): UUID[][] => {
    if (node.selected) {
      return [[...acc, node.selectorId]]
    } else {
      return (
        node.childNodes?.flatMap((childNode) =>
          traverse(childNode, [...acc, node.selectorId])
        ) || []
      )
    }
  }
  return traverse(selectorNode, []).map((x) => ({
    unitId: x[0],
    groupId: x[1],
    personId: x[2]
  }))
}
