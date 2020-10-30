// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useContext } from 'react'
import { useTranslation } from '~state/i18n'
import { UIContext } from '~state/ui'
import FormModal from '~components/common/FormModal'
import Section from '~components/shared/layout/Section'
import { faPen } from 'icon-set'
import { DaycareGroup } from '~types/unit'
import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'
import { InfoBox } from '~components/common/MessageBoxes'
import { editGroup } from '~api/unit'
import InputField from '~components/shared/atoms/form/InputField'

interface Props {
  group: DaycareGroup
  reload: () => void
}

export default React.memo(function GroupRenameModal({ group, reload }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)

  const [name, setName] = useState<string>(group.name)

  const submitForm = () => {
    void editGroup(group.daycareId, group.id, name.trim()).then(() => {
      clearUiMode()
      reload()
    })
  }

  return (
    <FormModal
      data-qa="group-rename-modal"
      title={i18n.unit.groups.renameModal.title}
      icon={faPen}
      iconColour={'blue'}
      resolveLabel={i18n.common.confirm}
      rejectLabel={i18n.common.cancel}
      reject={() => clearUiMode()}
      resolveDisabled={name.trim().length === 0}
      resolve={() => submitForm()}
    >
      <FixedSpaceColumn>
        <Section>
          <div className="bold">{i18n.unit.groups.renameModal.name}</div>
          <InputField value={name} onChange={setName} />
        </Section>
        <InfoBox message={i18n.unit.groups.renameModal.info} thin />
      </FixedSpaceColumn>
    </FormModal>
  )
})
