<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
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
        v-model="inputValue"
        :name="name"
        :type="inputType"
        :placeholder="placeholder"
        @focus="active = true"
        @blur="handleBlur"
        :disabled="disabled"
        aria-required="required"
      />
      <span v-if="icon" class="icon is-small is-left">
        <font-awesome-icon :icon="['far', icon]"></font-awesome-icon>
      </span>
    </p>
  </div>
</template>

<script>
  export default {
    data() {
      return {
        active: false
      }
    },
    props: {
      value: String,
      name: String,
      label: String,
      placeholder: String,
      required: Boolean,
      icon: String,
      inputType: {
        default: 'text',
        type: String
      },
      disabled: Boolean
    },

    computed: {
      inputValue: {
        set(value) {
          this.$emit('input', value)
        },

        get() {
          return this.value
        }
      }
    },

    methods: {
      handleBlur(e) {
        this.active = false
        this.$emit('blur', e)
      }
    }
  }
</script>

<style lang="scss">
  .form-field {
    .input-disabled {
      cursor: not-allowed;

      .input {
        color: #8d8d8d;
        background-color: #f5f5f58a;
        border-color: #f5f5f58a;
      }

      .icon {
        color: #999;
      }
    }

    ::-webkit-input-placeholder {
      /* WebKit, Blink, Edge */
      opacity: 0.75;
      font-size: 85%;
      font-style: italic;
    }

    :-moz-placeholder {
      /* Mozilla Firefox 4 to 18 */
      opacity: 0.75;
      font-size: 85%;
      font-style: italic;
    }

    ::-moz-placeholder {
      /* Mozilla Firefox 19+ */
      opacity: 0.75;
      font-size: 85%;
      font-style: italic;
    }

    :-ms-input-placeholder {
      /* Internet Explorer 10-11 */
      opacity: 0.75;
      font-size: 85%;
      font-style: italic;
    }

    .control.has-icons-left {
      .icon {
        z-index: 0;
      }
    }
  }
</style>
