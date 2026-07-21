---
version: alpha
name: paywith-design-analysis
description: A senior-focused fintech service built around calm trust, clear guidance, and selective protection. The base canvas uses a soft cool surface (#F4F9FA) with deep neutral text, while the primary cyan (#00B1D2) signals connection, safety, and active guidance without feeling overly clinical or controlling. The visual language is simple, spacious, and reassuring. Large typography, generous spacing, rounded surfaces, and clearly separated actions help senior users complete one task at a time with minimal cognitive load. Guardian-facing screens retain the same visual system but use denser information structures for transaction monitoring, alerts, and approval requests. Color is used functionally rather than decoratively. The primary color highlights key actions, linked-account states, and trusted interactions, while warning and danger colors are reserved strictly for transactions requiring attention. Cards and panels remain light and quiet, allowing risk status, transaction details, and approval decisions to stand out immediately. The overall brand should feel like a dependable financial companion rather than a surveillance or control tool: warm, accessible, modern, and respectful of the user’s independence.

colors:
  primary-50: "#001215"
  primary-100: "#00232A"
  primary-200: "#004754"
  primary-300: "#006A7E"
  primary-400: "#008EA8"
  primary-500: "#00B1D2"
  primary-600: "#33C1DB"
  primary-700: "#66D0E4"
  primary-800: "#99E0ED"
  primary-900: "#CCEFF6"
  gray-50: "#080D12"
  gray-100: "#121B21"
  gray-200: "#29343D"
  gray-300: "#414E58"
  gray-400: "#5C6770"
  gray-500: "#798086"
  gray-600: "#8F9AA3"
  gray-700: "#ABB4BB"
  gray-800: "#C4CDD4"
  gray-900: "#DFE6EC"
  background: "#F4F9FA"
  surface: "#F4F9FA"
  surface-card: "#FFFFFF"
  subbutton: "#BCC9CD"
  body: "#080D12"
  body-secondary: "#414E58"
  body-muted: "#5C6770"
  border: "#DFE6EC"
  border-strong: "#C4CDD4"
  focus: "#00B1D2"
  action: "#00B1D2"
  action-active: "#008EA8"
  on-action: "#FFFFFF"
  disabled: "#C4CDD4"
  on-disabled: "#5C6770"
  overlay: "#080D12"
  success: "#2FA737"
  success-alt: "#00BC00"
  warning: "#FF9F3F"
  error: "#FF6161"
  on-semantic: "#FFFFFF"

typography:
  hero:
    fontFamily: "'Pretendard', sans-serif"
    fontSize: 40px
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: -0.8px
  h1:
    fontFamily: "'Pretendard', sans-serif"
    fontSize: 32px
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: -0.64px
    margin: 16px
  h2:
    fontFamily: "'Pretendard', sans-serif"
    fontSize: 24px
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: -0.48px
  h3:
    fontFamily: "'Pretendard', sans-serif"
    fontSize: 20px
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: -0.4px
  h4:
    fontFamily: "'Pretendard', sans-serif"
    fontSize: 16px
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: -0.32px
  body-medium:
    fontFamily: "'Pretendard', sans-serif"
    fontSize: 14px
    fontWeight: 500
    lineHeight: 1.2
    letterSpacing: -0.28px
  body-regular:
    fontFamily: "'Pretendard', sans-serif"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.2
    letterSpacing: -0.28px
  caption:
    fontFamily: "'Pretendard', sans-serif"
    fontSize: 12px
    fontWeight: 400
    lineHeight: 1.2
    letterSpacing: -0.24px
  amount:
    fontFamily: "'Plus Jakarta Sans', sans-serif"
    fontSize: 40px
    fontWeight: 700
    lineHeight: 1.25
    letterSpacing: -0.8px
  numeric-input:
    fontFamily: "'Plus Jakarta Sans', sans-serif"
    fontSize: 24px
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: 0.48px
  numeric-input-large:
    fontFamily: "'Plus Jakarta Sans', sans-serif"
    fontSize: 32px
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: 1.28px

spacing:
  xxs: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 20px
  xl: 24px
  xxl: 32px
  section: 40px

radius:
  small: 4px
  medium: 8px
  large: 16px
  full: 9999px

opacity:
  disabled: 0.8
  overlay: 0.35
  loading: 0.5

border:
  default: "1px solid {colors.border}"
  strong: "1px solid {colors.border-strong}"
  focus: "2px solid {colors.focus}"
  error: "2px solid {colors.error}"

elevation:
  flat: none
  card: "0 2px 8px rgba(8, 13, 18, 0.06)"
  modal: "0 8px 24px rgba(8, 13, 18, 0.12)"

layout:
  baseUnit: 4px
  mobileGutter: 20px
  tabletGutter: 24px
  desktopGutter: 32px
  maxContentWidth: 1200px
  minTouchTarget: 48px

components:
  header:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.body}"
    height: 60px
    padding: "0 {spacing.lg}"
    border: "{border.default}"
  bottom-nav:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.body-secondary}"
    activeColor: "{colors.primary-300}"
    height: 80px
    border: "{border.default}"
  bottom-nav-ward:
    itemCount: 3
    centerActionSize: 120px
    centerActionRadius: "{radius.full}"
  bottom-nav-guardian:
    itemCount: 4
    itemWidth: 80px
    activeRadius: "{radius.full}"
  button-primary:
    backgroundColor: "{colors.action}"
    textColor: "{colors.on-action}"
    typography: "{typography.h4}"
    radius: "{radius.medium}"
    height: 56px
    padding: "0 {spacing.xl}"
  button-primary-active:
    backgroundColor: "{colors.action-active}"
    textColor: "{colors.surface-card}"
  button-secondary:
    backgroundColor: "{colors.disabled}"
    textColor: "{colors.body}"
    typography: "{typography.h4}"
    radius: "{radius.medium}"
    height: 56px
    padding: "0 {spacing.xl}"
  button-outline-primary:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.primary-500}"
    typography: "{typography.h4}"
    radius: "{radius.medium}"
    height: 56px
    padding: "0 {spacing.xl}"
    border: "2px solid {colors.primary-500}"
  button-text:
    backgroundColor: transparent
    textColor: "{colors.primary-300}"
    typography: "{typography.h4}"
    minHeight: "{layout.minTouchTarget}"
  button-danger:
    backgroundColor: "{colors.error}"
    textColor: "{colors.on-semantic}"
    typography: "{typography.h4}"
    radius: "{radius.medium}"
    height: 56px
    padding: "0 {spacing.xl}"
  button-outline-danger:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.error}"
    typography: "{typography.h4}"
    radius: "{radius.medium}"
    height: 56px
    padding: "0 {spacing.xl}"
    border: "2px solid {colors.error}"
  button-small:
    typography: "{typography.h4}"
    radius: "{radius.medium}"
    height: 40px
    padding: "0 {spacing.xl}"
  button-large:
    typography: "{typography.h1}"
    radius: "{radius.large}"
    height: 80px
    padding: "0 {spacing.xxl}"
  button-large-outline:
    borderWidth: 3px
  button-large-pill:
    radius: "{radius.full}"
  button-disabled:
    backgroundColor: "{colors.disabled}"
    textColor: "{colors.on-disabled}"
    opacity: "{opacity.disabled}"
  financial-summary-card:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.body}"
    radius: "{radius.large}"
    padding: "{spacing.xl}"
    border: "{border.default}"
    elevation: "{elevation.card}"
  account-card:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.body}"
    radius: "{radius.large}"
    padding: "{spacing.lg}"
    border: "{border.default}"
  transaction-row:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.body}"
    typography: "{typography.body-regular}"
    minHeight: 72px
    padding: "{spacing.md} 0"
    border: "{border.default}"
  alert-card:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.body}"
    statusColor: "{colors.warning}"
    radius: "{radius.medium}"
    padding: "{spacing.lg}"
    border: "{border.strong}"
  approval-card:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.body}"
    radius: "{radius.large}"
    padding: "{spacing.xl}"
    border: "{border.default}"
    elevation: "{elevation.card}"
  text-input:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.body}"
    typography: "{typography.h4}"
    radius: "{radius.medium}"
    height: 52px
    padding: "0 {spacing.md}"
    border: "{border.strong}"
  numeric-input:
    typography: "{typography.numeric-input}"
  numeric-input-large:
    typography: "{typography.numeric-input-large}"
  text-input-focus:
    border: "{border.focus}"
  text-input-error:
    border: "{border.error}"
    messageColor: "{colors.error}"
  status-badge:
    backgroundColor: "{colors.primary-900}"
    textColor: "{colors.primary-200}"
    typography: "{typography.caption}"
    radius: "{radius.full}"
    padding: "{spacing.xxs} {spacing.sm}"
  modal:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.body}"
    radius: "{radius.large}"
    padding: "{spacing.xl}"
    elevation: "{elevation.modal}"
  modal-backdrop:
    backgroundColor: "{colors.overlay}"
    opacity: "{opacity.overlay}"
  loading-state:
    color: "{colors.primary-500}"
    opacity: "{opacity.loading}"
    minHeight: "{layout.minTouchTarget}"
  empty-state:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.body-secondary}"
    typography: "{typography.body-regular}"
    radius: "{radius.large}"
    padding: "{spacing.xxl}"
  guardian-monitoring-list:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.body}"
    rowMinHeight: 64px
    border: "{border.default}"
