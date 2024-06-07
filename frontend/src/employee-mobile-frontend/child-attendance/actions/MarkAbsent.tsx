// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import {
  queryOrDefault,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useRouteParams from 'lib-common/useRouteParams'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { LegacyAsyncButton } from 'lib-components/atoms/buttons/LegacyAsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
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

import AbsenceSelector, { AbsenceTypeWithNoAbsence } from './AbsenceSelector'

export default React.memo(function MarkAbsent({ unitId }: { unitId: UUID }) {
  const navigate = useNavigate()
  const { i18n } = useTranslation()

  const { childId } = useRouteParams(['childId'])
  const child = useChild(useQueryResult(childrenQuery(unitId)), childId)

  const [selectedAbsenceType, setSelectedAbsenceType] = useState<
    AbsenceTypeWithNoAbsence | undefined
  >(undefined)

  const { mutateAsync: createAbsence } = useMutationResult(
    createFullDayAbsenceMutation
  )

  const groupId = child.map(({ groupId }) => groupId).getOrElse(null)
  const groupNotes = useQueryResult(
    queryOrDefault(groupNotesQuery, [])(groupId ? { groupId } : null)
  )

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
                  absenceTypes={[
                    'OTHER_ABSENCE',
                    'SICKLEAVE',
                    'UNKNOWN_ABSENCE',
                    'PLANNED_ABSENCE'
                  ]}
                  selectedAbsenceType={selectedAbsenceType}
                  setSelectedAbsenceType={setSelectedAbsenceType}
                />
              </FixedSpaceColumn>
            </AbsenceWrapper>
            <Gap size="m" />
            <Actions>
              <FixedSpaceRow fullWidth>
                <LegacyButton
                  text={i18n.common.cancel}
                  onClick={() => navigate(-1)}
                />
                {selectedAbsenceType !== undefined &&
                selectedAbsenceType !== 'NO_ABSENCE' ? (
                  <LegacyAsyncButton
                    primary
                    text={i18n.common.confirm}
                    onClick={() =>
                      createAbsence({
                        unitId,
                        childId,
                        body: {
                          absenceType: selectedAbsenceType
                        }
                      })
                    }
                    onSuccess={() => {
                      navigate(-1)
                    }}
                    data-qa="mark-absent-btn"
                  />
                ) : (
                  <LegacyButton
                    primary
                    text={i18n.common.confirm}
                    disabled={true}
                  />
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
