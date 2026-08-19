// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import type { MutationDescription } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { tabletMin } from 'lib-components/breakpoints'
import { H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faLockAlt } from 'lib-icons'

import { useTranslation } from '../localization'

export const DataRow = styled.div<{ $highlighted?: boolean }>`
  display: flex;
  border: ${(p) => (p.$highlighted ? `1px solid ${p.theme.colors.grayscale.g15}` : 'none')};
  border-radius: 4px;
  padding: ${(p) => (p.$highlighted ? defaultMargins.s : '0.5rem 0')};

  @media (max-width: ${tabletMin}) {
    flex-direction: column;
    gap: ${defaultMargins.xxs};
  }
`

export const DataRowLabel = styled.label`
  font-weight: 600;
  width: 220px;

  @media (max-width: ${tabletMin}) {
    width: auto;
  }
`

export const DataRowValue = styled.span`
  flex: 1;
`

export const SectionTitle = styled(H2)`
  @media (max-width: 600px) {
    color: ${(p) => p.theme.colors.grayscale.g100};
  }
`

export const TitleAndEditRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
`

export const RowActions = styled.div`
  display: flex;
  align-items: center;
  height: 0;
`

const EditModeButtons = styled.div`
  display: flex;
  gap: ${defaultMargins.m};
`

interface EditableSectionHeaderProps<Arg, Data> {
  title: string
  editing: boolean
  onStartEditing: () => void
  onCancel: () => void
  mutation: MutationDescription<Arg, Data>
  onSave: () => Arg
  onSaveSuccess: () => void
  saveDisabled?: boolean
  canEdit?: boolean
  navigateToLogin?: () => void
}

export function EditableSectionHeader<Arg, Data>({
  title,
  editing,
  onStartEditing,
  onCancel,
  mutation,
  onSave,
  onSaveSuccess,
  saveDisabled = false,
  canEdit = true,
  navigateToLogin
}: EditableSectionHeaderProps<Arg, Data>) {
  const t = useTranslation()
  return (
    <TitleAndEditRow>
      <SectionTitle $noMargin>{title}</SectionTitle>
      <RowActions>
        {editing ? (
          <EditModeButtons>
            <Button
              appearance="inline"
              onClick={onCancel}
              text={t.common.cancel}
              data-qa="cancel"
            />
            <MutateButton
              primary
              text={t.common.save}
              mutation={mutation}
              onClick={onSave}
              onSuccess={onSaveSuccess}
              disabled={saveDisabled}
              data-qa="save"
            />
          </EditModeButtons>
        ) : (
          <Button
            appearance="inline"
            icon={canEdit ? undefined : faLockAlt}
            onClick={canEdit ? onStartEditing : navigateToLogin}
            text={t.common.edit}
            data-qa={canEdit ? 'start-editing' : 'start-editing-login'}
          />
        )}
      </RowActions>
    </TitleAndEditRow>
  )
}