---

## Overview

Paywith enables senior users to remain in control of their finances while receiving guardian assistance only when needed. Use the calm `{colors.background}` canvas with deep-neutral `{colors.body}` text, and reserve `{colors.primary-500}` for primary actions, trusted connections, and active guidance.

Senior-facing screens emphasize one task at a time through large headings, generous spacing, and separated action areas. Guardian-facing screens retain the same visual language but may use denser lists for transaction monitoring, anomaly alerts, and approval requests. Protective features must preserve the senior user's agency and must not feel like surveillance or control.

**Core principles:**

- Use color to communicate actions, connections, status, and risk—not as decoration.
- Prefer one primary action per screen and visually separate secondary actions.
- Present amount, counterparty, risk reason, and outcome in a predictable reading order.
- Never communicate risk through color alone. Pair it with an icon and a Korean label such as `주의`, `승인 필요`, or `오류`.
- Keep cards and panels visually quiet so transaction details and approval decisions remain prominent.

## Colors

### Primary

| Token | Value | Use |
|---|---:|---|
| `{colors.primary-50}` | `#001215` | Darkest emphasis background |
| `{colors.primary-100}` | `#00232A` | High-contrast brand background |
| `{colors.primary-200}` | `#004754` | Emphasis text |
| `{colors.primary-300}` | `#006A7E` | Links and selected-state text |
| `{colors.primary-400}` | `#008EA8` | Pressed state |
| `{colors.primary-500}` | `#00B1D2` | Primary action and focus |
| `{colors.primary-600}` | `#33C1DB` | Secondary emphasis |
| `{colors.primary-700}` | `#66D0E4` | Selected-region background |
| `{colors.primary-800}` | `#99E0ED` | Light information background |
| `{colors.primary-900}` | `#CCEFF6` | Status badge and guidance background |

