// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import {Child} from 'lib-common/generated/api-types/attendance'

import { useTranslation } from '../../../../state/i18n'
import styled from "styled-components";
import { useNavigate } from 'react-router-dom'
import {faArrowRotateLeft} from "Icons";
import InlineButton from "../../../../../lib-components/atoms/buttons/InlineButton";
import colors from "../../../../../lib-customizations/common";
import {defaultMargins} from "../../../../../lib-components/white-space";

interface Props {
  child: Child
  unitId: string
  groupIdOrAll: string | 'all'
}

export default React.memo(function AttendanceChildDeparted({
  child,
  unitId,
  groupIdOrAll
}: Props) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  return (
    <ReturnToPresentButton
      icon={faArrowRotateLeft}
      text={i18n.attendances.actions.returnToPresent}
      onClick={() => navigate(`/units/${unitId}/groups/${groupIdOrAll}/child-attendance/${child.id}/mark-present`)}
      data-qa="return-to-present-btn"
    />
  )
})

export const ReturnToPresentButton = styled(InlineButton)`
  color: ${colors.main.m2};
  margin-top: ${defaultMargins.s};
  margin-left: ${defaultMargins.s};
  margin-bottom: ${defaultMargins.s};
  width: calc(100vw - 50px);
  display: flex;
  justify-content: center;
`
