// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import * as Sentry from '@sentry/browser'
import { faChevronUp } from 'Icons'
import sortBy from 'lodash/sortBy'
import React, { useEffect, useMemo, useRef, useState } from 'react'
import styled from 'styled-components'

import { useUniqueId } from 'lib-common/utils/useUniqueId'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faCheck, faChevronDown, faDash } from 'lib-icons'

import { useTranslations } from '../../i18n'
import IconButton from '../buttons/IconButton'

const DropdownContainer = styled.div`
  border: 1px solid ${(p) => p.theme.colors.grayscale.g70};
  border-radius: 4px;
  padding: ${defaultMargins.xs};
  display: flex;
  gap: ${defaultMargins.xs};
  justify-content: space-between;
  align-items: center;
  background-color: ${(p) => p.theme.colors.grayscale.g0};

  &:active {
    box-shadow: 0 0 0 2px ${(p) => p.theme.colors.main.m2Focus};
  }
`

const DropdownContainerContent = styled.div`
  color: ${(p) => p.theme.colors.grayscale.g70};
  display: flex;
  flex-wrap: wrap;
  gap: ${defaultMargins.xs};
`

const RelativeContainer = styled.div`
  position: relative;
`

const DropdownTreeListContainer = styled.div`
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  border: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  box-shadow: 0px 4px 4px rgba(0, 0, 0, 0.15);
  border-radius: 4px;
  max-height: 300px;
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  position: absolute;
  top: 100%;
  margin-top: ${defaultMargins.s};
  margin-bottom: ${defaultMargins.s};
  width: 100%;
  z-index: 15;
  overflow-y: auto;
`

const diameter = '30px'

const Box = styled.div`
  position: relative;
  width: ${diameter};
  height: ${diameter};
  margin-top: ${defaultMargins.xxs};
`

const CheckboxInput = styled.input`
  outline: none;
  appearance: none;
  width: ${diameter};
  height: ${diameter};
  border-radius: 4px;
  border: 1px solid ${(p) => p.theme.colors.grayscale.g70};
  margin: 0;

  background-color: ${(p) => p.theme.colors.grayscale.g0};

  &:checked {
    border-color: ${(p) => p.theme.colors.main.m2};
    background-color: ${(p) => p.theme.colors.main.m2};

    &:disabled {
      background-color: ${(p) => p.theme.colors.grayscale.g35};
    }
  }

  &:focus {
    box-shadow:
      0 0 0 2px ${(p) => p.theme.colors.grayscale.g0},
      0 0 0 4px ${(p) => p.theme.colors.main.m2Focus};
  }

  &:disabled {
    border-color: ${(p) => p.theme.colors.grayscale.g35};
  }
`

const IconWrapper = styled.div`
  position: absolute;
  left: 0;
  top: 0;

  display: flex;
  justify-content: center;
  align-items: center;
  width: ${diameter};
  height: ${diameter};

  font-size: 25px;
  color: ${(p) => p.theme.colors.grayscale.g0};

  pointer-events: none; // let click event go through icon to the checkbox
`

const TreeChildren = styled.div`
  margin-left: ${defaultMargins.XL};
`

const CheckboxRow = styled(FixedSpaceRow)`
  margin-bottom: ${defaultMargins.xs};
`

const ValueChip = styled.div`
  background-color: ${(p) => p.theme.colors.accents.a8lightBlue};
  padding: ${defaultMargins.xxs} ${defaultMargins.xs};
  font-weight: ${fontWeights.semibold};
  border-radius: 9999px;
  color: ${(p) => p.theme.colors.grayscale.g100};
  display: flex;
  flex-wrap: wrap;
  gap: ${defaultMargins.xs};
  align-items: center;
`

/**
 * If a node is unchecked, all of the children should be
 * unchecked as well. An indeterminate state is shown if
 * any of the children (checked recursively) is unchecked
 * when this parent is checked.
 */
export interface TreeNode {
  text: string
  key: string
  checked: boolean
  children: TreeNode[]
}

export const sortTreeByText = (tree: TreeNode[]): TreeNode[] =>
  sortBy(
    tree.map((node) => ({
      ...node,
      children: sortTreeByText(node.children)
    })),
    (node) => node.text
  )

interface TreeDropdownProps<N extends TreeNode> {
  tree: N[]
  onChange: (node: N[]) => void
  'data-qa'?: string
  placeholder: string
}

const updateNodeCheckedRecursively = <N extends TreeNode>(
  node: N,
  checked: boolean
): N => ({
  ...node,
  checked,
  children: node.children.map((child) =>
    updateNodeCheckedRecursively(child, checked)
  )
})

export const hasUncheckedChildren = (node: TreeNode): boolean =>
  node.children.some((child) => !child.checked || hasUncheckedChildren(child))

const hasOnlyUncheckedChildren = (node: TreeNode): boolean =>
  node.children.every(
    (child) => !child.checked && hasOnlyUncheckedChildren(child)
  )