Use `{colors.action}` for primary actions and `{colors.action-active}` for pressed states. Use the Figma-defined white `{colors.on-action}` on the bright cyan action surface.

### Neutral & Grayscale

Use `{colors.background}` and `{colors.surface}` for page foundations, and `{colors.surface-card}` for cards and form controls. Use `{colors.body}` for primary text, `{colors.body-secondary}` for supporting copy, and `{colors.body-muted}` for low-priority information.

Use `{colors.subbutton}` only for the neutral sub-button treatment defined by the Figma color system. Do not generalize it as a grayscale step or standard border color.

The grayscale runs from darkest `{colors.gray-50}` to lightest `{colors.gray-900}`. Use `{colors.border}` for standard dividers and `{colors.border-strong}` for stronger boundaries. Disabled controls use `{colors.disabled}` and `{colors.on-disabled}` with `{opacity.disabled}`, but must remain readable.

Avoid pure white and pure black as standalone brand colors or large page canvases. Their use is allowed when an approved component contract explicitly requires white foreground text or a white card/control surface, such as `{colors.on-action}`, `{colors.on-semantic}`, or `{colors.surface-card}`. Use `{colors.body}` instead of pure black for standard text.

### Semantic

- **Success** `{colors.success}`: completed connection, approval, or successful processing.
- **Success Alt** `{colors.success-alt}`: alternate success treatment when the component specification explicitly selects it. Do not substitute it for `{colors.success}` by default.
- **Warning** `{colors.warning}`: a transaction requiring attention but not immediate blocking.
- **Error** `{colors.error}`: failure, validation error, or blocked high-risk transaction.
- **Overlay** `{colors.overlay}`: modal backdrop with `{opacity.overlay}`.

