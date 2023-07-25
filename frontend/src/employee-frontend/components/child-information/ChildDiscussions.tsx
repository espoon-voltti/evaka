// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faPen, faTrash } from 'Icons'
import React, { Fragment, useCallback, useContext, useState } from 'react'
import styled from 'styled-components'

import { localDate } from 'lib-common/form/fields'
import { object } from 'lib-common/form/form'
import { useForm, useFormField } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import {
  ChildDiscussionData,
  ChildDiscussionWithPermittedActions
} from 'lib-common/generated/api-types/childdiscussion'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import Title from 'lib-components/atoms/Title'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import {
  createChildDiscussion,
  deleteChildDiscussion,
  getChildDiscussions,
  updateChildDiscussion
} from '../../api/child/child-discussions'
import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

interface Props {
  childId: UUID
}

const LineBreak = styled.div`
  width: 100%;
  border: 1px solid #d8d8d8;
  margin: 40px 0 20px 0;
`

export default React.memo(function ChildDiscussions({ childId }: Props) {
  const { i18n } = useTranslation()

  const { permittedActions } = useContext(ChildContext)
  const [creationModalOpen, setCreationModalOpen] = useState(false)

  const [childDiscussionData, reloadDiscussionData] = useApiState(
    () => getChildDiscussions(childId),
    [childId]
  )

  return (
    <div>
      <FlexRow justifyContent="space-between">
        <Title size={4}>{i18n.childInformation.childDiscussion.title}</Title>
        {permittedActions.has('CREATE_CHILD_DISCUSSION') && (
          <AddButtonRow
            text={i18n.childInformation.childDiscussion.addNew}
            onClick={() => setCreationModalOpen(true)}
            data-qa="create-discussion"
          />
        )}
      </FlexRow>
      {permittedActions.has('CREATE_CHILD_DISCUSSION') && creationModalOpen && (
        <CreationModal
          childId={childId}
          onAfterSubmit={reloadDiscussionData}
          onClose={() => setCreationModalOpen(false)}
        />
      )}
      {renderResult(childDiscussionData, (discussions) => (
        <ChildDiscussionSummary
          discussions={discussions}
          reloadDiscussionData={reloadDiscussionData}
        />
      ))}
    </div>
  )
})

const CreationModal = React.memo(function CreationModal({
  childId,
  onAfterSubmit,
  onClose
}: {
  childId: UUID
  onAfterSubmit: () => any | null
  onClose: () => void
}) {
  const { i18n, lang } = useTranslation()

  const form = useForm(
    discussionForm,
    () => ({
      offeredDate: null,
      heldDate: null,
      counselingDate: null
    }),
    i18n.validationErrors
  )

  const offeredDateState = useFormField(form, 'offeredDate')
  const heldDateState = useFormField(form, 'heldDate')
  const counselingDateState = useFormField(form, 'counselingDate')

  const submit = async () => {
    const res = await createChildDiscussion(childId, form.value())
    return res
  }

  const handleOnSuccess = () => {
    if (onAfterSubmit) {
      onAfterSubmit()
    }
    onClose()
  }

  return (
    <AsyncFormModal
      title={i18n.childInformation.childDiscussion.addNew}
      resolveAction={submit}
      onSuccess={handleOnSuccess}
      resolveLabel={i18n.common.confirm}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!form.isValid()}
    >
      <FixedSpaceRow alignItems="center" justifyContent="center">
        <Label>{i18n.childInformation.childDiscussion.offered}</Label>
        <DatePickerF
          bind={offeredDateState}
          locale={lang}
          data-qa="add-discussion-offered-date-input"
        />
      </FixedSpaceRow>
      <Gap />
      <FixedSpaceRow alignItems="center" justifyContent="center">
        <Label>{i18n.childInformation.childDiscussion.held}</Label>
        <DatePickerF
          bind={heldDateState}
          locale={lang}
          data-qa="add-discussion-held-date-input"
        />
      </FixedSpaceRow>
      <Gap />
      <FixedSpaceRow alignItems="center" justifyContent="center">
        <Label>{i18n.childInformation.childDiscussion.counseling}</Label>
        <DatePickerF
          bind={counselingDateState}
          locale={lang}
          data-qa="add-discussion-counseling-date-input"
        />
      </FixedSpaceRow>
    </AsyncFormModal>
  )
})

const ChildDiscussionSummary = React.memo(function ChildDiscussionSummary({
  discussions,
  reloadDiscussionData
}: {
  discussions: ChildDiscussionWithPermittedActions[]
  reloadDiscussionData: () => void
}) {
  return (
    <>
      {discussions.map((discussion, index) => (
        <Fragment key={discussion.data.id}>
          <ChildDiscussionRow
            discussion={discussion}
            reloadDiscussionData={reloadDiscussionData}
          />
          {index != discussions.length - 1 && <LineBreak />}
        </Fragment>
      ))}
    </>
  )
})

