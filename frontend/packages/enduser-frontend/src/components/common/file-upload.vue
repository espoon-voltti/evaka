<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="file-upload-container" v-on:drop.prevent v-on:dragover.prevent>
    <label class="file-input-label" v-on:drop="onDrop">
      <input
        type="file"
        accept="image/jpeg, image/png, application/pdf, application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document, application/vnd.oasis.opendocument.text"
        @change="onChange"
        data-qa="btn-upload-file"
      />
      <h4>{{ $t('file-upload.input.title') }}</h4>
      <span
        v-for="text in $t('file-upload.input.text')"
        v-bind:key="text"
        v-text="text"
      />
    </label>
    <div class="spacer" />
    <div class="files" data-qa="files">
      <div
        v-bind:class="{ uploaded: !inProgress(file) && !file.error }"
        class="file"
        v-for="file in files"
        v-bind:key="file.key"
      >
        <div class="file-icon-container">
          <font-awesome-icon
            :icon="['fal', fileIcon(file)]"
            size="lg"
          ></font-awesome-icon>
        </div>
        <div class="file-details">
          <div class="file-header">
            <a
              v-if="file.id"
              class="file-name"
              :href="`/api/application/attachments/${file.id}/download`"
              target="_blank"
              rel="noreferrer"
            >
              {{ file.name }}
            </a>
            <span v-else class="file-name">{{ file.name }}</span>
            <div>
              <button
                class="file-header-icon-button"
                @click="retryFile(file)"
                v-if="fileUploadFailed(file)"
              >
                <font-awesome-icon
                  :icon="['fal', 'redo']"
                  size="2x"
                ></font-awesome-icon>
              </button>
              <button
                class="file-header-icon-button"
                @click="deleteFile(file)"
                :disabled="inProgress(file) && !file.error"
              >
                <font-awesome-icon
                  :icon="['fal', 'times']"
                  size="2x"
                ></font-awesome-icon>
              </button>
            </div>
          </div>
          <transition name="progress-bar">
            <div class="file-progress-bar" v-show="inProgress(file)">
              <div class="file-progress-bar-background">
                <div
                  :class="[
                    'file-progress-bar-progress',
                    { error: !!file.error }
                  ]"
                  :style="progressBarStyles(file)"
                />
              </div>
              <div class="file-progress-bar-error" v-if="file.error">
                <span>{{ errorMessage(file) }}</span>
                <font-awesome-icon
                  :icon="['fas', 'exclamation-triangle']"
                  size="1x"
                ></font-awesome-icon>
              </div>
              <div class="file-progress-bar-details" v-else>
                <span>{{
                  inProgress(file)
                    ? $t('file-upload.loading')
                    : $t('file-upload.loaded')
                }}</span>
                <span>{{ file.progress }} %</span>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    props: {
      files: Array,
      onUpload: Function,
      onDelete: Function
    },
    methods: {
      onChange(event) {
        this.onUpload(event.target.files[0])
      },
      onDrop(event) {
        if (event.dataTransfer.files && event.dataTransfer.files[0]) {
          this.onUpload(event.dataTransfer.files[0])
        }
      },
      deleteFile(file) {
        this.onDelete(file)
      },
      retryFile(file) {
        this.onDelete(file)
        this.onUpload(file.file)
      },
      fileIcon(file) {
        switch (file.contentType) {
          case 'image/jpeg':
          case 'image/png':
            return 'file-image'
          case 'application/pdf':
            return 'file-pdf'
          case 'application/msword':
          case 'application/vnd.openxmlformats-officedocument.wordprocessingml.document':
          case 'application/vnd.oasis.opendocument.text':
            return 'file-word'
          default:
            return 'file'
        }
      },
      inProgress(file) {
        return !file.id
      },
      progressBarStyles(file) {
        return `width: ${file.progress}%;`
      },
      errorMessage(file) {
        const messages = this.$t('file-upload.error')
        return messages[file.error] || messages.default
      },
      fileUploadFailed(file) {
        return file.error === 'server-error'
      }
    }
  }
</script>

<style lang="scss">
  $progress-bar-height: 3px;
  $progress-bar-border-radius: 1px;

  .file-upload-container {
    display: flex;
    flex-direction: row;
    flex-wrap: wrap;
    align-items: flex-start;

    .file-input-label {
      display: flex;
      flex-direction: column;
      justify-content: center;
      background: $white-bis;
      border: 1px dashed $grey;
      border-radius: 8px;
      width: min(500px, 70vw);
      padding: 24px;
      text-align: center;

      input {
        display: none;
      }

      h4 {
        font-size: 18px;
        margin-bottom: 14px;
      }

      span {
        font-size: 13px;
      }
    }

    .spacer {
      padding: 20px;
    }

    .files {
      display: flex;
      flex-direction: column;

      > *:not(:last-child) {
        margin-bottom: 20px;
      }

      .file {
        display: flex;
        flex-direction: row;
        flex-wrap: nowrap;
        color: $grey-dark;

        .file-icon-container {
          margin-right: 16px;
          flex: 0;
        }

        .file-details {
          flex: 1;
          display: flex;
          flex-direction: column;
          flex-wrap: nowrap;

          .file-header {
            display: flex;
            flex-direction: row;
            flex-wrap: nowrap;
            justify-content: space-between;
            font-size: 15px;

            .file-header-icon-button {
              border: none;
              background: none;
              padding: 4px;
              margin-left: 12px;
              color: $grey;
              cursor: pointer;

              &:hover {
                color: $blue;
              }

              &:disabled {
                color: $grey-light;
                cursor: not-allowed;
              }
            }

            .file-name {
              width: min(400px, 50vw);
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }
          }

          .file-progress-bar {
            margin-top: 16px;
            font-size: 14px;

            > *:not(:last-child) {
              margin-bottom: 4px;
            }

            .file-progress-bar-background {
              position: relative;
              background: $grey;
              height: $progress-bar-height;
              border-radius: $progress-bar-border-radius;

              .file-progress-bar-progress {
                position: absolute;
                top: 0;
                left: 0;
                background: $blue;
                height: $progress-bar-height;
                border-radius: $progress-bar-border-radius;
                transition: width 0.5s ease-out;

                &.error {
                  background: $orange;
                }
              }
            }

            .file-progress-bar-details {
              display: flex;
              flex-direction: row;
              justify-content: space-between;
              color: $grey-dark;
            }

            .file-progress-bar-error {
              color: darken($orange, 10);

              svg {
                color: $orange;
                margin-left: 8px;
              }
            }
          }

          .progress-bar-leave-active {
            transition: opacity 0s ease-in 2s;
          }

          .progress-bar-leave-to {
            opacity: 0;
          }
        }
      }
    }
  }
</style>