Do not use semantic colors as large decorative fills. Pair them with an icon, explicit status name, and explanation. Use a readable foreground such as `{colors.on-semantic}`.

The Figma color frame still contains placeholder values for Secondary 50–900, Neutral Surface, Information, Disabled, and Overlay. Do not create or remap tokens from those placeholders until their variables are finalized.

## Typography

### Font Family

Use **Pretendard** for Korean UI copy with `sans-serif` as the fallback. Use **Plus Jakarta Sans** only for amounts, PIN values, and other numeric displays through `{typography.amount}`, `{typography.numeric-input}`, or `{typography.numeric-input-large}`. Default body copy to `{typography.body-regular}` and use larger heading tokens for critical status and action labels.

### Hierarchy

| Token | Size | Weight | Line Height | Letter Spacing | Use |
|---|---:|---:|---:|---:|---|
| `{typography.hero}` | 40px | 700 | 1.2 | -0.8px | Opening message |
| `{typography.h1}` | 32px | 700 | 1.2 | -0.64px | Page title with 16px margin |
| `{typography.h2}` | 24px | 700 | 1.2 | -0.48px | Major section title |
| `{typography.h3}` | 20px | 600 | 1.2 | -0.4px | Card title and critical status |
| `{typography.h4}` | 16px | 600 | 1.2 | -0.32px | Button and list title |
| `{typography.body-medium}` | 14px | 500 | 1.2 | -0.28px | Emphasized body and field label |
| `{typography.body-regular}` | 14px | 400 | 1.2 | -0.28px | Default copy and transaction detail |
| `{typography.caption}` | 12px | 400 | 1.2 | -0.24px | Supporting information and status badge |
| `{typography.amount}` | 40px | 700 | 1.25 | -0.8px | Balance and financial amount |
| `{typography.numeric-input}` | 24px | 600 | 1.2 | 0.48px | Standard numeric and PIN input |
| `{typography.numeric-input-large}` | 32px | 600 | 1.2 | 1.28px | Large numeric and PIN input |

Keep headings concise, but never clip them under zoom or at narrow widths. Format amounts with a currency unit and thousands separators. Long account and transaction names must have a way to reveal their full value.

## Layout

### Spacing System

Use the 4px `{layout.baseUnit}` with `{spacing.xxs}`, `{spacing.xs}`, `{spacing.sm}`, `{spacing.md}`, `{spacing.lg}`, `{spacing.xl}`, `{spacing.xxl}`, and `{spacing.section}`. Senior-facing screens use `{layout.mobileGutter}` as the default horizontal gutter. Keep related elements close and separate unrelated tasks with larger spacing.

### Grid & Container

- Default mobile layouts to one column with critical information and actions in vertical reading order.
- Use `{layout.tabletGutter}` on tablets and `{layout.desktopGutter}` on desktop.
- Cap wide layouts at `{layout.maxContentWidth}`.
- Guardian desktop screens may use two columns for summaries and transaction lists, but approval flows must remain linear.
- Reserve safe space so fixed navigation does not obscure content behind `{components.header.height}` or `{components.bottom-nav.height}`.

### Scroll Guidance

Every scrollable area must expose an obvious cue: a partially visible next card, visible scrollbar or indicator, or bottom fade. Horizontal scrolling requires clipped-content and directional cues. Never hide a critical financial action inside a horizontal scroller.

## Elevation

Use depth sparingly. Use `{elevation.flat}` for the canvas, `{elevation.card}` for cards, and `{elevation.modal}` only for modal decisions. Prefer `{border.default}` over heavy shadow for stable information boundaries.

Apply `{opacity.loading}` to a loading region and pair it with specific Korean status copy such as `불러오는 중입니다`. Do not fade the entire screen unnecessarily or make interactive availability ambiguous.

## Shapes

| Token | Value | Use |
|---|---:|---|
| `{radius.small}` | 4px | Small label and inline status |
| `{radius.medium}` | 8px | Button, input, and alert card |
| `{radius.large}` | 16px | Financial summary, approval card, and modal |
| `{radius.full}` | 9999px | Status badge and circular icon |

