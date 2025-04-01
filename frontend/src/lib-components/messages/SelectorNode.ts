// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  selectableRecipientIsStarter,
  selectableRecipientStartDate
} from 'lib-common/api-types/messaging'
import {
  DraftRecipient,
  MessageRecipient,
  SelectableRecipient,
  SelectableRecipientsResponse
} from 'lib-common/generated/api-types/messaging'
import { UUID } from 'lib-common/types'
import { TreeNode } from 'lib-components/atoms/dropdowns/TreeDropdown'

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

export const selectableRecipientsToNode = (
  accountId: UUID,
  recipients: SelectableRecipientsResponse[],
  starterTranslation: string,
  checkedRecipients: DraftRecipient[] = []
): SelectorNode[] => {
  const accountRecipients = recipients.find(
    (recipient) => recipient.accountId === accountId
  )?.receivers

  if (!accountRecipients) {
    return []
  }
  const selectorNodes = accountRecipients.map((r) =>
    selectableRecipientToNode(r, starterTranslation)
  )
  if (selectorNodes.length === 1 && selectorNodes[0].children.length === 0) {
    return selectorNodes.map((node) => ({ ...node, checked: true }))
  }
  if (checkedRecipients.length > 0) {
    return selectorNodes.map((node) => checkSelected(checkedRecipients, node))
  }
  return selectorNodes
}

function checkSelected(
  selectedRecipients: DraftRecipient[],
  node: SelectorNode
): SelectorNode {
  if (
    selectedRecipients
      .map((r) => recipientToKey(r.accountId, r.starter))
      .includes(node.key)
  ) {
    return checkAll(node)
  } else {
    const children = node.children.map((child) =>
      checkSelected(selectedRecipients, child)
    )
    return {
      ...node,
      checked: children.some((child) => child.checked),
      children
    }
  }
}

function checkAll(node: SelectorNode): SelectorNode {
  return { ...node, checked: true, children: node.children.map(checkAll) }
}

function recipientToKey(accountId: UUID, isStarter: boolean): string {
  return `${accountId}+${isStarter}`
}

function toRecipient(
  selectableRecipient: SelectableRecipient,
  isStarter: boolean
): MessageRecipient {
  switch (selectableRecipient.type) {
    case 'AREA':
      return {
        type: 'AREA',
        id: selectableRecipient.id
      }
    case 'UNIT_IN_AREA':
      return {
        type: 'UNIT',
        id: selectableRecipient.id,
        starter: false
      }

    case 'UNIT':
      return {
        type: 'UNIT',
        id: selectableRecipient.id,
        starter: isStarter
      }
    case 'GROUP':
      return {
        type: 'GROUP',
        id: selectableRecipient.id,
        starter: isStarter
      }
    case 'CHILD':
      return {
        type: 'CHILD',
        id: selectableRecipient.id,
        starter: isStarter
      }
    case 'CITIZEN':
      return {
        type: 'CITIZEN',
        id: selectableRecipient.id
      }
  }
}

function keyToSelectableRecipient(key: string): {
  accountId: UUID
  isStarter: boolean
} {
  const [accountId, isStarter] = key.split('+')
  return { accountId, isStarter: isStarter === 'true' }
}

export function nodeToSelectableRecipient(node: SelectedNode) {
  const r = keyToSelectableRecipient(node.key)
  return {
    id: r.accountId,
    isStarter: r.isStarter,
    text: node.text
  }
}

function selectableRecipientToNode(
  recipient: SelectableRecipient,
  starterTranslation: string
): SelectorNode {
  const startDate = selectableRecipientStartDate(recipient)
  const isStarter = selectableRecipientIsStarter(recipient)
  const nameWithStarterIndication = isStarter
    ? `${recipient.name} (${startDate?.format() ?? starterTranslation})`
    : recipient.name
  return {
    key: recipientToKey(recipient.id, isStarter),
    checked: false,
    text: nameWithStarterIndication,
    messageRecipient: toRecipient(recipient, isStarter),
    children:
      'receivers' in recipient
        ? recipient.receivers.map((r) =>
            selectableRecipientToNode(r, starterTranslation)
          )
        : []
  }
}

export type SelectedNode = {
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
