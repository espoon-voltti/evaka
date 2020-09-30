<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div :class="['message-box', { 'is-narrow': narrow }]">
    <div class="message-box-content-wrapper">
      <div class="message-box-title">
        <strong>{{ title }}</strong>
      </div>
      <div class="message-box-content">
        <slot />
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    props: {
      title: {
        type: String,
        required: true
      },
      narrow: {
        type: Boolean,
        default: false
      }
    }
  }
</script>

<style lang="scss" scoped>
  .message-box {
    margin-top: 1rem;
    margin-bottom: 2rem;
    padding: 16px;
    font-size: 0.9rem;
    background-color: #ffe78181;
    border-radius: 8px;
    word-break: break-word;
  }

  .is-narrow {
    margin: 0;
    padding: 4px 16px;
    width: fit-content;
  }
</style>
