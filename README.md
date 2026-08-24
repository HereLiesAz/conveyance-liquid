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
    same act-state-driven pattern `conveyance-bacterium`'s `bacterium.cell.divide` uses. One
    shared-center element pretending to be two -- for genuine two-body coalescence, see
    `LiquidField` below.
  - `liquid.drop.puddle` -- a drop that wobbles under its own weight at rest, no touch involved,
    and periodically sheds a satellite droplet on its own -- real puddle instability, past a
    critical size gravity overcomes surface tension even without a disturbance. Reuses
    `gravitySquashFor` as the wobble amplitude directly (scaled), so a `puddle` crosses the
    fission threshold (by about 8% at the current constants -- not a wide margin, no test pins
    it) while a `bead` used with the same template just sits still -- correctly, with no
    surface-gated branch needed. The shed itself is triggered off the wobble's own live value
    crossing the threshold on its way down, not a second independent timer.
- **`LiquidField`** (`LiquidField.kt`) -- genuine two-body coalescence: two **independently
  addressed** `RestingDrop`s, each placed by the host's own `firstPlacement`/`secondPlacement`
  modifiers (an `.align`, an `.offset`, a drag gesture), with their real on-screen centers
  measured via `onGloballyPositioned`. Only when they're close enough (relative to their own
  combined radii, not a flat pixel threshold) does it paint a connecting neck between them, on a
  `Canvas` overlay -- a decorative read of two already-placed elements' positions, never
  repositioning either one.

Unlike `conveyance-h2g2`/`conveyance-expressive`, a label isn't drawn inside the drop -- text
baked into a droplet breaks the physical read this whole style depends on. `scale` instead sizes
an optional caption rendered beside it.

## Status

All four phenomena from the original concept -- surface tension shape, viscous drag, coalescence,
fission -- have both a self-contained template and, for coalescence, a genuine two-body version
(`LiquidField`). What's still not here: a two-body reverse of fission (an independently addressed
satellite drop that can itself re-coalesce back into its parent, rather than only drifting away
and fading), and `LiquidField` is coalescence-only -- it doesn't yet call `MAX_ELONGATION`/shear
physics when two drops collide hard rather than drift gently together.

An adversarial audit found and this repo has since fixed four real defects, beyond the
already-known missing click wiring (every template now attaches
`Modifier.tell(owesTell, weight).clickable { engage() }`, matching
`conveyance-demo/.../Gallery.kt`'s own wiring): `liquid.drop.puddle`'s satellite shed ran off a
second, independent timer whose period didn't evenly divide the wobble's own, so after the first
cycle it drifted out of phase and fired while the drop was nowhere near stretched -- it's now
triggered directly off the wobble's own live value crossing the shear threshold; `LiquidField`'s
connecting neck painted `first`'s tint at both ends regardless of `second`'s own hue, contradicting
its own "catching the light the same way each drop's own gloss does" comment -- it now blends each
drop's own tint at its own end; `DropletShape`'s "roughly preserving area" claim was false by up to
63% at the elongation levels the templates actually reach (the doc comment has been corrected, not
the shape itself -- the visual ballooning reads fine for a stretched droplet, it just isn't
area-conserving); and `LiquidHue.of`'s hash fallback could hand `%` a negative dividend for
`hue.hashCode() == Int.MIN_VALUE` and throw -- replaced with `mod`, which can't.

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
