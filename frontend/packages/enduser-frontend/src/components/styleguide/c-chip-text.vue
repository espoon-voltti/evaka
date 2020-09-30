<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div
    class="chip"
    :class="{
      blue: blue,
      green: green,
      teal: teal,
      yellow: yellow,
      disabled: disabled,
      'raised-orange': raisedOrange,
      'raised-blue': raisedBlue,
      'raised-disabled': raisedDisabled
    }"
  >
    <slot />
  </div>
</template>

<script>
  export default {
    name: 'c-chip-text',
    props: {
      blue: {
        type: Boolean,
        default: false
      },
      green: {
        type: Boolean,
        default: false
      },
      teal: {
        type: Boolean,
        default: false
      },
      yellow: {
        type: Boolean,
        default: false
      },
      disabled: {
        type: Boolean,
        default: false
      },
      raisedOrange: {
        type: Boolean,
        default: false
      },
      raisedBlue: {
        type: Boolean,
        default: false
      },
      raisedDisabled: {
        type: Boolean,
        default: false
      }
    }
  }
</script>

<style lang="scss" scoped>
  .chip {
    align-items: center;
    background-color: $white;
    border-radius: 15px;
    color: $black;
    display: inline-flex;
    font-size: 14px;
    font-weight: 600;
    height: auto;
    justify-content: center;
    line-height: 1.5;
    padding-left: 0.75em;
    padding-right: 0.75em;
    white-space: nowrap;

    &.raised-orange {
      border: 1px solid $orange;
    }
    &.raised-blue {
      border: 1px solid $blue;
    }
    &.raised-disabled {
      border: 1px solid $grey;
    }
    &.blue {
      background-color: $hard-blue;
      color: $white !important;
    }
    &.green {
      background-color: $green;
      color: $black !important;
    }
    &.teal {
      background-color: $rock-blue;
      color: $black !important;
    }
    &.yellow {
      background-color: $yellow;
      color: $black !important;
    }
    &.disabled {
      background-color: $grey !important;
      color: $white !important;
    }
  }
</style>
