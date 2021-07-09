// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCombobox, UseComboboxStateChange } from 'downshift'
import React, {
  FocusEventHandler,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState
} from 'react'
import styled from 'styled-components'
import { faChevronDown, faTimes } from 'lib-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import computeScrollIntoView from 'compute-scroll-into-view'
import classNames from 'classnames'
import { SpinnerSegment } from '../state/Spinner'

const borderRadius = '2px'

const Root = styled.div`
  max-width: 500px;
  border-radius: ${borderRadius};
  border: 2px solid transparent;

  &.active {
    border-color: ${({ theme: { colors } }) => colors.main.primaryActive};
  }
  &.full-width {
    width: 100%;
    max-width: initial;
  }
`

const InputWrapper = styled.div`
  display: flex;
  align-items: center;
  border: 1px solid ${({ theme: { colors } }) => colors.greyscale.lighter};
  &.active {
    border-color: transparent;
  }
  border-radius: ${borderRadius};
  background-color: white;
`

const MenuWrapper = styled.div`
  margin: 0 -2px;
  z-index: 10;
  height: 0;
`

const Menu = styled.div`
  position: relative;
  width: 100%;
  z-index: 10;
  top: 10px;
  margin: 0;
  padding: 0;
  &.closed {
    display: none;
  }
  border: 1px solid ${({ theme: { colors } }) => colors.greyscale.lighter};
  border-radius: ${borderRadius};
  background-color: white;
  max-height: 300px;
  overflow-y: auto;
`

const MenuItemWrapper = styled.li`
  padding: 0;
  list-style: none;
`

const MenuItem = styled.div`
  padding: 8px 10px;
  &.highlighted {
    background-color: ${({ theme: { colors } }) => colors.brand.secondaryLight};
  }
  &.clickable {
    cursor: pointer;
  }
  white-space: pre-line;
`

const Input = styled.input`
  background: none;
  padding: 5px 0 5px 10px;
  flex: 1 1 auto;
  border: 0;
  outline: 0;
  &:read-only {
    color: ${({ theme: { colors } }) => colors.greyscale.dark};
  }
`

const Separator = styled.div`
  width: 1px;
  margin: 5px 0 5px;
  border-left: 1px solid ${({ theme: { colors } }) => colors.greyscale.lighter};
  cursor: pointer;
`

const Button = styled.button`
  color: ${({ theme: { colors } }) => colors.greyscale.dark};
  &:hover {
    color: ${({ theme: { colors } }) => colors.main.primaryHover};
  }
  &:active {
    color: ${({ theme: { colors } }) => colors.main.primaryActive};
  }
  &:disabled {
    color: ${({ theme: { colors } }) => colors.greyscale.dark};
    cursor: not-allowed;
  }

  cursor: pointer;
  padding: 4px 10px;
  background: none;
  border: 0;
  flex: 0 0 auto;
`

interface MenuItemProps<T> {
  item: T
  highlighted: boolean
}

interface Props<T> {
  items: T[]
  selectedItem: T | null
  onChange: (item: T | null) => void
  clearable?: boolean
  disabled?: boolean
  placeholder?: string
  filterItems?: (inputValue: string, items: T[]) => T[]
  getItemLabel?: (item: T) => string
  getItemDataQa?: (item: T) => string | undefined
  getMenuItemLabel?: (item: T) => string
  isLoading?: boolean
  menuEmptyLabel?: string
  onFocus?: FocusEventHandler<HTMLInputElement>
  onInputChange?: (newValue: string) => void
  children?: {
    menuItem?: (props: MenuItemProps<T>) => React.ReactNode
    menuEmptyItem?: (label: string) => React.ReactNode
  }
  fullWidth?: boolean
  'data-qa'?: string
}

function defaultGetItemLabel<T>(item: T) {
  return String(item)
}

function stopPropagation(e: React.SyntheticEvent) {
  e.stopPropagation()
}

function preventDefault(e: React.SyntheticEvent) {
  e.preventDefault()
}

function ensureElementIsInView(element: HTMLElement) {
  computeScrollIntoView(element, {
    block: 'end',
    inline: 'nearest',
    scrollMode: 'if-needed'
  }).forEach(({ el, top, left }) => {
    el.scrollTop = top
    el.scrollLeft = left
  })
}

