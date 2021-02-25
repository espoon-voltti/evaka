// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import ReactSelect from 'react-select'
import _ from 'lodash'
import { faTimes, faTrash } from '@evaka/lib-icons'
import colors from '@evaka/lib-components/src/colors'
import InputField, {
  TextArea
} from '@evaka/lib-components/src/atoms/form/InputField'
import IconButton from '@evaka/lib-components/src/atoms/buttons/IconButton'
import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import { defaultMargins, Gap } from '@evaka/lib-components/src/white-space'
import { Bulletin, IdAndName } from './types'

type Props = {
  bulletin: Bulletin
  groups: IdAndName[]
  onChange: (change: Partial<Bulletin>) => void
  onDeleteDraft: () => void
  onClose: () => void
  onSend: () => void
}

export default React.memo(function MessageEditor({
  bulletin,
  groups,
  onChange,
  onDeleteDraft,
  onClose,
  onSend
}: Props) {
  return (
    <Container>
      <TopBar>
        <span>Uusi tiedote</span>
        <IconButton icon={faTimes} onClick={onClose} white />
      </TopBar>
      <FormArea>
        <div>Vastaanottaja</div>
        <Gap size={'xs'} />
        <ReactSelect
          options={_.sortBy(groups, (g) => g.name.toLowerCase())}
          getOptionLabel={(u) => u.name}
          getOptionValue={(u) => u.id}
          value={groups.find((group) => group.id === bulletin.groupId) ?? null}
          onChange={(val) =>
            onChange({
              groupId: val
                ? 'length' in val
                  ? val.length > 0
                    ? val[0].id
                    : null
                  : val.id
                : null
            })
          }
        />
        <Gap size={'s'} />

        <div>Otsikko</div>
        <Gap size={'xs'} />
        <InputField
          value={bulletin.title}
          onChange={(title) => onChange({ title })}
        />
        <Gap size={'s'} />

        <div>Viesti</div>
        <Gap size={'xs'} />
        <TextArea
          value={bulletin.content}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
            onChange({ content: e.target.value })
          }
        />
        <Gap size={'s'} />
      </FormArea>
      <BottomRow>
        <InlineButton
          onClick={onDeleteDraft}
          text={'Hylkää luonnos'}
          icon={faTrash}
        />
        <Button
          text={'Lähetä'}
          primary
          onClick={onSend}
          disabled={
            !bulletin.groupId ||
            !groups.find((group) => group.id === bulletin.groupId)
          }
        />
      </BottomRow>
    </Container>
  )
})

const Container = styled.div`
  width: 680px;
  height: 835px;
  position: relative;
  right: 43px;
  bottom: 0;
  box-shadow: 0 60px 8px 8px rgba(15, 15, 15, 0.15);
  display: flex;
  flex-direction: column;
  background-color: ${colors.greyscale.white};
`

const TopBar = styled.div`
  width: 100%;
  height: 60px;
  background-color: ${colors.primary};
  color: ${colors.greyscale.white};
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: ${defaultMargins.m};
`

const FormArea = styled.div`
  width: 100%;
  flex-grow: 1;
  padding: ${defaultMargins.L} ${defaultMargins.L} ${defaultMargins.XXL};
`

const BottomRow = styled.div`
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: ${defaultMargins.L} ${defaultMargins.L} ${defaultMargins.XXL};
`
