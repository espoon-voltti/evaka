// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useLayoutEffect } from 'react'
import { useLocation } from 'react-router-dom'

import { scrollToPos } from 'lib-common/utils/scrolling'

export default React.memo(function ScrollToTop({
  children
}: {
  children?: React.ReactNode
}) {
  const location = useLocation()

  useLayoutEffect(() => {
    scrollToPos({ top: 0, left: 0, behavior: 'auto' })
  }, [location.pathname])

  return <>{children}</>
})