export default function Combobox<T>(props: Props<T>) {
  const {
    items,
    selectedItem,
    onChange,
    clearable,
    disabled,
    placeholder,
    getItemLabel = defaultGetItemLabel,
    getMenuItemLabel = getItemLabel,
    getItemDataQa,
    isLoading,
    menuEmptyLabel,
    onFocus,
    onInputChange,
    children,
    fullWidth,
    'data-qa': dataQa
  } = props
  const enabled = !disabled

  const defaultFilterItems = useCallback(
    (inputValue: string, items: T[]) => {
      const filter = inputValue.toLowerCase()
      return items.filter((item) =>
        getItemLabel(item).toLowerCase().startsWith(filter)
      )
    },
    [getItemLabel]
  )
  const defaultRenderMenuItem = useCallback(
    ({ highlighted, item }: MenuItemProps<T>) => (
      <MenuItem
        data-qa={getItemDataQa?.(item)}
        className={classNames({ highlighted, clickable: true })}
      >
        {getMenuItemLabel(item)}
      </MenuItem>
    ),
    [getMenuItemLabel, getItemDataQa]
  )

  const filterItems = useMemo(() => props.filterItems ?? defaultFilterItems, [
    props.filterItems,
    defaultFilterItems
  ])
  const renderMenuItem = useMemo(
    () => children?.menuItem ?? defaultRenderMenuItem,
    [children?.menuItem, defaultRenderMenuItem]
  )
  const renderEmptyResult = useMemo(
    () =>
      children?.menuEmptyItem ??
      ((message: string) => <MenuItem>{message}</MenuItem>),
    [children?.menuEmptyItem]
  )

  const itemToString = useCallback(
    (item: T | null) => (item ? getItemLabel(item) : ''),
    [getItemLabel]
  )
  const [currentFilter, setCurrentFilter] = useState('')
  const filteredItems = useMemo(() => filterItems(currentFilter, items), [
    filterItems,
    items,
    currentFilter
  ])

  const menuRef = useRef<HTMLElement>()

  const onInputValueChange = useCallback(
    ({ isOpen, inputValue }: UseComboboxStateChange<T>) => {
      if (isOpen) {
        setCurrentFilter(inputValue ?? '')
      } else {
        setCurrentFilter('')
      }
      if (onInputChange) {
        onInputChange(inputValue ?? '')
      }
    },
    [onInputChange, setCurrentFilter]
  )
  const onIsOpenChange = useCallback(
    ({ isOpen }: UseComboboxStateChange<T>) => {
      if (isOpen && menuRef.current) {
        ensureElementIsInView(menuRef.current)
      }
    },
    [menuRef]
  )
  const onSelectedItemChange = useCallback(
    ({ selectedItem }: UseComboboxStateChange<T>) => {
      if (onChange) {
        onChange(selectedItem ?? null)
      }
    },
    [onChange]
  )
  const {
    isOpen,
    toggleMenu,
    getInputProps,
    getToggleButtonProps,
    getMenuProps,
    getComboboxProps,
    getItemProps,
    highlightedIndex,
    setInputValue,
    reset
  } = useCombobox({
    selectedItem,
    itemToString,
    items: filteredItems,
    onInputValueChange,
    onIsOpenChange,
    onSelectedItemChange
  })
  const onClickClear = useCallback(
    (e: React.MouseEvent<HTMLButtonElement>) => {
      e.stopPropagation()
      reset()
    },
    [reset]
  )
  useEffect(() => {
    if (!isOpen) {
      setInputValue(itemToString(selectedItem))
    }
  }, [isOpen, setInputValue, itemToString, selectedItem])
  return (
    <Root
      data-qa={dataQa}
      className={classNames({ active: isOpen, 'full-width': fullWidth })}
      onClick={enabled ? toggleMenu : undefined}
    >
      <InputWrapper
        {...getComboboxProps({
          disabled,
          className: classNames({ active: isOpen })
        })}
      >
        <Input
          {...getInputProps({
            disabled,
            placeholder,
            onFocus
          })}
        />
        {clearable && selectedItem && (
          <Button data-qa="clear" onClick={enabled ? onClickClear : undefined}>
            <FontAwesomeIcon icon={faTimes} />
          </Button>
        )}
        <Separator>&nbsp;</Separator>
        <Button
          data-qa="toggle"
          type="button"
          {...getToggleButtonProps({
            disabled,
            // avoid toggling the menu twice
            onClick: preventDefault
          })}
        >
          <FontAwesomeIcon icon={faChevronDown} />
        </Button>
      </InputWrapper>
      <MenuWrapper onClick={stopPropagation}>
        <Menu
          className={classNames({ closed: !isOpen })}
          {...getMenuProps({
            // styled-components and downshift typings don't play nice together
            // eslint-disable-next-line @typescript-eslint/no-explicit-any,@typescript-eslint/no-unsafe-assignment
            ref: menuRef as any
          })}
        >
          {isOpen && (
            <>
              {isLoading ? (
                <MenuItemWrapper>
                  <SpinnerSegment />
                </MenuItemWrapper>
              ) : filteredItems.length === 0 ? (
                <MenuItemWrapper>
                  {renderEmptyResult(menuEmptyLabel ?? '')}
                </MenuItemWrapper>
              ) : (
                filteredItems.map((item, index) => (
                  <MenuItemWrapper
                    data-qa="item"
                    key={index}
                    {...getItemProps({ item, index })}
                  >
                    {renderMenuItem({
                      item,
                      highlighted: highlightedIndex === index
                    })}
                  </MenuItemWrapper>
                ))
              )}
            </>
          )}
        </Menu>
      </MenuWrapper>
    </Root>
  )
}
