// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faAngleRight } from 'icon-set'

export interface BreadcrumbItemProps {
  children: React.ReactNode
  active?: boolean
  className?: string
  dataQa?: string
}

export const Breadcrumb: React.FunctionComponent<BreadcrumbItemProps> = ({
  active = false,
  className,
  children,
  dataQa
}) => (
  <div
    className={classNames({ 'is-active': active }, className)}
    data-qa={dataQa}
  >
    {children}
  </div>
)

export interface BreadcrumbProps {
  children: React.ReactNode[]
  className?: string
  dataQa?: string
}

export const Breadcrumbs: React.FunctionComponent<BreadcrumbProps> = ({
  children,
  className,
  dataQa
}) => (
  <nav className={classNames('breadcrumb', className)} data-qa={dataQa}>
    <ul>
      {children.map((c, i) => (
        <li key={i}>
          {c}
          {i < children.length - 1 && <FontAwesomeIcon icon={faAngleRight} />}
        </li>
      ))}
    </ul>
  </nav>
)
