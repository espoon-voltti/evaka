import { faPen } from 'Icons'
import React, { useCallback, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { fontWeights } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { UIContext, UiState } from '../../state/ui'
import { RequireRole } from '../../utils/roles'
import { FlexRow } from '../common/styled/containers'

import {
  childDiscussionQuery,
  createChildDiscussionMutation,
  updateChildDiscussionMutation
} from './queries'

interface Props {
  childId: UUID
}

const DataRow = styled.div`
  display: flex;
  min-height: 2rem;
`

const DataLabel = styled.div`
  width: 240px;
  padding: 0 40px 0 0;
  margin: 0;
  font-weight: ${fontWeights.semibold};
`

const DataValue = styled.div`
  display: flex;
`

const RightAlignedRow = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  padding-top: 20px;
`

export default React.memo(function ChildDiscussions({ childId }: Props) {
  const { i18n, lang } = useTranslation()

  const [offeredDate, setOfferedDate] = useState<LocalDate | null>(null)
  const [heldDate, setHeldDate] = useState<LocalDate | null>(null)
  const [counselingDate, setCounselingDate] = useState<LocalDate | null>(null)

  const childDiscussionData = useQueryResult(childDiscussionQuery(childId))

  useEffect(() => {
    if (!childDiscussionData.isSuccess) return

    setOfferedDate(childDiscussionData.value.offeredDate)
    setHeldDate(childDiscussionData.value.heldDate)
    setCounselingDate(childDiscussionData.value.counselingDate)
  }, [childDiscussionData])

  const { mutateAsync: createChildDiscussion } = useMutationResult(
    createChildDiscussionMutation
  )
  const { mutateAsync: updateChildDiscussion } = useMutationResult(
    updateChildDiscussionMutation
  )

  const { uiMode, toggleUiMode, clearUiMode } = useContext<UiState>(UIContext)
  const editing = uiMode == 'child-discussion-editing'

  const startEdit = useCallback(() => {
    if (childDiscussionData.isSuccess) {
      setOfferedDate(childDiscussionData.value.offeredDate)
      setHeldDate(childDiscussionData.value.heldDate)
      setCounselingDate(childDiscussionData.value.counselingDate)
    }
    toggleUiMode('child-discussion-editing')
  }, [toggleUiMode])

  const handleCancel = () => {
    if (childDiscussionData.isSuccess) {
      setOfferedDate(childDiscussionData.value.offeredDate)
      setHeldDate(childDiscussionData.value.heldDate)
      setCounselingDate(childDiscussionData.value.counselingDate)
    }
    clearUiMode()
  }

  const handleSaveChildDiscussion = () => {
    if (childDiscussionData.isSuccess && childDiscussionData.value.id) {
      return updateChildDiscussion({
        childId,
        data: {
          offeredDate,
          heldDate,
          counselingDate
        }
      })
    } else {
      return createChildDiscussion({
        childId,
        data: {
          offeredDate,
          heldDate,
          counselingDate
        }
      })
    }
  }

  const handleSuccess = () => {
    clearUiMode()
  }

  return (
    <div>
      <FlexRow justifyContent="space-between">
        <Title size={4}>{i18n.childInformation.childDiscussion.title}</Title>
        {!editing && (
          <RequireRole
            oneOf={[
              'UNIT_SUPERVISOR',
              'ADMIN',
              'STAFF',
              'SPECIAL_EDUCATION_TEACHER'
            ]}
          >
            <InlineButton
              icon={faPen}
              onClick={startEdit}
              data-qa="edit-child-discussion-button"
              text={i18n.common.edit}
            />
          </RequireRole>
        )}
      </FlexRow>
      <DataRow>
        <DataLabel>{i18n.childInformation.childDiscussion.offered}</DataLabel>
        <DataValue data-qa="child-discussion-offered-date">
          {editing ? (
            <DatePicker
              date={offeredDate}
              onChange={setOfferedDate}
              locale={lang}
              hideErrorsBeforeTouched
              data-qa="child-discussion-offered-date-input"
            />
          ) : (
            offeredDate?.format()
          )}
        </DataValue>
      </DataRow>
      <DataRow>
        <DataLabel>{i18n.childInformation.childDiscussion.held}</DataLabel>
        <DataValue data-qa="child-discussion-held-date">
          {editing ? (
            <DatePicker
              date={heldDate}
              onChange={setHeldDate}
              locale={lang}
              hideErrorsBeforeTouched
              data-qa="child-discussion-held-date-input"
            />
          ) : (
            heldDate?.format()
          )}
        </DataValue>
      </DataRow>
      <DataRow>
        <DataLabel>
          {i18n.childInformation.childDiscussion.counseling}
        </DataLabel>
        <DataValue data-qa="child-discussion-counseling-date">
          {editing ? (
            <DatePicker
              date={counselingDate}
              onChange={setCounselingDate}
              locale={lang}
              hideErrorsBeforeTouched
              data-qa="child-discussion-counseling-date-input"
            />
          ) : (
            counselingDate?.format()
          )}
        </DataValue>
      </DataRow>
      {editing && (
        <RightAlignedRow>
          <FixedSpaceRow>
            <Button onClick={handleCancel} text={i18n.common.cancel} />
            <AsyncButton
              primary
              disabled={false}
              onClick={handleSaveChildDiscussion}
              onSuccess={handleSuccess}
              data-qa="confirm-edited-child-discussion-button"
              text={i18n.common.confirm}
            />
          </FixedSpaceRow>
        </RightAlignedRow>
      )}
    </div>
  )
})
