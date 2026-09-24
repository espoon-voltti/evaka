// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { localDate, string } from 'lib-common/form/fields'
import { nullBlank, object, required, validated } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { nonBlank } from 'lib-common/form/validators'
import { useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { MutateIconOnlyButton } from 'lib-components/atoms/buttons/MutateIconOnlyButton'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { H1, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPlus, faTrash } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import {
  createTodoItemMutation,
  deleteTodoItemMutation,
  todoItemsQuery
} from './queries'

const todoItemForm = object({
  description: validated(required(string()), nonBlank),
  deadline: nullBlank(localDate())
})

export default React.memo(function TodoItemsPage() {
  const { i18n } = useTranslation()
  const todoItems = useQueryResult(todoItemsQuery())
  const [addingNew, setAddingNew] = useState(false)

  return (
    <Container>
      <ContentArea $opaque>
        <H1>{i18n.todoItems.title}</H1>
        {addingNew ? (
          <TodoItemForm onClose={() => setAddingNew(false)} />
        ) : (
          <Button
            appearance="inline"
            icon={faPlus}
            text={i18n.todoItems.addNew}
            onClick={() => setAddingNew(true)}
            data-qa="add-new-button"
          />
        )}
        <Gap $size="L" />
        {renderResult(todoItems, (items) =>
          items.length === 0 ? (
            <P>{i18n.todoItems.noItems}</P>
          ) : (
            <Table>
              <Thead>
                <Tr>
                  <Th>{i18n.todoItems.description}</Th>
                  <Th $minimalWidth>{i18n.todoItems.createdAt}</Th>
                  <Th $minimalWidth>{i18n.todoItems.deadline}</Th>
                  <Th $minimalWidth />
                </Tr>
              </Thead>
              <Tbody>
                {items.map((item) => (
                  <Tr key={item.id} data-qa="todo-item">
                    <Td data-qa="todo-item-description">{item.description}</Td>
                    <Td $minimalWidth data-qa="todo-item-created-at">
                      {item.createdAt.format()}
                    </Td>
                    <Td $minimalWidth data-qa="todo-item-deadline">
                      {item.deadline?.format() ?? '–'}
                    </Td>
                    <Td $minimalWidth>
                      <MutateIconOnlyButton
                        icon={faTrash}
                        aria-label={i18n.common.remove}
                        mutation={deleteTodoItemMutation}
                        onClick={() => ({ id: item.id })}
                        data-qa="delete-button"
                      />
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )
        )}
      </ContentArea>
    </Container>
  )
})

const TodoItemForm = React.memo(function TodoItemForm({
  onClose
}: {
  onClose: () => void
}) {
  const { i18n, lang } = useTranslation()
  const form = useForm(
    todoItemForm,
    () => ({ description: '', deadline: localDate.empty() }),
    i18n.validationErrors
  )
  const { description, deadline } = useFormFields(form)

  return (
    <FixedSpaceColumn>
      <FixedSpaceRow $spacing="L">
        <FixedSpaceColumn $spacing="zero">
          <Label>{i18n.todoItems.description}</Label>
          <InputFieldF
            bind={description}
            width="L"
            autoFocus
            hideErrorsBeforeTouched
            data-qa="description-input"
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn $spacing="zero">
          <Label>{i18n.todoItems.deadline}</Label>
          <DatePickerF
            bind={deadline}
            locale={lang}
            hideErrorsBeforeTouched
            data-qa="deadline-input"
          />
        </FixedSpaceColumn>
      </FixedSpaceRow>
      <FixedSpaceRow>
        <MutateButton
          primary
          text={i18n.common.save}
          disabled={!form.isValid()}
          mutation={createTodoItemMutation}
          onClick={() => ({ body: form.value() })}
          onSuccess={onClose}
          data-qa="save-button"
        />
        <Button
          text={i18n.common.cancel}
          onClick={onClose}
          data-qa="cancel-button"
        />
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
})