const discussionForm = object({
  offeredDate: localDate,
  heldDate: localDate,
  counselingDate: localDate
})

function initialFormState(
  discussion: ChildDiscussionData
): StateOf<typeof discussionForm> {
  return {
    offeredDate: discussion.offeredDate,
    heldDate: discussion.heldDate,
    counselingDate: discussion.counselingDate
  }
}

const ChildDiscussionRow = React.memo(function ChildDiscussionRow({
  discussion,
  reloadDiscussionData
}: {
  discussion: ChildDiscussionWithPermittedActions
  reloadDiscussionData: () => void
}) {
  const { i18n, lang } = useTranslation()
  const { data: discussionData, permittedActions } = discussion

  const form = useForm(
    discussionForm,
    () => initialFormState(discussionData),
    i18n.validationErrors
  )

  const [editing, setEditing] = useState<boolean>(false)
  const offeredDateState = useFormField(form, 'offeredDate')
  const heldDateState = useFormField(form, 'heldDate')
  const counselingDateState = useFormField(form, 'counselingDate')

  const handleSaveDiscussion = useCallback(
    async () => await updateChildDiscussion(discussionData.id, form.value()),
    [discussionData.id, form]
  )

  const handleDeleteDiscussion = useCallback(async () => {
    await deleteChildDiscussion(discussionData.id)
    reloadDiscussionData()
  }, [discussionData.id, reloadDiscussionData])

  const startEdit = () => {
    setEditing(true)
    form.set(initialFormState(discussionData))
  }

  const handleCancel = () => {
    setEditing(false)
    form.set(initialFormState(discussionData))
  }

  const handleSuccess = () => {
    setEditing(false)
    reloadDiscussionData()
  }

  const hasChanges = () =>
    discussionData.offeredDate != offeredDateState.value() ||
    discussionData.heldDate != heldDateState.value() ||
    discussionData.counselingDate != counselingDateState.value()

  return (
    <div>
      <FlexRow justifyContent="space-between">
        <FixedSpaceColumn alignItems="center" justifyContent="center">
          <Label>{i18n.childInformation.childDiscussion.offered}</Label>
          {editing ? (
            <DatePickerF
              bind={offeredDateState}
              locale={lang}
              data-qa={`discussion-offered-date-input-${discussionData.id}`}
            />
          ) : (
            <span data-qa={`discussion-offered-date-${discussionData.id}`}>
              {offeredDateState.value()?.format()}
            </span>
          )}
        </FixedSpaceColumn>

        <FixedSpaceColumn alignItems="center" justifyContent="center">
          <Label>{i18n.childInformation.childDiscussion.held}</Label>
          {editing ? (
            <DatePickerF
              bind={heldDateState}
              locale={lang}
              data-qa={`discussion-held-date-input-${discussionData.id}`}
            />
          ) : (
            <span data-qa={`discussion-held-date-${discussionData.id}`}>
              {heldDateState.value()?.format()}
            </span>
          )}
        </FixedSpaceColumn>

        <FixedSpaceColumn alignItems="center" justifyContent="center">
          <Label>{i18n.childInformation.childDiscussion.counseling}</Label>
          {editing ? (
            <DatePickerF
              bind={counselingDateState}
              locale={lang}
              data-qa={`discussion-counseling-date-input-${discussionData.id}`}
            />
          ) : (
            <span data-qa={`discussion-counseling-date-${discussionData.id}`}>
              {counselingDateState.value()?.format()}
            </span>
          )}
        </FixedSpaceColumn>
        {editing ? (
          <FixedSpaceColumn alignItems="center" justifyContent="center">
            <Button onClick={handleCancel} text={i18n.common.cancel} />
            <AsyncButton
              primary
              disabled={!hasChanges()}
              onClick={handleSaveDiscussion}
              onSuccess={handleSuccess}
              data-qa={`confirm-edited-discussion-button-${discussionData.id}`}
              text={i18n.common.confirm}
            />
          </FixedSpaceColumn>
        ) : (
          <FixedSpaceColumn alignItems="center" justifyContent="center">
            <InlineButton
              disabled={!permittedActions.includes('UPDATE')}
              icon={faPen}
              onClick={startEdit}
              data-qa={`edit-discussion-button-${discussionData.id}`}
              text={i18n.common.edit}
            />
            <InlineButton
              disabled={!permittedActions.includes('DELETE')}
              icon={faTrash}
              onClick={handleDeleteDiscussion}
              data-qa={`delete-discussion-button-${discussionData.id}`}
              text={i18n.common.remove}
            />
          </FixedSpaceColumn>
        )}
      </FlexRow>
    </div>
  )
})
