# Portail B2B — backlog revue flux

Rescan date: 2026-08-15 (post-fixes). Scope: client + admin journeys vs standard B2B process.

---

## Verdict (current)

**One order story:** Catalogue → panier → `/orders/new` → `POST /api/orders` (+ optional `customerOrderReference` = n° BC client).

**One sample story:** Catalogue (samples-only) → editable ship-to in panier → `POST /api/samples` → client list `/dashboard/demandes/echantillons` → admin `/admin/samples`.

**One claim story:** Client réclamation → `POST /api/claims` → admin `/admin/claims`.

**One invoice dispute story:** Facture détail → `POST /api/invoices/{n}/dispute` → admin `/admin/disputes`.

Unfinished modules (cotation, PO upload, `/admin/demandes` inbox) are **redirected / removed from nav**, not shown as live features.

---

## Fixed in this pass

- [x] Invoice contestation wired to API (no fake success / fake file upload)
- [x] Sample ship-to editable in cart (no “fix Profil” dead-end for SAP-locked address)
- [x] Mixed cart: sample-only CTA only when no products
- [x] Admin dashboard sample queue statuses = `new` | `processing`
- [x] `openReqCount` = open claims + open samples (not quotations)
- [x] Client nav: Catalogue; **Échantillons & qualité**; **Rendez-vous** (no “Outils”)
- [x] Demandes hub lists live types; cotation/BC/admin Demandes redirected
- [x] Claims admin UI = allowed transitions only
- [x] No simulated clients / fake customer layout fallback
- [x] Notification: unknown `/client/*` → `/dashboard`

---

## Remaining (optional / low)

- [ ] Certificates / “outils” module if product needs it (orphan `CertificateSearch`)
- [ ] Global admin orders queue (KPI Commandes still lands on clients directory)
- [ ] Attachment upload for disputes/claims when storage API exists
- [ ] Certificates module when a real API exists

---

## Target IA (live)

**Client**  
1. Tableau de bord  
2. Catalogue  
3. Suivi commandes  
4. Factures  
5. Échantillons & qualité (échantillons + réclamation)  
6. Rendez-vous  
7. Profil  

**Admin**  
Dashboard · Clients · Utilisateurs · Échantillons · Réclamations · Contestations · Rendez-vous · Créer un compte · Profil  

**Single order**  
`/catalog` → cart → `/orders/new` → `POST /api/orders`

**Single sample**  
Catalogue sample lines → `POST /api/samples` → lists on client + `/admin/samples`

---

## Route map

| Route | Backend? |
|---|---|
| `/catalog` → `/orders/new` | Yes |
| `/dashboard/commandes` | Yes |
| `/dashboard/factures` (+ contestation) | Yes |
| `/dashboard/demandes/echantillons` | Yes |
| `/dashboard/demandes/reclamation` | Yes |
| `/dashboard/demandes` | Hub (no API) |
| `/dashboard/demandes/cotation` | Redirect → hub |
| `/dashboard/demandes/bon-de-commande` | Redirect → catalog |
| `/admin/samples` · `/admin/claims` · `/admin/disputes` | Yes |
| `/admin/demandes` | Redirect → samples |
