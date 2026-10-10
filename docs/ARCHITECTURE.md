# Architecture

- `data/Entities.kt` — Room entities + DAOs + `AppDatabase` (vehicles, clients, contracts)
- `data/DatabaseProvider.kt` — singleton Room DB (`locationauto-dz.db`)
- `data/ContractPdfGenerator.kt` — offline A4 PDF via `PdfDocument`, FR/AR labels
- `ui/MainViewModel.kt` — MVVM: Flows for vehicles/clients/contracts; create/close contract
  (closing a contract frees the vehicle; creating one marks it rented)
- `ui/Screens.kt` — 4 tabs: Contracts / Vehicles / Clients / Returns (Compose + Material 3)
- `MainActivity.kt` — composition root, manual ViewModel factory (no Hilt → fewer deps, offline-safe)

Contract math (`days()` = inclusive days min 1; `totalDzd()` = days×rate + extras) is pure Kotlin,
covered by `ContractMathTest`.
