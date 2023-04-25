// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { MessageReceiver } from 'lib-common/api-types/messaging'
import type {
  MessageReceiversResponse,
  MessageRecipient
} from 'lib-common/generated/api-types/messaging'
import type { UUID } from 'lib-common/types'
import type { TreeNode } from 'lib-components/atoms/dropdowns/TreeDropdown'

/*
    SelectorNode is a recursive type for storing a selectorId, the corresponding selected
    state and (possibly) child objects of the same type. It updates according to the following rules:

    1. If a SelectorNode is selected, each child node will also be selected.
    2. If a SelectorNode is unselected, each child node will also be unselected.
    3. If each child node of a SelectorNode is selected, it will also be selected.
    4. If a child node of a SelectorNode is unselected, it will also be unselected.
*/
export interface SelectorNode extends TreeNode {
  messageRecipient: MessageRecipient
  children: SelectorNode[]
}

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

  if (selectorNodes.length === 1 && selectorNodes[0].children.length === 0) {
    return selectorNodes.map((node) => ({ ...node, checked: true }))
  }

  return selectorNodes
}

const receiverAsSelectorNode = (receiver: MessageReceiver): SelectorNode => ({
  key: receiver.id,
  checked: false,
  text: receiver.name,
  messageRecipient: { type: receiver.type, id: receiver.id },
  children:
    'receivers' in receiver
      ? receiver.receivers.map(receiverAsSelectorNode)
      : []
})

type SelectedNode = {
  key: UUID
  text: string
  messageRecipient: MessageRecipient
}

export const getSelected = (selectors: SelectorNode[]): SelectedNode[] =>
  selectors.flatMap((selector) => {
    const selectedChildren = getSelected(selector.children)
    const selectedKeys = selectedChildren.map(({ key }) => key)
    if (
      selector.checked &&
      selector.children.length === selectedChildren.length &&
      selector.children.every(({ key }) => selectedKeys.includes(key))
    ) {
      const { key, text, messageRecipient } = selector
      return [{ key, text, messageRecipient }]
    } else {
      return selectedChildren
    }
  })
