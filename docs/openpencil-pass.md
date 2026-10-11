# OpenPencil pass — locationauto-dz

## Design (Shared/OpenPencil/locationauto-dz/)
- `design.html` (contract card + fleet summary + add action, DZ tokens) → `design.fig`
- `lint --preset accessibility`: 0 errors, 0 warnings

## Code review (no rewrite needed)
Rail + bar nav icons already labeled, dark-mode toggle described, TopAppBar present.
No code change.

## APKs
`android-build.yml` adds `assembleRelease` (unsigned) + release artifact + versioned
`v*-b*` GitHub Release with notes + both APKs.
