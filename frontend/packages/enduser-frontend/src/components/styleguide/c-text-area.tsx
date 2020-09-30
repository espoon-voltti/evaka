// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Component, Prop } from 'vue-property-decorator'

import BaseInput from './base-input'
import { CreateElement } from 'vue'

@Component
class TextArea extends BaseInput {
  @Prop()
  public rows: number

  public render(h: CreateElement) {
    const { label, id, _inputValue, placeholder } = this
    return label ? (
      <div class="field">
        <label for={id}>
          {label}
          {this.$slots.default}
        </label>
        <textarea
          id={id}
          value={_inputValue}
          placeholder={placeholder}
          onfocus={this.handleFocus}
          onblur={this.handleBlur}
          name={this.inputName}
          oninput={this.handleInput}
          rows={this.rows}
        />
      </div>
    ) : (
      <div class="field">
        <textarea
          id={id}
          value={_inputValue}
          placeholder={placeholder}
          onfocus={this.handleFocus}
          onblur={this.handleBlur}
          name={this.inputName}
          oninput={this.handleInput}
          rows={this.rows}
          aria-label={placeholder}
        />
      </div>
    )
  }
}

export default TextArea

// <style lang="scss" scoped>
// textarea {
//   resize: y;
// }
// </style>
