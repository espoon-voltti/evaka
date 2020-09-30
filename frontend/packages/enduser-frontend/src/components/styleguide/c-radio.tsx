// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { CreateElement } from 'vue'
import { Prop } from 'vue-property-decorator'
import Component from 'vue-class-component'
import { set, get } from 'lodash'
import styled from 'vue-styled-components'
import BaseInput from './base-input'

@Component
class Radio extends BaseInput {
  @Prop()
  public name: string | string[]

  @Prop({
    required: true
  })
  public value: object

  @Prop({
    required: true
  })
  public inputValue: string

  @Prop()
  public description: string

  @Prop()
  public dataQa: string

  get modelValue(): string {
    return this.name ? get(this.value, this.name) : this.value || ''
  }

  public handleInput() {
    this.$emit('input', this.getUpdateValue(this.inputValue))
  }

  public getUpdateValue(value) {
    return set(
      {
        ...this.value
      },
      this.name,
      value
    )
  }

  public render(h: CreateElement) {
    return (
      <RadioWrapper class="radio">
        <div
          class="radiobutton"
          tabindex="0"
          onkeyup={this.handleKeyUp}
          role="radio"
          aria-checked={this.modelValue === this.inputValue}
          aria-labelledby={`${this.id}-label`}
        >
          <input
            class="input"
            type="radio"
            id={this.id}
            name={this.inputName}
            value={this.inputValue}
            disabled={this.disabled}
            onChange={this.handleInput}
            checked={this.modelValue === this.inputValue}
            ref="radiobutton"
            tabindex="-1"
            data-qa={this.dataQa}
          />
          <label id={`${this.id}-label`} class="label" for={this.id}>
            <div class="tick">
              <i class="fal fa-check" aria-hidden></i>
            </div>
            {this.label}
            <div class="description">{this.description}</div>
          </label>
          {this.$slots.default}
        </div>
      </RadioWrapper>
    )
  }

  public handleKeyUp(e: any) {
    if (e.keyCode === 13) {
      const el = this.$refs.radiobutton as HTMLElement
      el.click()
    }
  }
}

const RadioWrapper = styled.div`
  min-height: 35px; /* radio tick height */

  .radiobutton {
    display: flex;
    flex-direction: row;
    flex-wrap: nowrap;
  }

  &.radio {
    padding-left: 0;
  }

  label.label {
    display: inline-block;
    padding-top: 6px;
    padding-left: 42px;
    position: relative;

    &:before,
    .tick {
      position: absolute;
      left: 0;
      top: 0;
    }
  }

  .description {
    margin-top: 0.5rem;
    font-size: 0.9rem;
  }
`

export default Radio
