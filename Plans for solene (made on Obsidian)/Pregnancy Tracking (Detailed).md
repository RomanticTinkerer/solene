**FEATURES:**

**IMPLEMENTATION**: One By One.

Utilise the phone's processing power to render a belly according to weeks passed. Show 

	**FEATURES**:
- 3D Visuals, Organ Development and Milestones
- Granular Tracking
- Kick Counters
- Contraction Timer
- Syncs to a Partner's Phone [[Partner Sharing]]
- Expert written medical articles.

**DETAILED TRACKING**

Use highly specific markers provided by user. Like poor sleep, pain, nausea, etc. Serve up articles from medical professionals.

# Torso and Abdomen Rendering Implementation Plan

## Scope

Build an Android-first 3D visualization that renders only the torso, abdomen, and pregnancy belly progression. The model is a visual representation, not a clinical prediction tool.

Out of scope:

- Full body modeling
- Fetal modeling
- Medical diagnosis or measurement
- Cloud rendering
- Static week-by-week Blender morph targets as the primary approach

In scope:

- A neutral torso mesh
- Procedural abdomen deformation
- Week-based pregnancy belly progression
- Height, weight, and body-frame parameter adjustments
- Local real-time rendering on Android
- Local encrypted persistence of user parameters

## Recommended Stack

- Android: Kotlin
- UI: Jetpack Compose with Material 3 Expressive
- Renderer: Google Filament
- Asset format: glTF / GLB for the base torso mesh
- Storage: encrypted local app storage
- Prototype controls: Compose sliders for week and body parameters

Filament is a good fit because it is designed for high-quality mobile rendering, supports glTF workflows, and gives enough control for custom materials and vertex processing.

## High-Level Architecture

```text
Compose screen
    |
    v
Filament viewport
    |
    v
Base torso mesh
    |
    v
Procedural deformation stage
    |
    v
Updated vertex positions / shader displacement
    |
    v
Rendered torso and abdomen
```

The app should treat the 3D model as a specialized visualization component, not as the center of the entire product architecture.

## Rendering Strategy

Use a single neutral torso mesh with stable topology.

The mesh should include:

- Upper torso
- Rib area
- Abdomen
- Pelvis start / lower abdomen boundary
- No arms
- No legs
- No head
- No clothing in v1

The torso mesh should be low or medium poly. Start around 5k to 20k vertices. Increase only if deformation artifacts are visible.

The mesh must have consistent local coordinate semantics:

```text
x = left/right
y = vertical torso axis
z = front/back
```

Assume positive `z` points forward from the abdomen.

## Deformation Model

Pregnancy progression is represented as a normalized week value.

```text
weekT = clamp((week - 6) / 34, 0, 1)
```

Use a sigmoid curve for visual growth:

```text
growth = 1 / (1 + exp(-8 * (weekT - 0.55)))
```

This avoids a linear look and keeps early pregnancy visually subtle.

## Abdomen Mask

Each vertex receives an abdomen influence value based on its position.

```text
abdomenMask =
  exp(
    -pow(x / abdomenWidth, 2)
    -pow((y - abdomenCenterY) / abdomenHeight, 2)
    -pow((z - abdomenCenterZ) / abdomenDepth, 2)
  )
```

This creates a smooth ellipsoid influence field centered on the abdomen.

Only vertices inside or near the abdomen receive meaningful displacement.

## Vertex Displacement

The basic deformation pushes vertices forward and slightly downward.

```text
forwardPush = growth * maxForwardDisplacement * abdomenMask
lowerSag = growth * growth * sagFactor * abdomenMask

vertex.z += forwardPush
vertex.y -= lowerSag
```

Use `growth * growth` for sag so downward movement becomes more visible later in pregnancy.

For a more natural shape, widen the abdomen as it grows:

```text
sideExpansion = growth * sideExpansionFactor * abdomenMask
vertex.x += sign(vertex.x) * sideExpansion * abs(vertex.x)
```

Keep side expansion smaller than forward push.

## Parameter Inputs

Use user-entered height and weight only as soft visual parameters.

```text
heightM = heightCm / 100
bmi = weightKg / pow(heightM, 2)
heightScale = heightCm / 165
```

Example parameter mapping:

```text
maxForwardDisplacement = baseForwardDisplacement * heightScale * bmiVisualScale
abdomenWidth = baseAbdomenWidth * frameWidthScale
abdomenHeight = baseAbdomenHeight * torsoLengthScale
sagFactor = baseSagFactor * softnessScale
```

Do not imply that height and weight can predict the user's actual pregnancy appearance. They should only tune the visualization.

## Natural Variation

Add very subtle asymmetry after the base deformation works.

```text
variation = noise(vertexPosition * 2.0) * growth * variationStrength
vertex.z += variation * abdomenMask
```

Keep `variationStrength` very low. Excessive noise will make the abdomen look lumpy or medically concerning.

Recommended starting range:

```text
variationStrength = 0.005 to 0.02
```

