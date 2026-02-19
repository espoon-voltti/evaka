<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Component Library (lib-components)

## Purpose

Reference for eVaka's component library - what components exist and when to use them. Focuses on practical component selection rather than implementation details.

**Implementation**: `frontend/src/lib-components/`

## Component Naming Patterns

- **F-postfix** (e.g., `InputFieldF`, `CheckboxF`) - Integrates with the [form framework](forms.md), accepts `bind` prop for automatic state/validation handling
- **Mutate-prefix** (e.g., `MutateButton`, `MutateFormModal`) - Integrates with mutations from the [query framework](queries.md), handles async state and loading indicators

## Buttons & Actions

**Button** - Standard button for most use cases
- `appearance="button"` (default): Full button with border and background, supports `primary` prop
  - `primary={true}`: Filled with primary color
  - `primary={false}` (default): Outlined style
- `appearance="inline"`: No border/background, inline element
- `appearance="link"`: Styled like a link with underline

**InlineLinkButton** - Link element (`<a>`) styled to look like an inline button (no border/background)

**LinkButton** - Link element (`<a>`) styled to look like a full button (border, padding, primary/secondary styles)

**MutateButton** - Button that triggers mutations asynchronously, shows loading spinner automatically

**ConfirmedMutation** - Button with confirmation modal for mutations (supports button, inline, or icon button styles)

**IconOnlyButton** - Button displaying only an icon without text label

**MutateIconOnlyButton** - IconOnlyButton with mutation integration for async operations

**AddButton** - Large circular button with plus icon for adding items

**ResponsiveAddButton** - AddButton that hides text label on smaller screens

**ReturnButton** - Button with left arrow icon for returning to previous page (calls history.back() by default)

**AsyncButton** - ⚠️ Deprecated, use MutateButton instead

**AsyncIconOnlyButton** - ⚠️ Deprecated, use MutateIconOnlyButton instead

**LegacyButton** - ⚠️ Deprecated, use Button instead

**LegacyInlineButton** - ⚠️ Deprecated, use Button with appearance="inline" instead

## Form Inputs

Standard components work standalone. F-postfix variants integrate with the [form framework](forms.md) for automatic validation, error display, and state management.

**InputField / InputFieldF** - Text input with label, info text, and error display

**TextArea / TextAreaF** - Multi-line text input

**PasswordInputF** - Password input with show/hide toggle

**PinInput** - PIN code input with separate boxes for each digit

**Checkbox / CheckboxF** - Single checkbox with label

**Radio** - Radio button option (use within a radio button group)

**Select / SelectF** - Dropdown select menu

**Combobox** - Searchable dropdown with typeahead filtering

**MultiSelect** - Select multiple options from a dropdown

**TreeDropdown** - Hierarchical dropdown with checkboxes for selecting items from a tree structure (supports parent-child relationships with indeterminate states)

**SelectionChip** - Checkbox styled as a pill/chip button

**DatePicker / DatePickerF** - Finnish date input with calendar picker

**DatePickerLowLevel** - Date picker with direct string value/onChange for custom state management

**DateRangePicker / DateRangePickerF** - Select date range (start and end dates)

**DateRangePickerLowLevel** - Date range picker with direct string value/onChange for custom state management

**TimeInput / TimeInputF** - Time picker (hours and minutes)

**TimeRangeInput / TimeRangeInputF** - Time range picker (start and end times)

**FileUpload** - File upload component with file size/extension validation and uploaded file list display

**FileDownloadButton** - Button for downloading files with file name and icon

## Modals & Dialogs

**PlainModal** - Minimal modal (background + container only, no padding/title/buttons) for fully custom content

**BaseModal** - Standard modal with title, optional text/icon, and close button - base for other modals

**InfoModal** - Display information with action buttons (OK, or OK/Cancel) - does not wrap content in `<form>`

**FormModal** - Modal wrapping content in `<form>` element with submit handling and resolve/reject buttons

**MutateFormModal** - FormModal with mutation integration for async form submission

**AsyncFormModal** - ⚠️ Deprecated, use MutateFormModal instead

## State & Feedback

**Toast** - Notification message that can be manually dismissed

