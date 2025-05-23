// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext, useState } from 'react'
import { Link } from 'wouter'

import DateRange from 'lib-common/date-range'
import type {
  ChildId,
  FosterParentId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { cancelMutation, useQueryResult } from 'lib-common/query'
import Tooltip from 'lib-components/atoms/Tooltip'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label, P } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { renderResult } from '../async-rendering'
import { DbPersonSearch } from '../common/PersonSearch'
import Toolbar from '../common/Toolbar'

import { NameTd } from './common'
import {
  createFosterParentRelationshipMutation,
  deleteFosterParentRelationshipMutation,
  fosterChildrenQuery,
  updateFosterParentRelationshipValidityMutation
} from './queries'
import { PersonContext } from './state'

interface Props {
  id: PersonId
}

export default React.memo(function FosterChildren({ id }: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext(PersonContext)
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const [editing, setEditing] = useState<{
    id: FosterParentId
    validDuring: DateRange
  }>()
  const [deleting, setDeleting] = useState<FosterParentId>()
  const fosterChildren = useQueryResult(fosterChildrenQuery({ parentId: id }))

  const startEditing = useCallback(
    (id: FosterParentId, validDuring: DateRange) => {
      toggleUiMode('edit-foster-child')
      setEditing({ id, validDuring })
    },
    [toggleUiMode]
  )

  const stopEditing = useCallback(() => {
    clearUiMode()
    setEditing(undefined)
  }, [clearUiMode])

  const startDeleting = useCallback(
    (id: FosterParentId) => {
      toggleUiMode('delete-foster-child')
      setDeleting(id)
    },
    [toggleUiMode]
  )

  const stopDeleting = useCallback(() => {
    clearUiMode()
    setDeleting(undefined)
  }, [clearUiMode])

  return (
    <>
      {uiMode === 'add-foster-child' && (
        <FosterChildCreationModal parentId={id} close={clearUiMode} />
      )}
      {uiMode === 'edit-foster-child' && editing !== undefined && (
        <FosterChildEditingModal
          id={editing.id}
          parentId={id}
          initialValidDuring={editing?.validDuring}
          close={stopEditing}
        />
      )}
      {uiMode === 'delete-foster-child' && deleting !== undefined && (
        <FosterChildDeleteConfirmationModal
          id={deleting}
          parentId={id}
          close={stopDeleting}
        />
      )}
      {permittedActions.has('CREATE_FOSTER_PARENT_RELATIONSHIP') && (
        <AddButtonRow
          text={i18n.personProfile.fridgeChildAdd}
          onClick={() => {
            toggleUiMode('add-foster-child')
          }}
          disabled={!!uiMode}
          data-qa="add-foster-child-button"
        />
      )}
      {renderResult(fosterChildren, (fosterChildren) => (
        <Table>
          <Thead>
            <Tr>
              <Th>{i18n.common.form.name}</Th>
              <Th>{i18n.common.form.socialSecurityNumber}</Th>
              <Th>{i18n.common.form.age}</Th>
              <Th>{i18n.common.form.startDate}</Th>
              <Th>{i18n.common.form.endDate}</Th>
              <Th>{i18n.common.form.lastModified}</Th>
              <Th />
            </Tr>
          </Thead>
          <Tbody>
            {orderBy(
              fosterChildren,
              [({ child }) => child.lastName, ({ child }) => child.firstName],
              ['asc', 'asc']
            ).map(
              ({
                relationshipId,
                child,
                modifiedAt,
                modifiedBy,
                validDuring
              }) => (
                <Tr
                  key={relationshipId}
                  data-qa={`foster-child-row-${child.id}`}
                >
                  <NameTd data-qa="name">
                    <Link to={`/child-information/${child.id}`}>
                      {child.firstName} {child.lastName}
                    </Link>
                  </NameTd>
                  <Td>{child.socialSecurityNumber}</Td>
                  <Td>
                    {LocalDate.todayInHelsinkiTz().differenceInYears(
                      child.dateOfBirth
                    )}
                  </Td>
                  <Td data-qa="start">{validDuring.start.format()}</Td>
                  <Td data-qa="end">{validDuring.end?.format() ?? ''}</Td>
                  <Td>
                    <Tooltip
                      tooltip={i18n.common.form.lastModifiedBy(modifiedBy.name)}
                      position="left"
                    >
                      {modifiedAt.format()}
                    </Tooltip>
                  </Td>
                  <Td>
                    <Toolbar
                      disableAll={!!uiMode}
                      onEdit={() => startEditing(relationshipId, validDuring)}
                      onDelete={() => startDeleting(relationshipId)}
                      editable={true}
                      deletable={true}
                      dateRange={{
                        startDate: validDuring.start,
                        endDate: validDuring.end
                      }}
                      dataQaEdit="edit"
                      dataQaDelete="delete"
                    />
                  </Td>
                </Tr>
              )
            )}
          </Tbody>
        </Table>
      ))}
    </>
  )
})

