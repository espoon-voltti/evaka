<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <validation :name="label" :value="value" :validators="getValidators">
    <div class="form-field">
      <label for="email" class="label" :class="{ 'input-active': active }">
        {{ label }}<span v-if="label">:</span> <span v-show="required">*</span>
      </label>
      <p class="control" :class="{ 'has-icons-left': icon }">
        <input
          class="input"
          name="email"
          id="email"
          v-model="inputValue"
          type="email"
          placeholder="nimi@sahkoposti.fi"
          @focus="active = true"
          @blur="handleBlur"
          :validators="required + 'email'"
          aria-required="true"
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
    required as requiredValidator,
    email,
    isValidEmail
  } from '@/components/validation/validators.js'

  export default {
    data() {
      return {
        active: false
      }
    },
    components: {
      Validation
    },
    props: {
      value: String,
      required: {
        type: Boolean,
        default: false
      },
      icon: {
        type: String,
        default: 'email'
      },
      validators: {
        type: Array,
        default: () => [email]
      },
      label: {
        type: String,
        default: 'Sähköposti'
      }
    },
    computed: {
      getValidators() {
        return this.required
          ? [requiredValidator, ...this.validators]
          : this.validators
      },

      inputValue: {
        get() {
          return this.value
        },

        set(value) {
          if (isValidEmail(value)) {
            this.$emit('input', value)
          }
        }
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
