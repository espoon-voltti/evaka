// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTranslation } from '~state/i18n'

import styled from 'styled-components'
import { customColours, EspooColours } from '~utils/colours'

export type StatusLabelType = 'coming' | 'active' | 'completed' | 'conflict'

const Container = styled.div<{ status: StatusLabelType }>`
  height: 25px;
  width: fit-content;
  border-radius: 12px;
  padding: 0 10px;
  text-align: center;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0;

  ${(p) =>
    p.status == 'coming'
      ? `
   color: ${customColours.darkGreen};
   border: 1px solid ${customColours.darkGreen};
  `
      : ''}

  ${(p) =>
    p.status == 'active'
      ? `
   color: ${EspooColours.white};
   background: ${customColours.darkGreen};
  `
      : ''}

  ${(p) =>
    p.status == 'completed'
      ? `
   color: ${EspooColours.greyDark};
   border: 1px solid ${EspooColours.grey};
  `
      : ''}

  ${(p) =>
    p.status == 'conflict'
      ? `
   color: ${EspooColours.white};
   background: ${EspooColours.red};
  `
      : ''}
`

export interface Props {
  status: StatusLabelType
}

function StatusLabel({ status }: Props) {
  const { i18n } = useTranslation()

  return <Container status={status}>{i18n.common.statuses[status]}</Container>
}

export default StatusLabel
