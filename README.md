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
- **`LiquidPairShape`** (`LiquidPairShape.kt`) -- coalescence's outline: two circular drops whose
  centers pull together as `proximity` goes 0→1, bridged by a growing rectangular neck once
  they're close -- the same "two lobes plus a connecting belt" geometry
  `conveyance-bacterium`'s `MitosisShape` uses, run in the opposite narrative direction (surface
  tension pulling together, instead of a cleavage furrow pinching apart). Real geometry, not a
  blur-and-threshold gooey-blend render -- that would need `RenderEffect`, Android-only below API
  31; this reads as convincingly "merging" without it.
- **`Templates`** (`Templates.kt`) -- four templates:
  - `liquid.drop.rest` -- static, gravity-squashed only.
  - `liquid.drop.drag` -- a real drag gesture drives `elongation` live, and on release an
    **underdamped spring** (`dampingRatio = 0.35`) relaxes it back to zero, a genuine
    damped-harmonic-oscillator model producing the overshoot-and-wobble a real disturbed
    droplet's surface tension actually produces, not a canned bounce curve. Past 85% of max
    elongation, the stretched neck **shears**: a small satellite droplet detaches at the trailing
    tip and drifts away, shrinking and fading, over 500ms -- real fission, the way a dragged
    mercury bead actually sheds a smaller bead when pulled too fast for surface tension to hold
    it together.
  - `liquid.drop.pair` -- `LiquidPairShape`'s `proximity` tracks `ActScope.yielding`'s live
    progress while the act is `ActState.Yielding`, reaching 1 (fully merged) at `Settled` -- the
    same act-state-driven pattern `conveyance-bacterium`'s `bacterium.cell.divide` uses.

Unlike `conveyance-h2g2`/`conveyance-expressive`, a label isn't drawn inside the drop -- text
baked into a droplet breaks the physical read this whole style depends on. `scale` instead sizes
an optional caption rendered beside it.

## Status

All four phenomena from the original concept -- surface tension shape, viscous drag, coalescence,
fission -- now have a working template. What's still not here: `liquid.drop.pair` merges two
drops that were always declared together as one element, not two independently addressed drops
that happen to end up near each other on screen (the same self-contained-composable scope
`conveyance-bacterium` settled for its own eating template); and there's no reverse of
`liquid.drop.pair` -- a merged puddle splitting back into two under its own weight, the way
`puddle`-sized drops actually can.

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
