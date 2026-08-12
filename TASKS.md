# Backend — finish partial features (tick when done)

## Commandes (list + create + status exist — missing pieces)

- [x] Add `GET /api/orders/{orderNumber}` returning full order detail (lines, invoice summary, live status) with client/admin access checks
- [x] Add `PUT /api/admin/orders/{orderNumber}/status` updating `erp_mock.order_status` and notifying the client via `PortalNotificationService`

## Profil client (`GET /api/customers/me` exists — missing update)

- [x] Add `PUT /api/customers/me` to update allowed `erp_mock.customers` fields and log each change in `app.client_change_log`

## Factures (`GET /api/invoices` + disputes exist — missing download)

- [x] Add `GET /api/invoices/{invoiceNumber}` returning one invoice with order link, scoped to the connected client
- [ ] Add OpenPDF or PDFBox dependency and a basic invoice PDF template
- [ ] Add `GET /api/invoices/{invoiceNumber}/file` generating once, storing in `app.documents`, reusing on later calls, with customer isolation

## Rendez-vous (client `POST` + `GET` exist — admin side missing)

- [ ] Add `GET /api/admin/appointments` listing all appointment requests for admin
- [ ] Add `PUT /api/admin/appointments/{id}/confirm` setting status to confirmed and notifying the client
- [ ] Add `PUT /api/admin/appointments/{id}/reject` setting status to cancelled/rejected and notifying the client

## Tableau de bord admin (KPIs partial)

- [ ] Extend `GET /api/admin/dashboard/kpis` with pending-orders count from `erp_mock.order_status` (`confirmed` + `in_preparation`)

## Réclamations (API mostly done — optional detail endpoints)

- [ ] Add `GET /api/claims/{id}` for client to fetch one own reclamation by id
- [ ] Add `GET /api/admin/claims/{id}` for admin to fetch one reclamation by id

## Contestations factures (API mostly done — optional admin detail)

- [ ] Add `GET /api/admin/invoices/disputes/{id}` returning one dispute with invoice context for admin review page