Components with the same role must share the same radius. Avoid excessively rounded financial cards. Distinguish actions through explicit labels, not shape alone.

## Components

### Header & Bottom Navigation

**`header`** — Height `{components.header.height}` with a card surface and standard divider. Include only the screen title, backward navigation, and essential secondary actions. Do not use back and close controls interchangeably.

**`bottom-nav`** — Shared 80px foundation for persistent role-specific navigation. Indicate the active item with `{components.bottom-nav.activeColor}` plus an icon and label. Every item must provide at least `{layout.minTouchTarget}` of interactive area.

**`bottom-nav-ward`** — Three destinations for the senior flow with a 120px circular center action. Reserve layout space for the raised center action so it never obscures page content.

**`bottom-nav-guardian`** — Four equal-width destinations for the guardian flow. Use an icon, label, and active pill together. The active pill background token remains pending until the dedicated Figma palette node is reviewed.

### Buttons

**Button sizes** — Use 40px for `small`, 56px for `default`, and 80px for `large`. Large buttons use `{typography.h1}`, `{radius.large}`, and may use `{radius.full}` for the pill variant. Large outline buttons use a 3px border; default outline buttons use a 2px border.

**`button-primary`** — The screen's default-size solid primary action. Use `{components.button-primary.height}`, `{components.button-primary.backgroundColor}`, and the Figma-defined white `{components.button-primary.textColor}`.

**`button-secondary`** — Use the solid neutral treatment for an alternative action, not necessarily cancellation.

**`button-outline-primary`** — Use a white surface, cyan text, and a 2px cyan border. It has the same semantic priority as its paired primary action but a quieter visual treatment.

**`button-text`** — Use for low-priority actions such as viewing details while preserving the minimum touch target.

**`button-danger`** — Use the solid error treatment with white text only for destructive or difficult-to-reverse actions such as blocking or disconnecting. Use `button-outline-danger` for the outlined counterpart. Explain the outcome before execution.

**`button-disabled`** — Explain the disabled reason in adjacent helper text. Never use an ambiguous label such as `확인`.

### Financial Cards & Lists

**`financial-summary-card`** — Shows total balance, available amount, and primary status at a glance. Give the amount the highest visual hierarchy and provide a control to conceal sensitive values.

**`account-card`** — Shows institution, account alias, masked account number, balance, and connection status. Avoid interaction conflicts between a clickable card and nested buttons.

**`transaction-row`** — Within `{components.transaction-row.minHeight}`, display merchant, date/time, amount, and status in a consistent order. Distinguish incoming and outgoing transactions with both a sign and text.

**`alert-card`** — Shows the risk reason, relevant transaction details, and recommended action. Never rely on `{components.alert-card.statusColor}` alone to convey severity.

**`approval-card`** — Shows requester, counterparty, amount, request time, and risk reason before presenting separate `거래 승인하기` and `거래 거절하기` actions.

**`guardian-monitoring-list`** — Provides a denser view of recent transactions and alerts. Rows must be at least `{components.guardian-monitoring-list.rowMinHeight}` high. Express filtered results and unread status in text.

### Forms & Status

**`text-input`** — Use `{components.text-input.height}` and an always-visible external label. Never rely on placeholder text as the only description of input purpose.

**`text-input-focus`** — Make keyboard and touch focus obvious with `{border.focus}`.

**`text-input-error`** — Pair `{border.error}` with a specific error message. Never clear the user's input automatically after validation fails.

**`status-badge`** — Combine status color and icon with Korean text such as `정상`, `주의`, `승인 필요`, or `처리 완료`.

### Modal, Loading & Empty State

**`modal`** — Request one decision at a time. Clearly separate title, impact explanation, primary action, and close action. Apply `{components.modal-backdrop.opacity}` to the backdrop.

**`loading-state`** — Pair progress feedback with target-specific Korean copy such as `거래 내역을 불러오는 중입니다`. For prolonged loading, provide retry or return navigation.

**`empty-state`** — Explain why no data exists and provide a valid next action. Do not reuse the same message for errors and legitimate empty results.

## Accessibility

