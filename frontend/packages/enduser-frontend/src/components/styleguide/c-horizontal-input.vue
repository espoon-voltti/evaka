<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="field is-horizontal">
    <div class="field-label">
      <label class="label">{{ label }}</label>
    </div>
    <div class="field-body">
      <div class="field">
        <div :class="['control', inputIcon ? 'has-icons-right' : undefined]">
          <input
            :class="['input', state ? `is-${state}` : undefined]"
            type="text"
            :placeholder="placeholder"
            v-model="_inputValue"
            @focus="active = true"
            @blur="handleBlur"
            :name="inputName"
          />
          <span class="icon is-right" v-if="inputIcon">
            <font-awesome-icon :icon="inputIcon"></font-awesome-icon>
          </span>
        </div>
        <p class="help" v-show="message">{{ message }}</p>
      </div>
    </div>
  </div>
</template>

<script>
  import Input from './c-input'

  export default {
    extends: Input
  }
</script>
