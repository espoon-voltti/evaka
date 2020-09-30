<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <button
    class="c-button button"
    :type="type"
    @click="$emit('click')"
    :class="{
      'is-primary': primary,
      'is-borderless': borderless,
      'is-outlined': outlined,
      'is-warning': warning,
      'is-danger': danger,
      'is-success': success,
      [`is-${size}`]: size !== '',
      'has-icon': icon
    }"
  >
    <font-awesome-icon v-if="icon" :icon="icon" />
    <slot />
  </button>
</template>

<script>
  export default {
    name: 'c-button',

    props: {
      size: {
        type: String,
        validator: (value) =>
          ['', 'small', 'wide', 'medium', 'large'].includes(value),
        default: ''
      },
      type: {
        type: String,
        default: 'button',
        validator: (value) => ['button', 'submit'].includes(value)
      },
      primary: {
        type: Boolean,
        default: false
      },
      borderless: {
        type: Boolean,
        default: false
      },
      outlined: {
        type: Boolean,
        default: false
      },
      warning: {
        type: Boolean,
        default: false
      },
      danger: {
        type: Boolean,
        default: false
      },
      success: {
        type: Boolean,
        default: false
      },
      icon: {
        type: [Array, Boolean],
        default: false
      }
    }
  }
</script>

<style lang="scss">
  .c-button {
    &.is-borderless {
      &:focus {
        box-shadow: none !important;
        outline: auto;
      }
    }

    &.is-wide {
      min-width: 160px;
    }

    svg {
      margin: 0 10px;
    }
  }
</style>
