<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <footer class="common-footer">
    <div>&copy; {{ $t('footer.espoo-label') }}</div>
    <div>
      <a
        :href="$t('footer.privacy-policy-link')"
        target="_blank"
        rel="noreferrer"
      >
        {{ $t('footer.privacy-policy-label') }}
      </a>
    </div>
    <div>
      <a
        :href="$t('footer.send-feedback-link')"
        target="_blank"
        rel="noreferrer"
      >
        {{ $t('footer.send-feedback-label') }}
      </a>
    </div>
  </footer>
</template>

<script>
  export default {
    name: 'footer-component'
  }
</script>

<style lang="scss">
  .common-footer {
    display: flex;
    flex-direction: row;
    justify-content: center;
    font-size: $size-small;
    a {
      color: $link;
    }
    div {
      display: inline-block;
      margin: 0 10px 0 10px;
    }
  }

  .map-footer {
    div {
      margin-left: 5px;
    }
  }

  @media screen and (min-width: 768px) {
    .map-footer {
      display: block;
    }
  }

  .space-footer {
    padding: 20px 0 20px 0;
  }
</style>
