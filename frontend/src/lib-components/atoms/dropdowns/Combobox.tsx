// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, {
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState
} from 'react'
import styled, { css } from 'styled-components'

import { faChevronDown, faChevronUp, faTimes } from 'lib-icons'

import { useTranslations } from '../../i18n'
import UnderRowStatusIcon from '../StatusIcon'
import type { InputInfo } from '../form/InputField'
import { InputFieldUnderRow } from '../form/InputField'
import { SpinnerSegment } from '../state/Spinner'

import type { DropdownProps } from './shared'
import { borderRadius, borderStyles, Root } from './shared'

export interface ComboboxProps<T> extends DropdownProps<T, HTMLInputElement> {
  clearable?: boolean
  filterItems?: (inputValue: string, items: readonly T[]) => T[]
  onInputChange?: (newValue: string) => void
  getMenuItemLabel?: (item: T) => string
  isLoading?: boolean
  menuEmptyLabel?: string
  children?: {
    menuItem?: (props: MenuItemProps<T>) => React.ReactNode
    menuEmptyItem?: (label: string) => React.ReactNode
  }
  fullWidth?: boolean
  openAbove?: boolean
  info?: InputInfo
}

export interface MenuItemProps<T> {
  item: T
  highlighted: boolean
}

const InputWrapper = styled.div`
  display: flex;
  align-items: center;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  ${borderStyles};

  &.active {
    border-color: transparent;
  }

  &.success {
    border-color: ${(p) => p.theme.colors.status.success};
  }

  &.warning {
    border-color: ${(p) => p.theme.colors.status.warning};
  }
`

const MenuWrapper = styled.div`
  position: relative;
  margin: 0 -2px;
  z-index: 10;
  height: 0;
`

const Menu = styled.div<{ $openAbove?: boolean }>`
  z-index: 10;
  ${(p) =>
    p.$openAbove
      ? css`
          position: absolute;
          bottom: 40px;
          left: 0;
        `
      : css`
          position: relative;
          width: 100%;
        `}

  margin: 0;
  padding: 0;

  &.closed {
    display: none;
  }

  border: 1px solid ${(p) => p.theme.colors.grayscale.g70};
  border-radius: ${borderRadius};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
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
    background-color: ${(p) => p.theme.colors.main.m4};
  }

  &.clickable {
    cursor: pointer;
  }

  white-space: pre-line;
`

const Input = styled.input`
  background: none;
  padding: 8px 0 8px 12px;
  flex: 1 1 auto;
  border: 0;
  outline: 0;

  &:read-only {
    color: ${(p) => p.theme.colors.grayscale.g70};
  }
`

const Separator = styled.div`
  width: 1px;
  margin: 5px 0 5px;
  border-left: 1px solid ${(p) => p.theme.colors.grayscale.g15};
`

const Button = styled.button`
  color: ${(p) => p.theme.colors.grayscale.g70};

  &:hover {
    color: ${(p) => p.theme.colors.main.m2Hover};
  }

  &:active {
    color: ${(p) => p.theme.colors.main.m2Active};
  }

  &:disabled {
    color: ${(p) => p.theme.colors.grayscale.g70};
    cursor: not-allowed;
  }

  cursor: pointer;
  padding: 4px 10px;
  background: none;
  border: 0;
  flex: 0 0 auto;
`

function defaultGetItemLabel<T>(item: T) {
  return String(item)
}