function _TreeLevel<N extends TreeNode>({
  node,
  onChange,
  defaultExpanded = false,
  labels
}: {
  node: N
  onChange: (node: N) => void
  defaultExpanded?: boolean
  labels: {
    expand: (opt: string) => string
    collapse: (opt: string) => string
  }
}) {
  const id = useUniqueId()

  const indeterminate = useMemo(() => hasUncheckedChildren(node), [node])

  const [expanded, setExpanded] = useState(defaultExpanded)

  if (!node.checked && node.children.some(({ checked }) => checked)) {
    Sentry.captureMessage('Unchecked node has checked children', 'error')
    return null
  }

  return (
    <div>
      <CheckboxRow alignItems="center">
        <Box data-qa={`tree-checkbox-${node.key}`}>
          <CheckboxInput
            type="checkbox"
            checked={node.checked}
            id={id}
            onChange={(e) => {
              e.stopPropagation()
              onChange(updateNodeCheckedRecursively(node, !node.checked))
            }}
            aria-checked={indeterminate ? 'mixed' : node.checked}
          />
          <IconWrapper>
            {indeterminate ? (
              <FontAwesomeIcon icon={faDash} />
            ) : (
              <FontAwesomeIcon icon={faCheck} />
            )}
          </IconWrapper>
        </Box>
        <label htmlFor={id}>{node.text}</label>
        {node.children.length > 0 && (
          <IconButton
            icon={expanded ? faChevronUp : faChevronDown}
            onClick={() => setExpanded(!expanded)}
            aria-expanded={expanded}
            aria-label={
              expanded ? labels.collapse(node.text) : labels.expand(node.text)
            }
            data-qa={`tree-toggle-${node.key}`}
          />
        )}
      </CheckboxRow>
      {expanded && node.children.length > 0 && (
        <TreeChildren>
          {node.children.map((childNode, i) => (
            <TreeLevel
              node={childNode}
              key={childNode.key}
              onChange={(changedNode) => {
                const newNode = {
                  ...node,
                  children: node.children.map((existingNode, idx) =>
                    idx === i ? changedNode : existingNode
                  )
                }

                if (!hasUncheckedChildren(newNode)) {
                  // if all of the children are checked, the parent node
                  // should be checked now too
                  onChange({
                    ...newNode,
                    checked: true
                  })
                  return
                }

                onChange({
                  ...newNode,
                  checked: !hasOnlyUncheckedChildren(newNode)
                })
              }}
              labels={labels}
            />
          ))}
        </TreeChildren>
      )}
    </div>
  )
}

const TreeLevel = React.memo(_TreeLevel) as typeof _TreeLevel

function TreeDropdown<N extends TreeNode>({
  tree,
  onChange,
  'data-qa': dataQa,
  placeholder
}: TreeDropdownProps<N>) {
  const i18n = useTranslations().treeDropdown
  const [active, setActive] = useState(false)

  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (active) {
      const listener = (ev: MouseEvent) => {
        if (
          ref.current !== ev.target &&
          !ref.current?.contains(ev.target as Node)
        ) {
          setActive(false)
        }
      }

      document.addEventListener('mousedown', listener)

      return () => {
        document.removeEventListener('mousedown', listener)
      }
    }

    return () => {
      // no-op
    }
  }, [active])

  const treeValue = useMemo(() => {
    const formEntries = (
      nodes: TreeNode[]
    ): { key: string[]; child: React.ReactNode }[] =>
      nodes
        .filter(({ checked }) => checked)
        .flatMap((node) =>
          hasUncheckedChildren(node)
            ? formEntries(node.children).map(({ child, key }) => ({
                child: (
                  <React.Fragment key={JSON.stringify([...node.key, key])}>
                    {node.text}/{child}
                  </React.Fragment>
                ),
                key: [node.key, ...key]
              }))
            : [{ child: <>{node.text}</>, key: [node.key] }]
        )

    return tree
      .filter(({ checked }) => checked)
      .map((node) =>
        hasUncheckedChildren(node) ? (
          formEntries(node.children).map(({ child, key }) => (
            <ValueChip key={JSON.stringify([...node.key, key])} data-qa="value">
              {tree.length !== 1 ? (
                // do not show unnecessary top-level node text if it's the only top-level node
                <>{node.text}/</>
              ) : null}
              {child}
            </ValueChip>
          ))
        ) : (
          <ValueChip key={node.key} data-qa="value">
            {node.text}
          </ValueChip>
        )
      )
  }, [tree])

  return (
    <RelativeContainer ref={ref} data-qa={dataQa}>
      <DropdownContainer
        role="combobox"
        aria-expanded={active}
        onClick={() => setActive(!active)}
        tabIndex={0}
        onKeyUp={(ev) => {
          if (ev.key === 'Enter') {
            setActive(!active)
          }
        }}
        data-qa="tree-dropdown"
        data-qa-expanded={active}
      >
        <DropdownContainerContent data-qa="selected-values">
          {treeValue.length === 0 ? placeholder : treeValue}
        </DropdownContainerContent>
        <IconButton icon={faChevronDown} aria-label={i18n.expandDropdown} />
      </DropdownContainer>
      {active && (
        <DropdownTreeListContainer
          data-qa={dataQa ? `${dataQa}-tree` : undefined}
        >
          {tree.map((node, i) => (
            <TreeLevel<N>
              node={node}
              key={node.key}
              onChange={(changedNode) =>
                onChange(
                  tree.map((existingNode, idx) =>
                    idx === i ? changedNode : existingNode
                  )
                )
              }
              defaultExpanded={tree.length === 1}
              labels={i18n}
            />
          ))}
        </DropdownTreeListContainer>
      )}
    </RelativeContainer>
  )
}

export default React.memo(TreeDropdown) as typeof TreeDropdown
