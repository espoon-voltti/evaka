// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import {
  constantQuery,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useRouteParams from 'lib-common/useRouteParams'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'

import { renderResult } from '../../async-rendering'
import { groupNotesQuery } from '../../child-notes/queries'
import ChildNameBackButton from '../../common/ChildNameBackButton'
import { Actions, CustomTitle } from '../../common/components'
import { useTranslation } from '../../common/i18n'
import { TallContentArea } from '../../pairing/components'
import ChildNotesSummary from '../ChildNotesSummary'
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
    groupId ? groupNotesQuery({ groupId }) : constantQuery([])
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
                  <AsyncButton
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
          <ChildNotesSummary child={child} groupNotes={groupNotes} />
        </>
      ))}
    </TallContentArea>
  )
})

const AbsenceWrapper = styled.div`
  display: flex;
  flex-direction: column;
`
