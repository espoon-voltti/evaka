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
import { faPlus } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { createTodoItemMutation, todoItemsQuery } from './queries'

const todoItemForm = object({
  description: validated(required(string()), nonBlank),
  deadline: nullBlank(localDate())
})

export default React.memo(function TodoItemsPage() {
  const todoItems = useQueryResult(todoItemsQuery())
  const [addingNew, setAddingNew] = useState(false)

  return (
    <Container>
      <ContentArea $opaque>
        <H1>Tehtävälista</H1>
        {addingNew ? (
          <TodoItemForm onClose={() => setAddingNew(false)} />
        ) : (
          <Button
            appearance="inline"
            icon={faPlus}
            text="Lisää uusi tehtävä"
            onClick={() => setAddingNew(true)}
            data-qa="add-new-button"
          />
        )}
        <Gap $size="L" />
        {renderResult(todoItems, (items) =>
          items.length === 0 ? (
            <P>Ei tehtäviä</P>
          ) : (
            <Table>
              <Thead>
                <Tr>
                  <Th>Kuvaus</Th>
                  <Th $minimalWidth>Luotu</Th>
                  <Th $minimalWidth>Määräpäivä</Th>
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
          <Label>Kuvaus</Label>
          <InputFieldF
            bind={description}
            width="L"
            autoFocus
            hideErrorsBeforeTouched
            data-qa="description-input"
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn $spacing="zero">
          <Label>Määräpäivä</Label>
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
          text="Tallenna"
          disabled={!form.isValid()}
          mutation={createTodoItemMutation}
          onClick={() => ({ body: form.value() })}
          onSuccess={onClose}
          data-qa="save-button"
        />
        <Button text="Peruuta" onClick={onClose} data-qa="cancel-button" />
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
})
