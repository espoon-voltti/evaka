// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  MessageReceiver,
  MessageReceiversResponse,
  MessageRecipient
} from 'lib-common/generated/api-types/messaging'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

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
  messageRecipient: MessageRecipient
  childNodes: SelectorNode[]
}

export interface SelectorChange {
  selectorId: UUID
  selected: boolean
}

export interface ReactSelectOption {
  label: string
  value: string
}

export const isShallow = (s: SelectorNode): boolean =>
  s.childNodes.every((cn) => cn.childNodes.length === 0)

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
  selectorNodes: SelectorNode[]
): ReactSelectOption[] =>
  selectorNodes.flatMap((selectorNode) => {
    return !selectorNode.selected
      ? [asSelectOption(selectorNode)].concat(
          getReceiverOptions(selectorNode.childNodes)
        )
      : []
  })

export const deselectAll = (selectorNodes: SelectorNode[]): SelectorNode[] =>
  selectorNodes.map((node) => ({
    ...node,
    selected: false,
    childNodes: deselectAll(node.childNodes)
  }))

export interface ChildSelectorNode extends SelectorNode {
  dateOfBirth: LocalDate
}

export const isChildSelectorNode = (
  node: SelectorNode
): node is ChildSelectorNode => 'dateOfBirth' in node

export const receiversAsSelectorNode = (
  accountId: UUID,
  receivers: MessageReceiversResponse[]
): SelectorNode[] => {
  const accountReceivers = receivers.find(
    (receiver) => receiver.accountId === accountId
  )?.receivers

  if (!accountReceivers) {
    return []
  }

  const selectorNodes = accountReceivers.map(receiverAsSelectorNode)

  if (selectorNodes.length === 1 && selectorNodes[0].childNodes.length === 0) {
    return selectorNodes.map((node) => ({ ...node, selected: true }))
  }

  return selectorNodes
}

const receiverAsSelectorNode = (receiver: MessageReceiver): SelectorNode => ({
  selectorId: receiver.id,
  selected: false,
  name: receiver.name,
  messageRecipient: { type: receiver.type, id: receiver.id },
  childNodes: receiver.receivers.map(receiverAsSelectorNode)
})

export const getSelectedBottomElements = (selector: SelectorNode): UUID[] => {
  if (selector.childNodes.length > 0) {
    return [...new Set(selector.childNodes.flatMap(getSelectedBottomElements))]
  } else {
    return selector.selected ? [selector.selectorId] : []
  }
}

export const asSelectOption = (selector: {
  selectorId: string
  name: string
}): ReactSelectOption => ({
  value: selector.selectorId,
  label: selector.name
})

type SelectedNode = {
  selectorId: UUID
  name: string
  messageRecipient: MessageRecipient
}

export const getSelected = (selectors: SelectorNode[]): SelectedNode[] =>
  selectors.flatMap((selector) => {
    if (!selector.selected) {
      return getSelected(selector.childNodes)
    } else {
      const { selectorId, name, messageRecipient } = selector
      return [{ selectorId, name, messageRecipient }]
    }
  })

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
