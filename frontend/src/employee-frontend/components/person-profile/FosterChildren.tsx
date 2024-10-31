// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext, useState } from 'react'
import { Link } from 'react-router-dom'

import { wrapResult } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import Tooltip from 'lib-components/atoms/Tooltip'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2, Label, P } from 'lib-components/typography'

import {
  createFosterParentRelationship,
  deleteFosterParentRelationship,
  getFosterChildren,
  updateFosterParentRelationshipValidity
} from '../../generated/api-clients/pis'
import { useTranslation } from '../../state/i18n'
import { PersonContext } from '../../state/person'
import { UIContext } from '../../state/ui'
import { NameTd } from '../PersonProfile'
import { renderResult } from '../async-rendering'
import { DbPersonSearch } from '../common/PersonSearch'
import Toolbar from '../common/Toolbar'

const createFosterParentRelationshipResult = wrapResult(
  createFosterParentRelationship
)
const deleteFosterParentRelationshipResult = wrapResult(
  deleteFosterParentRelationship
)
const getFosterChildrenResult = wrapResult(getFosterChildren)
const updateFosterParentRelationshipResult = wrapResult(
  updateFosterParentRelationshipValidity
)

interface Props {
  id: UUID
  open: boolean
}

export default React.memo(function FosterChildren({
  id,
  open: startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext(PersonContext)
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const [open, setOpen] = useState(startOpen)
  const [editing, setEditing] = useState<{ id: UUID; validDuring: DateRange }>()
  const [deleting, setDeleting] = useState<UUID>()
  const [fosterChildren, reloadFosterChildren] = useApiState(
    () => getFosterChildrenResult({ parentId: id }),
    [id]
  )

  const startEditing = useCallback(
    (id: UUID, validDuring: DateRange) => {
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
    (id: UUID) => {
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
        <FosterChildCreationModal
          parentId={id}
          reload={reloadFosterChildren}
          close={clearUiMode}
        />
      )}
      {uiMode === 'edit-foster-child' && editing !== undefined && (
        <FosterChildEditingModal
          id={editing.id}
          initialValidDuring={editing?.validDuring}
          reload={reloadFosterChildren}
          close={stopEditing}
        />
      )}
      {uiMode === 'delete-foster-child' && deleting !== undefined && (
        <FosterChildDeleteConfirmationModal
          id={deleting}
          reload={reloadFosterChildren}
          close={stopDeleting}
        />
      )}
      <CollapsibleContentArea
        title={<H2>{i18n.personProfile.fosterChildren.sectionTitle}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="person-foster-children-collapsible"
      >
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
                            tooltip={i18n.common.form.lastModifiedBy(
                                    modifiedBy.name
                                  )
                            }
                            position="left"
                          >
                            {modifiedAt.format()}
                          </Tooltip>
                      </Td>
                      <Td>
                        <Toolbar
                          disableAll={!!uiMode}
                          onEdit={() =>
                            startEditing(relationshipId, validDuring)
                          }
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
      </CollapsibleContentArea>
    </>
  )
})

const FosterChildCreationModal = React.memo(function FosterChildCreationModal({
  parentId,
  reload,
  close
}: {
  parentId: UUID
  reload: () => Promise<unknown>
  close: () => void
}) {
  const { i18n, lang } = useTranslation()
  const [form, setForm] = useState<{
    childId: string | null
    validDuring: DateRange
  }>({
    childId: null,
    validDuring: new DateRange(LocalDate.todayInHelsinkiTz(), null)
  })

  const onSuccess = useCallback(() => {
    void reload().then(close)
  }, [reload, close])

  return (
    <AsyncFormModal
      title={i18n.personProfile.fosterChildren.addFosterChildTitle}
      resolveAction={() => {
        const childId = form.childId
        if (childId === null) {
          return
        }

        return createFosterParentRelationshipResult({
          body: {
            parentId: parentId,
            childId,
            validDuring: form.validDuring
          }
        })
      }}
      resolveLabel={i18n.common.confirm}
      onSuccess={onSuccess}
      rejectAction={close}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={form.childId === null}
      data-qa="add-foster-child-modal"
    >
      <P>{i18n.personProfile.fosterChildren.addFosterChildParagraph}</P>
      <Label>{i18n.personProfile.fosterChildren.childLabel}</Label>
      <DbPersonSearch
        onResult={(person) => setForm({ ...form, childId: person?.id ?? null })}
        onlyChildren
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
    </AsyncFormModal>
  )
})

const FosterChildEditingModal = React.memo(function FosterChildEditingModal({
  id,
  initialValidDuring,
  reload,
  close
}: {
  id: UUID
  initialValidDuring: DateRange
  reload: () => Promise<unknown>
  close: () => void
}) {
  const { i18n, lang } = useTranslation()
  const [validDuring, setValidDuring] = useState(initialValidDuring)

  const onSuccess = useCallback(() => {
    void reload().then(close)
  }, [reload, close])

  return (
    <AsyncFormModal
      title={i18n.personProfile.fosterChildren.updateFosterChildTitle}
      resolveAction={() =>
        updateFosterParentRelationshipResult({ id, body: validDuring })
      }
      resolveLabel={i18n.common.confirm}
      onSuccess={onSuccess}
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
    </AsyncFormModal>
  )
})

const FosterChildDeleteConfirmationModal = React.memo(
  function FosterChildDeleteConfirmationModal({
    id,
    reload,
    close
  }: {
    id: UUID
    reload: () => Promise<unknown>
    close: () => void
  }) {
    const { i18n } = useTranslation()

    const onSuccess = useCallback(() => {
      void reload().then(close)
    }, [reload, close])

    return (
      <AsyncFormModal
        title={i18n.personProfile.fosterChildren.updateFosterChildTitle}
        resolveAction={() => deleteFosterParentRelationshipResult({ id })}
        resolveLabel={i18n.common.confirm}
        onSuccess={onSuccess}
        rejectAction={close}
        rejectLabel={i18n.common.cancel}
        data-qa="delete-foster-child-modal"
      >
        <P>{i18n.personProfile.fosterChildren.deleteFosterChildParagraph}</P>
      </AsyncFormModal>
    )
  }
)
