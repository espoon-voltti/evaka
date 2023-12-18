// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'
import { Navigate } from 'react-router-dom'

import { Translations } from 'lib-customizations/employee'

import { useTranslation } from '../state/i18n'
import { TitleContext, TitleState } from '../state/title'
import { UserContext } from '../state/user'

interface Props {
  title?: keyof Translations['titles']
  hideDefaultTitle?: boolean
  requireAuth?: boolean
  children?: React.ReactNode
}

export default React.memo(function EmployeeRoute({
  title,
  hideDefaultTitle,
  requireAuth = true,
  children
}: Props) {
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)

  useEffect(
    () => setTitle(title ? i18n.titles[title] : '', hideDefaultTitle),
    [hideDefaultTitle, i18n.titles, setTitle, title]
  )

  return requireAuth ? <RequireAuth element={children} /> : <>{children}</>
})

const RequireAuth = React.memo(function EnsureAuthenticated({
  element
}: {
  element: React.ReactNode
}) {
  const { loggedIn } = useContext(UserContext)
  return loggedIn ? <>{element}</> : <Navigate replace to="/" />
})
