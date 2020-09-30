// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Component } from 'vue-property-decorator'
import {
  timeString,
  required as requiredValidator
} from '../validation/input-validation'
import { isValidTimeString } from '../validation/validators'
import Input from './c-input'
import { get } from 'lodash'

function removeSeconds(val?: string): string {
  return val ? val.replace(/^(\d{2}:\d{2}):\d{2}/, '$1') : ''
}

@Component
class TimeInput extends Input {
  get inputValidators() {
    return [
      ...(this.required ? [requiredValidator] : []),
      timeString,
      ...this.validators
    ]
  }

  // overload to add quick-fix for unwanted seconds
  get _inputValue(): string {
    const val = get(this.value, this.name, '')
    return removeSeconds(val)
  }

  public handleInput(e: { target: HTMLInputElement }) {
    const emit = (str: string) => this.$emit('input', this.getUpdateValue(str))
    const hourPart = (str: string) => str.split(':')[0]
    const val = e.target.value as string

    if (isValidTimeString(val) && hourPart(val).length === 1) {
      emit('0' + val)
    } else {
      emit(val)
    }
  }
}

export default TimeInput

export { removeSeconds }
