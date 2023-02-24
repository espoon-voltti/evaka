// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { AbsenceType } from 'lib-common/generated/api-types/daycare'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { farStickyNote } from 'lib-icons'

import { renderResult } from '../../async-rendering'
import { groupNotesQuery } from '../../child-notes/queries'
import ChildNameBackButton from '../../common/ChildNameBackButton'
import { Actions, CustomTitle, DailyNotes } from '../../common/components'
import { useTranslation } from '../../common/i18n'
import { TallContentArea } from '../../pairing/components'
import DailyNote from '../DailyNote'
import { childrenQuery, createFullDayAbsenceMutation } from '../queries'
import { useChild } from '../utils'

import AbsenceSelector from './AbsenceSelector'

export default React.memo(function MarkAbsent() {
  const navigate = useNavigate()
  const { i18n } = useTranslation()

  const { childId, unitId, groupId } = useNonNullableParams<{
    unitId: string
    groupId: string
    childId: string
  }>()
  const child = useChild(useQueryResult(childrenQuery(unitId)), childId)

  const [selectedAbsenceType, setSelectedAbsenceType] = useState<
    AbsenceType | undefined
  >(undefined)

  const { mutateAsync: createAbsence } = useMutationResult(
    createFullDayAbsenceMutation
  )

  const groupNotes = useQueryResult(groupNotesQuery(groupId))

  return (
    <TallContentArea
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      {renderResult(combine(child, groupNotes), ([child, groupNotes]) => (
        <>
          <ChildNameBackButton child={child} onClick={() => navigate(-2)} />
          <ContentArea
            shadow
            opaque={true}
            paddingHorizontal="s"
            paddingVertical="m"
          >
            <AbsenceWrapper>
              <CustomTitle>{i18n.attendances.actions.markAbsent}</CustomTitle>
              <Gap size="m" />
              <FixedSpaceColumn spacing="s">
                <AbsenceSelector
                  selectedAbsenceType={selectedAbsenceType}
                  setSelectedAbsenceType={setSelectedAbsenceType}
                />
              </FixedSpaceColumn>
            </AbsenceWrapper>
            <Gap size="m" />
            <Actions>
              <FixedSpaceRow fullWidth>
                <Button
                  text={i18n.common.cancel}
                  onClick={() => navigate(-1)}
                />
                {selectedAbsenceType ? (
                  <AsyncButton
                    primary
                    text={i18n.common.confirm}
                    onClick={() =>
                      createAbsence({
                        unitId,
                        childId,
                        absenceType: selectedAbsenceType
                      })
                    }
                    onSuccess={() => {
                      navigate(-1)
                    }}
                    data-qa="mark-absent-btn"
                  />
                ) : (
                  <Button primary text={i18n.common.confirm} disabled={true} />
                )}
              </FixedSpaceRow>
            </Actions>
          </ContentArea>
          <Gap size="s" />
          <ContentArea
            shadow
            opaque={true}
            paddingHorizontal="s"
            paddingVertical="s"
            blue
          >
            <DailyNotes>
              <span>
                <RoundIcon
                  content={farStickyNote}
                  color={colors.main.m1}
                  size="m"
                />
              </span>
              <DailyNote
                child={child ? child : undefined}
                groupNotes={groupNotes}
              />
            </DailyNotes>
          </ContentArea>
        </>
      ))}
    </TallContentArea>
  )
})

const AbsenceWrapper = styled.div`
  display: flex;
  flex-direction: column;
`