**TimedToast** - Toast with configurable display duration that auto-dismisses

**Notifications** - Global notification system (context provider + rendering component) for displaying Toast/TimedToast messages, includes built-in offline detection and app update prompts

**MessageBox** - Inline message container with icon, border, and background color
- **InfoBox** - Info styling (info icon, blue color)
- **AlertBox** - Warning styling (exclamation icon, warning color)
- **ErrorBox** - Error styling (exclamation icon, danger color)

**SpinnerSegment** - Loading spinner in a segment/card layout

**SpinnerOverlay** - Loading spinner with dimmed overlay (absolute positioning, covers parent)

**FullScreenDimmedSpinner** - Full-screen loading spinner (fixed positioning, covers viewport)

**ErrorSegment** - Styled segment for displaying error messages

**ErrorPage** - Full-page error display

## Layout & Containers

**Gap** - Spacing element (horizontal or vertical) with configurable size

**FixedSpaceRow** - Horizontal flex container with consistent spacing between children

**FixedSpaceColumn** - Vertical flex container with consistent spacing between children

**FixedSpaceFlexWrap** - Wrapping flex container with consistent spacing

**AlignRight** - Container that aligns content to the right

**Container** - Page-level container that limits max width and centers content (use `wide` prop for wider layouts)

**ContentArea** - Section container with title, shadow, and background

**CollapsibleContentArea** - ContentArea that can be expanded/collapsed

**ExpandingInfo** - Expandable section with info icon that reveals additional content

**CollapsibleSection** - Section with title that can be expanded/collapsed

**ListGrid** - Grid layout for lists with configurable columns

**Table** - Styled table component

**AdaptiveFlex** - Flex container that adapts layout based on screen size

**MobileOnly** - Show content only on mobile screens

**MobileAndTablet** - Show content on mobile and tablet screens

**TabletAndDesktop** - Show content on tablet and desktop screens

**Desktop** - Show content only on desktop screens

**ScreenReaderOnly** - Visually hidden content accessible to screen readers

**PageWrapper** - Page-level wrapper container

**StickyFooter** - Footer that sticks to the bottom of the viewport

## Navigation

**NavLink** - Internal navigation link with automatic active state detection

**ExternalLink** - External link with icon indicator (opens in new tab by default)

**Tabs** - Tab navigation with click handlers for switching between views

**TabLinks** - Tab navigation using routing links (changes URL)

**Pagination** - Paginated navigation controls

**SkipToContent** - Accessibility link for keyboard users to skip to main content (hidden until focused)

## Data Display

**StaticChip** - Display-only chip with colored background and text

**IconChip** - Chip with icon and label, supports custom colors

**RoundIcon** - Icon displayed in a circular background

**StatusIcon** - Icon for displaying status (warning, success, etc.)

**PlacementCircle** - Circular indicator for placement visualization (full day vs. part day)

**Tooltip** - Hover tooltip for displaying additional information

**LabelValueList** - Grid layout for displaying label-value pairs

**OptionalLabelledValue** - Label-value pair that only renders if value exists

**OrderedList** - Styled ordered list

**UnorderedList** - Styled unordered list

**ExpandableList** - List that can be expanded or collapsed

**Linkify** - Converts URLs in text to clickable links

**RoundImage** - Image displayed in a circular frame

**HorizontalLine** - Horizontal divider line

## Icons

eVaka uses FontAwesome icons via the `lib-icons` package. A curated subset of icons are exported to manage the mapping between free and pro FontAwesome icons.

**Usage:**
```typescript
import { faCheck, faExclamation } from 'lib-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

<FontAwesomeIcon icon={faCheck} />
```

**Available icons:** Import from `lib-icons` - includes both free and pro FontAwesome icons pre-selected for the application

**Icon display components:** See RoundIcon, StatusIcon, and IconChip in the Data Display section

## Typography

**H1, H2, H3, H4** - Heading components

**P** - Paragraph component

**Label** - Label element for form fields

**LabelLike** - Non-semantic element styled like a label

**Bold, Strong, Italic, Light, Dimmed** - Text styling components

**Title** - ⚠️ Deprecated, use H1-H4 instead