function Combobox<T>(props: ComboboxProps<T>) {
  const {
    id,
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
    name,
    onFocus,
    onInputChange,
    children,
    fullWidth,
    openAbove,
    'data-qa': dataQa,
    'aria-labelledby': ariaLabelledby
  } = props
  const i18n = useTranslations()

  const [isOpen, setIsOpenRaw] = useState(false)
  const [inputValue, setInputValue] = useState(() =>
    selectedItem ? getItemLabel(selectedItem) : ''
  )
  const [highlightedIndex, setHighlightedIndex] = useState(-1)
  const baseId = useId()
  const menuId = `${baseId}-listbox`

  const setIsOpen = useCallback((open: boolean) => {
    setIsOpenRaw(open)
    if (!open) setHighlightedIndex(-1)
  }, [])

  const defaultFilterItems = useCallback(
    (inputValue: string, items: readonly T[]) => {
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

  const filterItems = useMemo(
    () => props.filterItems ?? defaultFilterItems,
    [props.filterItems, defaultFilterItems]
  )
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
  const filteredItems = useMemo(
    () => filterItems(currentFilter, items),
    [filterItems, items, currentFilter]
  )

  const toggleMenu = () => setIsOpen(!isOpen)

  const reset = useCallback(() => {
    onChange?.(null)
    setInputValue('')
    setIsOpen(false)
    setCurrentFilter('')
  }, [onChange, setIsOpen])

  useEffect(() => {
    if (!isOpen) {
      setInputValue(itemToString(selectedItem))
      setCurrentFilter('')
    }
  }, [isOpen, itemToString, selectedItem])

  const prevSelectedItemRef = useRef(selectedItem)
  useEffect(() => {
    if (!isOpen && prevSelectedItemRef.current !== selectedItem) {
      setInputValue(itemToString(selectedItem))
    }
    prevSelectedItemRef.current = selectedItem
  }, [isOpen, selectedItem, itemToString])

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        if (!isOpen) {
          setIsOpen(true)
          setHighlightedIndex(0)
        } else {
          setHighlightedIndex((prev) =>
            prev >= filteredItems.length - 1 ? 0 : prev + 1
          )
        }
        break
      case 'ArrowUp':
        e.preventDefault()
        if (isOpen) {
          setHighlightedIndex((prev) =>
            prev <= 0 ? filteredItems.length - 1 : prev - 1
          )
        }
        break
      case 'Enter':
        if (isOpen) {
          e.preventDefault()
          if (
            highlightedIndex >= 0 &&
            highlightedIndex < filteredItems.length
          ) {
            onChange?.(filteredItems[highlightedIndex] ?? null)
          }
          setIsOpen(false)
        }
        break
      case 'Escape':
        setIsOpen(false)
        setInputValue(itemToString(selectedItem))
        break
      case 'Home':
        if (isOpen) {
          e.preventDefault()
          setHighlightedIndex(0)
        }
        break
      case 'End':
        if (isOpen) {
          e.preventDefault()
          setHighlightedIndex(filteredItems.length - 1)
        }
        break
    }
  }

  return (
    <>
      <Root
        data-qa={dataQa}
        className={classNames({ active: isOpen, 'full-width': fullWidth })}
      >
        <InputWrapper
          className={classNames({ active: isOpen }, props.info?.status)}
        >
          <Input
            value={inputValue}
            onChange={(e) => {
              const newValue = e.target.value
              setInputValue(newValue)
              if (!isOpen) setIsOpen(true)
              setCurrentFilter(newValue)
              onInputChange?.(newValue)
            }}
            onClick={() => setIsOpen(!isOpen)}
            onKeyDown={handleKeyDown}
            onBlur={() => setIsOpen(false)}
            role="combobox"
            aria-expanded={isOpen}
            aria-controls={menuId}
            aria-activedescendant={
              highlightedIndex >= 0
                ? `${baseId}-option-${highlightedIndex}`
                : undefined
            }
            aria-autocomplete="list"
            id={id}
            name={name}
            disabled={disabled}
            placeholder={placeholder}
            onFocus={onFocus}
            aria-labelledby={ariaLabelledby}
          />
          {clearable && selectedItem && (
            <>
              <Button
                data-qa="clear"
                type="button"
                onClick={
                  disabled
                    ? undefined
                    : (e) => {
                        e.stopPropagation()
                        reset()
                      }
                }
              >
                <FontAwesomeIcon icon={faTimes} />
              </Button>
              <Separator>&nbsp;</Separator>
            </>
          )}
          <Button
            data-qa="toggle"
            type="button"
            disabled={disabled}
            aria-label={
              isOpen ? i18n.combobox.closeDropdown : i18n.combobox.openDropdown
            }
            onMouseDown={(e) => {
              e.preventDefault()
              toggleMenu()
            }}
          >
            <FontAwesomeIcon icon={isOpen ? faChevronUp : faChevronDown} />
          </Button>
        </InputWrapper>
        <MenuWrapper>
          <Menu
            id={menuId}
            role="listbox"
            $openAbove={openAbove}
            className={classNames({ closed: !isOpen })}
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
                      id={`${baseId}-option-${index}`}
                      role="option"
                      aria-selected={item === selectedItem}
                      onClick={() => {
                        onChange?.(item)
                        setIsOpen(false)
                      }}
                      onMouseEnter={() => setHighlightedIndex(index)}
                      onMouseDown={(e) => e.preventDefault()}
                      ref={
                        highlightedIndex === index
                          ? (el) => el?.scrollIntoView({ block: 'nearest' })
                          : undefined
                      }
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
      {props.info && (
        <InputFieldUnderRow className={classNames(props.info.status)}>
          <span data-qa={dataQa ? `${dataQa}-info` : undefined}>
            {props.info.text}
          </span>
          <UnderRowStatusIcon status={props.info.status} />
        </InputFieldUnderRow>
      )}
    </>
  )
}

export default React.memo(Combobox) as typeof Combobox