## CPU First, GPU Later

Implement deformation in two stages.

Stage 1: CPU deformation

- Easier to debug
- Easier to inspect vertex positions
- Good for validating equations
- Can recompute only when week or body parameters change

Stage 2: GPU deformation

- Move deformation into a vertex shader or Filament material customization
- Useful for smooth slider interaction
- Better demonstrates on-device GPU use

Do not optimize before the shape is visually correct.

## Prototype Milestones

### Milestone 1: Static Torso Viewer

Goal: render a neutral torso mesh on Android.

Tasks:

- Create a Compose screen with a Filament viewport
- Load a GLB torso mesh
- Add camera orbit or fixed camera framing
- Add basic lighting
- Confirm it runs on a mid-range Android device

Success criteria:

- Torso renders at stable frame rate
- Camera framing shows abdomen clearly
- No deformation yet

### Milestone 2: Week Slider Deformation

Goal: drive abdomen deformation from a pregnancy-week slider.

Tasks:

- Add week slider from 6 to 40
- Implement CPU-side vertex deformation
- Recompute mesh when slider changes
- Add debug display for current week and growth value

Success criteria:

- Week 6 is visually subtle
- Week 20 shows moderate abdomen growth
- Week 32 and 40 show clear belly progression
- Mesh does not tear, fold, or self-intersect obviously

### Milestone 3: Body Parameter Controls

Goal: tune visual output using body parameters.

Tasks:

- Add height input
- Add weight input
- Add optional body-frame selector: narrow, average, broad
- Add softness slider for debugging
- Map parameters to deformation constants

Success criteria:

- Inputs change proportions without extreme distortion
- Same week can produce visibly different but believable shapes
- Defaults look reasonable with no user tuning

### Milestone 4: GPU Deformation

Goal: move deformation to GPU where practical.

Tasks:

- Port deformation math to shader/material logic
- Pass week and body parameters as uniforms
- Compare GPU output against CPU output
- Keep CPU fallback if needed

Success criteria:

- Slider interaction is smooth
- Visual output matches CPU prototype closely
- Rendering remains stable on mid-range Android hardware

### Milestone 5: Visual Polish

Goal: make the model feel intentional and comfortable.

Tasks:

- Add soft studio lighting
- Use a non-realistic but human-friendly material
- Avoid overly clinical realism
- Add simple camera framing presets
- Add loading and fallback states

Success criteria:

- The visualization looks calm, clear, and non-alarming
- It fits inside the Material 3 app experience
- It does not feel like a separate game engine screen

## Performance Guidelines

- Do not recompute mesh every frame unless the user is actively dragging a slider
- Cache deformation results for the current week and body parameters
- Keep mesh size modest
- Pause rendering when the screen is not visible
- Use fixed camera and lighting in v1
- Benchmark on mid-range Android, not only flagship devices

Suggested performance targets:

```text
Viewport interaction: 60 FPS target
Static display: minimal GPU/CPU work
Mesh deformation update: under 16 ms preferred, under 33 ms acceptable
Memory: keep 3D assets small enough for low-end devices
```

## Local Encrypted Storage

Store visualization parameters locally and encrypted.

Persist:

- Selected pregnancy week or gestational start date
- Height
- Weight
- Body-frame preference
- Visualization settings

Avoid storing:

- Unnecessary logs
- Analytics events with reproductive health data
- Raw debug traces containing personal health values

Recommended Android options:

- Android Keystore for key management
- Jetpack Security for encrypted key-value storage
- SQLCipher if using SQLite

## Validation Plan

Validate the model visually, technically, and ergonomically.

Visual checks:

- Week 6, 12, 20, 28, 32, 36, 40
- Shorter/taller height values
- Lower/higher weight values
- Narrow/average/broad frame presets

Technical checks:

- No mesh tearing
- No severe normal artifacts
- No obvious clipping from the camera
- No crashes during rapid slider updates
- Stable memory use after repeated screen opens

UX checks:

- Week changes feel smooth
- The visualization is clearly framed as illustrative
- Users can disable or ignore the 3D view
- The viewport does not dominate the rest of the tracker app

## Key Risks

### Risk: The abdomen looks like a sphere

Mitigation:

- Use separate forward push, side expansion, and lower sag terms
- Tune abdomen center and vertical influence carefully
- Add subtle asymmetry only after the base shape works

### Risk: Body parameters create unrealistic results

Mitigation:

- Clamp all parameter ranges
- Use presets instead of fully freeform tuning
- Treat height and weight as soft modifiers

### Risk: GPU implementation becomes complex too early

Mitigation:

- Build CPU deformation first
- Port only after the visual model is proven
- Keep equations simple and shader-friendly

### Risk: The feature feels medically authoritative

Mitigation:

- Avoid exact-body claims
- Use language like "visual progression"
- Do not compare the user's body to the render

## Suggested File/Module Layout

