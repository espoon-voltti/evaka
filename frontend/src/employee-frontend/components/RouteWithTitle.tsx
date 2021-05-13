// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'
import { Route } from 'react-router-dom'
import { RouteComponentProps } from 'react-router'
import { TitleContext, TitleState } from '../state/title'

interface Props<T> {
  title?: string
  path: string
  component:
    | React.ComponentType<RouteComponentProps<T>>
    | React.ComponentType<unknown>
  exact?: boolean
}

export function RouteWithTitle<T>({ title, path, component, exact }: Props<T>) {
  const { setTitle } = useContext<TitleState>(TitleContext)

  useEffect(() => {
    if (title) setTitle(title)
  }, [path]) // eslint-disable-line react-hooks/exhaustive-deps

  return exact ? (
    <Route exact path={path} component={component} />
  ) : (
    <Route path={path} component={component} />
  )
}
