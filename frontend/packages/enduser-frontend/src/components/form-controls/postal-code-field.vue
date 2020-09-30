<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <validation :name="label" :value="value" :validators="getValidators">
    <div class="form-field">
      <label :for="name" class="label" :class="{ 'input-active': active }">
        {{ label }}<span v-if="label">:</span> <span v-show="required">*</span>
      </label>
      <p
        class="control"
        :class="{ 'has-icons-left': icon, 'input-disabled': disabled }"
      >
        <input
          class="input"
          type="number"
          min="0"
          max="99999"
          :name="name"
          :placeholder="placeholder"
          v-model="inputValue"
          @focus="active = true"
          @blur="handleBlur"
          :disabled="disabled"
        />
        <span v-if="icon" class="icon is-small is-left">
          <font-awesome-icon :icon="['far', icon]"></font-awesome-icon>
        </span>
      </p>
    </div>
  </validation>
</template>

<script>
  import Validation from '@/components/validation/validation.vue'
  import {
    isValidPostalCode,
    postalCode,
    required as requiredValidator
  } from '@/components/validation/validators.js'

  export default {
    components: {
      Validation
    },
    data() {
      return {
        active: false
      }
    },
    props: {
      value: String,
      name: String,
      label: String,
      required: {
        type: Boolean,
        default: false
      },
      validators: {
        type: Array,
        default: () => [postalCode]
      },
      icon: {
        type: String,
        default: 'home'
      },
      placeholder: '',
      disabled: Boolean
    },
    computed: {
      inputValue: {
        get() {
          return this.value
        },

        set(value) {
          if (isValidPostalCode(value)) {
            this.$emit('input', value)
          }
        }
      },

      getValidators() {
        return this.required
          ? [requiredValidator, ...this.validators]
          : this.validators
      }
    },

    methods: {
      handleBlur(e) {
        this.active = false
        this.$emit('input', e.target.value)
      }
    }
  }
</script>
