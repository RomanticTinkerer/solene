*Offline library of trustworthy health articles, shown where they're relevant: next to a logged symptom, inside a well-being suggestion, or on a pregnancy week.*

(This note was linked from [[solene - Fork of drip - open source period tracker app]] but didn't exist yet.)

---

# Developed plan (October 2026)

## Purpose

Short, sourced explanations, shown in context:

- an (i) icon on each day-log section (bleeding, temperature, mucus, cervix, mood, pain, desire);
- a link next to symptoms and in [[Daily well-being plan]] notifications;
- one article per week and per marker in [[Pregnancy Tracking (Detailed)]].

It is fully offline: no feed, no remote content, no tracking of what was read.

## Starting content already exists

drip's `i18n/en/symptom-info.js` already has explainers for bleeding, cervix, desire, mood, mucus, note, pain, sex and temperature, plus a general introduction to the symptothermal method.

- It is GPL-licensed, like Solene, so it can be reused directly.
- The sections match the native day log almost one-to-one.
- Porting these is milestone 1, so no new writing is needed to start.

## Format

One Markdown file per article in `app/src/main/assets/articles/`:

```markdown
---
id: pain-cramps
title: Period cramps
tags: [pain, cramps, menstrual]
sources:
  - https://example.org/source-page
license: CC-BY-4.0
reviewed_by: null
reviewed_on: null
---

Body text in plain Markdown: headings, paragraphs, bullet lists, bold.
```

- **Matching:** each feature asks for articles by tag, e.g. `pain` + `cramps`, or `pregnancy-week-14`.
- **Rendering:** a small renderer for the subset above. Add a Markdown library only if articles ever need tables or images.

## Sourcing and licences

- Write our own text and cite sources. Facts aren't copyrightable; someone else's wording is.
- Copy text only from compatible licences:
  - public domain, such as many US federal health pages (check each page, since some host licensed material);
  - CC BY or CC BY-SA.
- **Avoid NC licences** (CC BY-NC, CC BY-NC-SA; many WHO materials use these). F-Droid flags apps bundling non-free assets, and NC conflicts with the GPL's freedom to redistribute commercially.
- Every article lists its sources and licence in its front matter, and the Credits screen lists them.

## "Expert written"

- Articles show their review status. "Not yet medically reviewed" is shown until a qualified reviewer signs off. Their name and date go in the front matter, and the article shows "Reviewed by … on …".
- Recruit reviewers from the open-source community (clinicians, midwives). Re-review every article yearly or when its sources change.

## Milestones

1. Port drip's symptom-info texts and add (i) icons to the day-log sections.
2. Article screen, plus a "Library" list on the You tab.
3. Contextual links from well-being notifications and pregnancy weeks.
4. Reviewer workflow (front matter fields, review badge).

## Out of scope

- Downloading or updating content over the network. Updates ship with app releases.
- Search: add when there are more than about 50 articles.
- Translations beyond what drip already provides. Add them when contributors offer.

## Risks

- **Outdated advice:** the yearly re-review and a visible review date make stale articles obvious.
- **Tone:** articles inform. Anything that sounds like a diagnosis gets rewritten to "talk to a doctor or midwife if…".
