// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'
import { faChevronUp as falChevronUp } from 'icon-set'
import { faChevronDown as falChevronDown } from 'icon-set'
import { fasChevronUp } from 'icon-set'
import { fasChevronDown } from 'icon-set'

import { Button, ButtonProps } from '../../elements/button'
import { Icon } from '../../elements/icon'
import { StatusType } from '../../types/common'

export interface TableProps {
  className?: string
  dataQa?: string
  interactive?: boolean
}

interface Props {
  dataQa?: string
  className?: string
  children?: React.ReactNode
  onClick?: (e: React.MouseEvent) => void
}

export const Table: React.FunctionComponent<TableProps & Props> = ({
  children,
  className,
  dataQa,
  interactive,
  onClick
}) => (
  <table
    onClick={onClick}
    className={classNames(
      'table',
      {
        'is-interactive': interactive
      },
      className
    )}
    data-qa={dataQa}
  >
    {children}
  </table>
)

export const Head: React.FunctionComponent<Props> = ({
  children,
  className
}) => <thead className={className}>{children}</thead>

export const Body: React.FunctionComponent<Props> = ({
  children,
  className
}) => <tbody className={className}>{children}</tbody>

export interface TableStatusProps {
  status?: StatusType
}

export const Row: React.FunctionComponent<Props & TableStatusProps> = ({
  children,
  className,
  status,
  onClick,
  dataQa
}) => (
  <tr
    data-qa={dataQa}
    onClick={onClick}
    className={classNames(
      'status',
      {
        [`is-${status ?? ''}`]: status
      },
      className
    )}
  >
    {children}
  </tr>
)

export interface TableCellProps extends Props {
  colSpan?: number
  narrow?: boolean
  dataQa?: string
  align?: 'left' | 'right' | 'center'
}

const TableCellImpl = (
  Component: 'td' | 'th'
): React.FunctionComponent<TableCellProps> => ({
  children,
  className,
  onClick,
  colSpan,
  narrow = false,
  align = 'left',
  dataQa
}) => {
  return (
    <Component
      data-qa={dataQa}
      className={classNames(
        `has-text-${align}`,
        { 'is-narrow': narrow },
        className
      )}
      onClick={onClick}
      colSpan={colSpan}
    >
      {children}
    </Component>
  )
}

export const Th = TableCellImpl('th')

export const Td = TableCellImpl('td')

export interface TableHeadButton extends TableCellProps {
  sortable?: boolean
  sorted?: 'ASC' | 'DESC'
}

export const HeadButton: React.FunctionComponent<
  TableHeadButton & ButtonProps & Props
> = ({
  children,
  className,
  colSpan,
  sortable,
  sorted,
  narrow,
  ...buttonProps
}) => (
  <Th className={className} colSpan={colSpan} narrow={narrow}>
    <Button {...buttonProps} plain dark className="thead-button">
      <span>{children}</span>

      {sortable && (
        <div className="thead-button-icons">
          <Icon
            icon={sorted === 'DESC' ? fasChevronUp : falChevronUp}
            iconSize={'xs'}
          />
          <Icon
            icon={sorted === 'ASC' ? fasChevronDown : falChevronDown}
            iconSize={'xs'}
          />
        </div>
      )}
    </Button>
  </Th>
)

export const Toolbox: React.FunctionComponent<TableCellProps> = ({
  children,
  ...rest
}) => (
  <Td {...rest}>
    <div className="table-toolbox">{children}</div>
  </Td>
)
