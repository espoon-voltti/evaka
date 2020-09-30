// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import classNames from 'classnames'
import { Input } from '~components/shared/alpha'
import { FeeAlterationType, PartialFeeAlteration } from '~types/fee-alteration'
import Select, { SelectOptionProps } from '../../common/Select'

interface Props {
  edited: PartialFeeAlteration
  setEdited: (v: PartialFeeAlteration) => void
  typeOptions: SelectOptionProps[]
}

function FeeAlterationRowInput({ edited, setEdited, typeOptions }: Props) {
  return (
    <>
      <Select
        options={typeOptions}
        value={edited.type}
        onChange={(e) =>
          setEdited({
            ...edited,
            type: e.target.value as FeeAlterationType
          })
        }
      />
      <Input
        type="number"
        value={edited.amount !== undefined ? edited.amount.toString() : ''}
        onChange={(e) =>
          setEdited({
            ...edited,
            amount: Math.max(0, Math.min(99999, Number(e.target.value)))
          })
        }
      />
      <div className="is-absolute-radio-container">
        <input
          type="radio"
          name="is-absolute"
          id="is-absolute-false"
          value="false"
          onChange={() =>
            setEdited({
              ...edited,
              isAbsolute: false
            })
          }
        />
        <label
          htmlFor="is-absolute-false"
          className={classNames({
            left: true,
            selected: !edited.isAbsolute
          })}
        >
          %
        </label>
        <input
          type="radio"
          name="is-absolute"
          id="is-absolute-true"
          value="true"
          onChange={() =>
            setEdited({
              ...edited,
              isAbsolute: true
            })
          }
        />
        <label
          htmlFor="is-absolute-true"
          className={classNames({
            right: true,
            selected: edited.isAbsolute
          })}
        >
          €
        </label>
      </div>
    </>
  )
}

export default FeeAlterationRowInput
