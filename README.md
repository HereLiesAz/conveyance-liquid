# conveyance-liquid

A composable-set library for [Conveyance](https://github.com/HereLiesAz/Conveyance): the Liquid style -- shape, motion, and transformation modeled on the real physics of a liquid drop on a solid surface, mercury beading and rolling on stone rather than a metaphor for it. Surface tension holds an element's rest shape; a touch or drag adds momentum and viscous damping; two elements that touch coalesce into one, and a dragged element can shear a satellite droplet off a stretched neck.

## What this is

Per [azphalt's `spec/composable.md`](https://github.com/HereLiesAz/azphalt/blob/main/spec/composable.md),
a `kind: "composable"` `.azp` package is a **pure header**: it names this artifact's Gradle
coordinates (`library.group` / `library.artifact`) and selects a `templateId`, `hue`,
`surface`, `scale`, and `act` from it. It carries no code of its own. This repository *is* the
artifact a composable package's `library` block points at -- the `.azp` package itself is
authored and published separately, wherever its author chooses; this repo does not need to hold
one.

Example composable manifest referencing this library:

```jsonc
{
  "azphalt": "0.1",
  "id": "com.hereliesaz.azphalt.example",
  "name": "Example",
  "version": "1.0.0",
  "kind": "composable",
  "license": "MIT",
  "compat": ">=0.1",
  "composable": {
    "library": { "group": "com.hereliesaz.conveyance", "artifact": "conveyance-liquid", "version": "0.1.0" },
    "elements": [
      { "id": "confirm-record", "templateId": "liquid.drop.drag", "hue": "mercury", "surface": "bead", "scale": "lead", "act": "create", "jobs": ["confirms a destructive action"] }
    ]
  },
  "files": {}
}
```

## What's here

- **`DropletShape`** (`DropletShape.kt`) -- the outline, drawn from a `radius(θ)` function sampled
  around the center and smoothed into a closed curve, not a fixed corner-radius shape. Two real
  physical effects drive it: `gravitySquash` flattens the bottom the way a sessile droplet's
  contact patch flattens under gravity while surface tension keeps the rest round (smaller drops
  stay closer to spherical -- surface tension dominates at small scale, the real capillary-length
  effect); `elongation`/`dragAngleRadians` stretch the drop along its direction of travel and
  compress it perpendicular to that, an approximation of momentum fighting surface tension.
- **`LiquidHue`** (`LiquidHue.kt`) -- five tints (`mercury` default, plus `azure`/`verdant`/
  `ember`/`violet`), each a base/highlight/shadow triad for a glossy, off-center specular
  gradient -- the reflectivity that reads as "liquid" rather than "flat circle."
- **`LiquidSize`** (in `Templates.kt`) -- `surface`'s `bead`/`puddle`/`drop` size classes set both
  the drop's diameter *and* its `gravitySquash`, since size and flatness are physically linked,
  not two independent knobs.
- **`Templates`** (`Templates.kt`) -- two templates: `liquid.drop.rest` (static, gravity-squashed
  only) and `liquid.drop.drag` -- a real drag gesture drives `elongation` live, and on release an
  **underdamped spring** (`dampingRatio = 0.35`) relaxes it back to zero, a genuine
  damped-harmonic-oscillator model producing the overshoot-and-wobble a real disturbed droplet's
  surface tension actually produces, not a canned bounce curve.

Unlike `conveyance-h2g2`/`conveyance-expressive`, a label isn't drawn inside the drop -- text
baked into a droplet breaks the physical read this whole style depends on. `scale` instead sizes
an optional caption rendered beside it.

## Status

A first real slice, not a finished set. Two templates cover shape and drag physics; the other two
phenomena from the original concept -- **coalescence** (two drops merging on contact) and
**fission** (a dragged drop shearing off a satellite droplet) -- aren't implemented yet. A
convincing coalescence effect needs a real gooey-blend render (blur + alpha-threshold across both
drops' combined layer), which is Android-only below API 31 without a fallback; that's real
platform-specific work, not scaffolding, and is the natural next addition here.

## Using it

```kotlin
repositories {
    maven("https://jitpack.io")
}
dependencies {
    implementation("com.github.HereLiesAz:conveyance-liquid:main-SNAPSHOT")
}
```

Resolved via [JitPack](https://jitpack.io) directly from this repository -- `conveyance-core` and
`conveyance-compose` both apply `maven-publish`, which is all JitPack needs, so there is no
separate publish step to configure. Conveyance itself has no tagged release yet, so this artifact
and its upstream dependency on Conveyance both pin to `main-SNAPSHOT` for now; switch both to a
real tag once one exists.
