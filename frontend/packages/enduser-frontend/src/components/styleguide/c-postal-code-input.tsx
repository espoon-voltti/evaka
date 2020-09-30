// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Component } from 'vue-property-decorator'
import Input from './c-input'
import {
  required as requiredValidator,
  postalCode
} from '../validation/input-validation'

@Component
class PostalCodeInput extends Input {
  get inputValidators() {
    return [
      ...(this.required ? [requiredValidator] : []),
      postalCode,
      ...this.validators
    ]
  }
  public handleInput(e: any) {
    const v = e.target.value as string
    if (!postalCode(this.inputName, this.getValidationName)(v)) {
      return
    }
    this.$emit('input', this.getUpdateValue(v))
  }
}

export default PostalCodeInput
