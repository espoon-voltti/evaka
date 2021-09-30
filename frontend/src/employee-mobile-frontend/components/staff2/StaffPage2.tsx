/*
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/

import React from 'react'
import { ContentArea } from 'lib-components/layout/Container'
import BottomNavBar, { NavItem } from '../common/BottomNavbar'

export interface Props {
  onNavigate: (item: NavItem) => void
}

export default function StaffPage2({ onNavigate }: Props) {
  return (
    <>
      <ContentArea opaque fullHeight>
        hello world
      </ContentArea>
      <BottomNavBar selected="staff" onChange={onNavigate} />
    </>
  )
}