const FosterChildCreationModal = React.memo(function FosterChildCreationModal({
  parentId,
  close
}: {
  parentId: PersonId
  close: () => void
}) {
  const { i18n, lang } = useTranslation()
  const [form, setForm] = useState<{
    childId: ChildId | null
    validDuring: DateRange
  }>({
    childId: null,
    validDuring: new DateRange(LocalDate.todayInHelsinkiTz(), null)
  })

  return (
    <MutateFormModal
      title={i18n.personProfile.fosterChildren.addFosterChildTitle}
      resolveMutation={createFosterParentRelationshipMutation}
      resolveAction={() =>
        form.childId
          ? {
              body: {
                parentId: parentId,
                childId: form.childId,
                validDuring: form.validDuring
              }
            }
          : cancelMutation
      }
      resolveLabel={i18n.common.confirm}
      onSuccess={close}
      rejectAction={close}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={form.childId === null}
      data-qa="add-foster-child-modal"
    >
      <P>{i18n.personProfile.fosterChildren.addFosterChildParagraph}</P>
      <Label>{i18n.personProfile.fosterChildren.childLabel}</Label>
      <DbPersonSearch
        onResult={(person) => setForm({ ...form, childId: person?.id ?? null })}
        ageLessThan={18}
        excludePeople={[parentId]}
        data-qa="person-search"
      />
      <Label>{i18n.personProfile.fosterChildren.validDuringLabel}</Label>
      <DateRangePicker
        start={form.validDuring.start}
        end={form.validDuring.end}
        onChange={(start, end) =>
          start
            ? setForm({
                ...form,
                validDuring: new DateRange(start, end)
              })
            : undefined
        }
        locale={lang}
      />
    </MutateFormModal>
  )
})

const FosterChildEditingModal = React.memo(function FosterChildEditingModal({
  id,
  parentId,
  initialValidDuring,
  close
}: {
  id: FosterParentId
  parentId: PersonId
  initialValidDuring: DateRange
  close: () => void
}) {
  const { i18n, lang } = useTranslation()
  const [validDuring, setValidDuring] = useState(initialValidDuring)

  return (
    <MutateFormModal
      title={i18n.personProfile.fosterChildren.updateFosterChildTitle}
      resolveMutation={updateFosterParentRelationshipValidityMutation}
      resolveAction={() => ({ id, parentId, body: validDuring })}
      resolveLabel={i18n.common.confirm}
      onSuccess={close}
      rejectAction={close}
      rejectLabel={i18n.common.cancel}
      data-qa="edit-foster-child-modal"
    >
      <Label>{i18n.personProfile.fosterChildren.validDuringLabel}</Label>
      <DateRangePicker
        start={validDuring.start}
        end={validDuring.end}
        onChange={(start, end) =>
          start ? setValidDuring(new DateRange(start, end)) : undefined
        }
        locale={lang}
      />
    </MutateFormModal>
  )
})

const FosterChildDeleteConfirmationModal = React.memo(
  function FosterChildDeleteConfirmationModal({
    id,
    parentId,
    close
  }: {
    id: FosterParentId
    parentId: PersonId
    close: () => void
  }) {
    const { i18n } = useTranslation()

    return (
      <MutateFormModal
        title={i18n.personProfile.fosterChildren.deleteFosterChildTitle}
        resolveMutation={deleteFosterParentRelationshipMutation}
        resolveAction={() => ({ id, parentId })}
        resolveLabel={i18n.common.confirm}
        onSuccess={close}
        rejectAction={close}
        rejectLabel={i18n.common.cancel}
        data-qa="delete-foster-child-modal"
      >
        <P>{i18n.personProfile.fosterChildren.deleteFosterChildParagraph}</P>
      </MutateFormModal>
    )
  }
)
