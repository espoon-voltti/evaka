<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div
    class="tag"
    :class="{
      'is-primary': primary,
      'is-warning': warning,
      'is-danger': danger,
      'is-success': success,
      'is-light': light
    }"
  >
    <slot />
  </div>
</template>

<script>
  export default {
    name: 'c-tag',
    props: {
      light: {
        type: Boolean,
        default: true
      },
      primary: {
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
      }
    }
  }
</script>

<style lang="scss" scoped>
  .tag {
    &.is-light {
      color: $white;
      background-color: $grey;
    }
    &.is-primary {
      background-color: $soft-blue;
    }
  }
</style>