- Make buttons and critical controls at least `{layout.minTouchTarget}` in both dimensions.
- Ensure body/background and action-text/button combinations meet WCAG AA contrast.
- Never communicate success, warning, error, or selection through color alone.
- Give every icon button a visible label or accessible name.
- Match focus order to visual reading order and never remove `{border.focus}`.
- Under zoom and large-text settings, prevent amounts, action labels, and errors from clipping or overlapping.
- Do not auto-dismiss financial warnings; provide enough time to understand them.
- Before a sensitive or irreversible action, restate the outcome, counterparty, and amount.

## UX Writing

- Write Korean guidance and outcome messages in the formal `~니다` style.
- Prefer Korean to English in user-facing UI. Explain unavoidable financial terminology in plain Korean.
- Use `시니어` for the protected user and `보호자` for the assisting user in all user-facing copy. Internal domain terms such as `Ward` and `Guardian` must not appear in the interface.
- Describe the action the user performs instead of using language that sounds like the interface is questioning or addressing them.
- Replace generic labels such as `확인`, `예`, and `계속` with active labels that state the outcome.
- Prefer user-led `~하기` labels for actions. Concise navigation labels such as `다음`, `이전`, and `홈으로` are allowed when the destination or direction is unambiguous.
- Put one core message in each sentence. Separate the risk reason from the recovery instruction.

### Korean Particles

Choose particles according to the relationship between the user action and its target.

- Delivery, request, or contact to a specific target: `에게` — for example, `보호자에게 승인 요청하기`.
- Relationship formation or a shared action: `와` — for example, `보호자와 연결하기`.
- Viewing a target's information or status: `의` or omission — for example, `시니어의 거래 내역` or `시니어 거래 내역`.
- Directly changing or selecting a target: `을/를` — for example, `보호자를 선택하기`.

| Avoid | Use |
|---|---|
| 확인 | 송금하기 |
| 예 | 거래 승인하기 |
| 계속 | 계좌 연결하기 |
| 취소할까요? | 송금을 취소합니다 |
| 문제가 발생했습니다 | 계좌 정보를 불러오지 못했습니다. 다시 시도해 주세요. |

## Responsive Behavior

| Name | Width | Key Changes |
|---|---:|---|
| Mobile | `< 640px` | One column, `{layout.mobileGutter}`, persistent bottom navigation |
| Tablet | `640–1023px` | `{layout.tabletGutter}`, up to two summary-card columns |
| Desktop | `1024–1279px` | `{layout.desktopGutter}`, optional two-column guardian monitoring |
| Wide | `≥ 1280px` | Cap content at `{layout.maxContentWidth}` |

On senior mobile screens, place the primary action in a stable lower-screen position without overlapping `{components.bottom-nav.height}`. Guardian desktop screens may increase information density, but must not reduce type sizes, touch targets, or status labels. Convert tables to card lists on small screens and never make critical information available only through horizontal scrolling.

## Do's and Don'ts

### Do

- Use `{colors.primary-500}` consistently for primary actions, trusted connections, and focus.
- Make the amount, counterparty, and processing status the first information users can scan.
- Pair warnings and errors with an icon, Korean status name, and recovery action.
- Emphasize one primary task at a time on senior-facing screens.
- Use language that respects the senior user's choice and independence, including in guardian approval flows.

### Don't

- Do not overuse semantic colors as decoration or large background fills.
- Do not represent a risky transaction through color or a small badge alone.
- Do not place multiple equally prominent primary buttons side by side.
- Do not expose account numbers, contact details, or authentication data without masking.
- Do not hide scrollability or place the only critical action outside the visible viewport.
- Do not use outcome-ambiguous labels such as `확인`, `진행`, or `처리`.

## Iteration Guide

1. Identify whether the current user is a senior or a guardian.
2. Define one primary task and one primary action for the screen.
3. Reference real YAML tokens such as `{colors.primary-500}`; do not repeat inline color, spacing, or radius values.
4. Design default, pressed, focus, disabled, loading, success, warning, and error states together.
5. Verify that amount, counterparty, risk reason, and result follow the intended reading order.
6. Test color perception, keyboard access, zoom, large text, and screen-reader output.
7. Test small senior-facing screens and guardian layouts with production-like Korean copy and long institution or merchant names.
