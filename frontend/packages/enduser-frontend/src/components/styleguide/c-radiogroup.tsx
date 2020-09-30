// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Vue, { CreateElement } from 'vue'
import { Prop } from 'vue-property-decorator'
import Component from 'vue-class-component'
import styled from 'vue-styled-components'

export interface RadioOption {
  value: string
  label: string
}

@Component
class RadioGroup extends Vue {
  @Prop({
    required: true
  })
  public value: object

  @Prop({
    required: true
  })
  public name: string | string[]

  @Prop()
  public label: string

  @Prop({
    required: true
  })
  public options: RadioOption[]

  public render(h: CreateElement) {
    return (
      <RadioGroupWrapper class="c-radio-group">
        <div class="c-radio-group-label">
          {this.label}
          {this.$slots.default}
        </div>
        <div class="c-radio-group-options">
          {this.options.map((o) => (
            <c-radio
              name={this.name}
              value={this.value}
              onChange={o.value}
              label={o.label}
              onInput={this.handleInput}
            />
          ))}
        </div>
      </RadioGroupWrapper>
    )
  }

  public handleInput(value: object) {
    this.$emit('input', value)
  }
}

const RadioGroupWrapper = styled.div`
  .c-radio-group-label {
    margin-bottom: 8px;
  }
`

export default RadioGroup