```text
app/
  ui/
    pregnancy/
      PregnancyTorsoScreen.kt
      PregnancyTorsoControls.kt
  rendering/
    TorsoRenderer.kt
    TorsoMeshLoader.kt
    AbdomenDeformer.kt
    DeformationParams.kt
    DeformationCurves.kt
  storage/
    EncryptedVisualizationSettingsStore.kt
```

## Initial Implementation Order

1. Add the Filament viewport inside a Compose screen.
2. Load and display a neutral torso GLB.
3. Define normalized torso coordinate assumptions.
4. Implement the sigmoid week-growth function.
5. Implement CPU abdomen deformation.
6. Add week slider and debug controls.
7. Add height, weight, and frame modifiers.
8. Tune visual constants using screenshots from milestone weeks.
9. Benchmark on a mid-range Android device.
10. Move deformation to GPU only after the CPU version looks believable.

## Practical v1 Goal

The v1 should prove this statement:

> A neutral torso mesh can be procedurally deformed on-device into a believable pregnancy abdomen visualization using week, height, weight, and frame parameters.

If that is true in the prototype, the feature is worth integrating into the main app.

---

# App Integration Plan (non-3D features, October 2026)

The torso plan above stays as written. This section covers everything else in the feature list and how pregnancy fits into the existing native app.

## Build order

1. **Pregnancy mode.** Everything else depends on it.
2. **Contraction timer and kick counter.** Small, high value, no new dependencies.
3. **Granular tracking.** Pregnancy symptoms in the existing day log.
4. **Weekly content** from the [[Health Content Library]].
5. **Partner sync** through [[Partner Sharing]].
6. **3D torso** (plan above). The largest item, so it goes last. Milestone 1 must also measure how much Filament adds to the APK size.

## Pregnancy mode

**Starting it:**

- Settings (or [[Developer Options]] while under construction) → "I'm pregnant", with either the first day of the last period or a due date.
- Due date = last period + 280 days. Gestational age = days since the last period, shown as "week 14, day 3".

**While it's on:**

- Period predictions and period reminders pause.
- Home shows the week and the matching content card. The calendar shows week numbers.
- No cycle completes, so Stats stays as it was before the pregnancy.

**Ending it:** "Pregnancy ended", with a date.

- Cycle tracking resumes, and the next logged bleeding starts a new cycle.
- `CycleHistory` must skip the pregnancy interval. A 280-day "cycle" would wreck every average and the prediction.
- Wording is neutral for every outcome. No celebratory copy, no follow-up prompts. A loss must not trigger "congratulations" anywhere.
- Offer to hide pregnancy content afterwards.

**Data:** `PregnancyRecord(lastPeriod: LocalDate, dueDate: LocalDate, ended: LocalDate?)`, stored as a list in DataStore and included in export/import.

## Contraction timer

- One large button: tap at the start of a contraction, tap again at the end.
- **Shows:** each contraction's duration, the interval from one start to the next, and averages over the last hour.
- Every tap is saved immediately. Process death or a phone restart during labour must not lose the session.
- **Optional threshold:** "Tell me when contractions are ≤ X minutes apart, lasting ≥ Y seconds, for Z minutes."
  - The user enters what their midwife or doctor told them. Guidance differs by country and by pregnancy, so the app ships no built-in rule.
- Works offline, screen-on, one-handed.

## Kick counter

- A session: start, tap for each movement, stop. Stores start, end, and count.
- History lists sessions, so the user learns their baby's usual pattern.
- **No pass/fail threshold.** Guidance on counting to a fixed number differs between countries.
- Every screen shows: "If you notice your baby moving less than usual, contact your midwife or maternity unit straight away. Don't wait until tomorrow."

## Granular tracking

The day log gains pregnancy-only chips while pregnancy mode is on. Existing fields are reused where they fit:

| Marker | Where it lives |
|---|---|
| Nausea | Existing `PainType.NAUSEA` |
| Fatigue | Existing `Mood.TIRED` |
| Poor sleep | Sleep from [[Wearable Integration]] if connected, otherwise a new chip |
| Heartburn, swelling, headache | New pregnancy chips; headache already exists as `PainType.HEADACHE` |

Each marker links to its article in the [[Health Content Library]]. "Serve up articles" means a link next to the marker, not a feed.

## Medical articles

- One article per pregnancy week, plus one per marker, from the [[Health Content Library]].
- "Expert written" needs real reviewers. Until an article has a recorded reviewer, it says "Not yet medically reviewed" and lists its sources.

## Partner sync

The snapshot from [[Partner Sharing]] gains: pregnancy week, due date, last kick session summary, and a live contraction summary (only if the tracker shares it).

## Risks

- **Liability:** contraction and kick features are recording tools, not monitors. Every screen points to the user's care provider; nothing tells the user that "everything is fine".
- **Wrong dates:** the due date is editable. An early ultrasound often moves it, so offer "Update due date" prominently.
- **Pregnancy loss:** see the wording rules above. Test the ending flow with that case first.


