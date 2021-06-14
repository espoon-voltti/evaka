// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import FormModal from '../../../../lib-components/molecules/modals/FormModal'
import InputField from '../../../../lib-components/atoms/form/InputField'
import { Label } from 'lib-components/typography'
import { useTranslation } from '../../../state/i18n'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import Combobox from '../../../../lib-components/atoms/form/Combobox'
import IconButton from '../../../../lib-components/atoms/buttons/IconButton'
import { faTrash } from '../../../../lib-icons'
import InlineButton from '../../../../lib-components/atoms/buttons/InlineButton'
import {
  VasuQuestion,
  VasuQuestionType,
  VasuQuestionTypes
} from '../vasu-content'

interface Props {
  onSave: (question: VasuQuestion) => void
  onCancel: () => void
}

export default React.memo(function CreateQuestionModal({
  onCancel,
  onSave
}: Props) {
  const { i18n } = useTranslation()
  const [type, setType] = useState<VasuQuestionType>('TEXT')
  const [name, setName] = useState('')
  const [options, setOptions] = useState([''])

  function createQuestion(): VasuQuestion {
    switch (type) {
      case 'TEXT':
        return {
          type: 'TEXT',
          name: name,
          multiline: false,
          value: ''
        }
      case 'CHECKBOX':
        return {
          type: 'CHECKBOX',
          name: name,
          value: false
        }
      case 'RADIO_GROUP':
        return {
          type: 'RADIO_GROUP',
          name: name,
          optionNames: options,
          value: null
        }
      case 'MULTISELECT':
        return {
          type: 'MULTISELECT',
          name: name,
          optionNames: options,
          minSelections: 0,
          maxSelections: null,
          value: []
        }
      default:
        throw Error('Unexpected type')
    }
  }

  return (
    <FormModal
      title="Uusi kysymys"
      resolve={{
        action: () => onSave(createQuestion()),
        label: i18n.common.confirm
      }}
      reject={{
        action: onCancel,
        label: i18n.common.cancel
      }}
    >
      <FixedSpaceColumn>
        <FixedSpaceColumn spacing="xxs">
          <Label>Kysymyksen tyyppi</Label>
          <Combobox
            items={VasuQuestionTypes}
            selectedItem={type}
            onChange={(t) => {
              if (t) setType(t)
            }}
            getItemDataQa={(t) => t.toString()}
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn spacing="xxs">
          <Label>Kysymys</Label>
          <InputField value={name} onChange={setName} />
        </FixedSpaceColumn>
        {(type === 'RADIO_GROUP' || type === 'MULTISELECT') && (
          <FixedSpaceColumn spacing="xxs">
            <Label>Vaihtoehdot</Label>
            {options.map((opt, i) => (
              <FixedSpaceRow spacing="xs" key={`opt-${i}`}>
                <InputField
                  value={opt}
                  onChange={(val) =>
                    setOptions([
                      ...options.slice(0, i),
                      val,
                      ...options.slice(i + 1)
                    ])
                  }
                  width="m"
                />
                <IconButton
                  icon={faTrash}
                  disabled={options.length < 2}
                  onClick={(e) => {
                    e.preventDefault()
                    setOptions([
                      ...options.slice(0, i),
                      ...options.slice(i + 1)
                    ])
                  }}
                />
              </FixedSpaceRow>
            ))}
            <InlineButton
              onClick={() => setOptions([...options, ''])}
              text={'Lisää vaihtoehto'}
            />
          </FixedSpaceColumn>
        )}
      </FixedSpaceColumn>
    </FormModal>
  )
})
