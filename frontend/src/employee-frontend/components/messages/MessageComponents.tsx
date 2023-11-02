// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { formatDateOrTime } from 'lib-common/date'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

export const MessageRow = styled.div<{ unread?: boolean }>`
  display: flex;
  justify-content: space-between;
  padding: ${defaultMargins.s};
  cursor: pointer;
  &:first-child {
    border-top: 1px solid ${colors.grayscale.g15};
  }
  border-bottom: 1px solid ${colors.grayscale.g15};
  border-left: 6px solid
    ${(p) => (p.unread ? colors.status.success : 'transparent')};

  .delete-btn {
    opacity: 0;
  }

  &:hover {
    .delete-btn {
      opacity: 1;
    }
  }
`
export const Participants = styled.div<{ unread?: boolean }>`
  color: ${(p) => (p.unread ? colors.grayscale.g100 : colors.grayscale.g70)};
  font-weight: ${fontWeights.semibold};
`
export const Truncated = styled.div`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`
export const Title = styled.span<{ unread?: boolean }>`
  font-weight: ${(p) => (p.unread ? fontWeights.semibold : fontWeights.normal)};
`
export const Hyphen = styled.span<{ unread?: boolean }>`
  font-weight: ${(p) => (p.unread ? fontWeights.semibold : fontWeights.normal)};
  margin-left: 10px;
  margin-right: 10px;
`
export const ParticipantsAndPreview = styled.div`
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding-right: ${defaultMargins.m};
`
export const TypeAndDate = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-end;
`

export function Timestamp({ date }: { date: HelsinkiDateTime }) {
  return <span>{formatDateOrTime(date)}</span>
}
