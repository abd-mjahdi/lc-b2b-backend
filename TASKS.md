# Liste de tâches complète : Portail B2B Lesieur Cristal (MVP — 1,5 mois, duo de stagiaires)

## Phase 1 : Cadrage, environnement et maquettage (Jours 1-3)

1. Créer le dépôt Git avec une structure séparant clairement backend, frontend, migrations Liquibase et documentation, et rédiger un README expliquant comment lancer le projet en local.

2. Rédiger un document de cadrage listant, écran par écran, ce qui reste dans le périmètre après le nettoyage du diagramme de classes : Commande, Suivi de commande, Facture/PDF, Infos client, Réclamation, Demande de cotation, Demande d'échantillon, Bon de commande, Contact, Rendez-vous — en notant explicitement que Contrat, Cahier des charges (CDC) et Alpha AT sont hors périmètre.

3. Compléter ce document avec les deux entités absentes du diagramme actuel : Demande d'échantillon et Message de contact / Bon de commande, pour que le modèle de données de la Phase 2 parte d'une base complète.

4. Installer et vérifier les versions exactes des outils utilisés par les deux stagiaires (Java 21 + Spring Boot 3.x, Node LTS, PostgreSQL 17, Liquibase) pour éviter les écarts d'environnement.

5. Mettre en place un `docker-compose.yml` de développement lançant PostgreSQL 17 avec volume persistant, pour que les deux stagiaires travaillent contre la même base locale.

6. Créer un squelette Next.js (App Router) et un squelette Spring Boot vides mais qui démarrent, poussés sur le dépôt, pour valider la chaîne outillage avant d'écrire du code métier.

7. Esquisser rapidement (papier ou Figma/Excalidraw) chaque écran client : accueil, connexion, historique des commandes, suivi, infos client, réclamation, devis, échantillon, bon de commande, rendez-vous, contact.

8. Répartir les rôles entre les deux stagiaires (un axe plutôt backend/données, un axe plutôt frontend/UI, chacun capable de dépanner l'autre) et fixer un point quotidien de 15 minutes vu le délai serré.

## Phase 2 : Modélisation des données et base PostgreSQL (Jours 4-8)

9. Finaliser le schéma de classes simplifié : garder Utilisateur, Client, Administrateur, Commande, Facture, Produit, DemandePrix, RendezVous, Reclamation, et ajouter DemandeEchantillon, BonDeCommande et MessageContact.

10. Créer deux schémas dans la même instance PostgreSQL 17 : `app` (données propres au portail) et `erp_mock` (données simulant SAP) — un seul des deux à retirer le jour où l'accès SAP réel arrivera.

11. Mettre en place la structure Liquibase dans le projet Spring Boot (changelog maître + un changeset par table), avec un changelog séparé pour `app` et un pour `erp_mock`.

12. Écrire les changesets `erp_mock` : `customers`, `orders`, `order_status` (statut en cours, date de mise à jour, livraison prévue), `invoices` (numéro, date, statut réglé/à régler, échéance).

13. Écrire les changesets `app` pour les comptes : `users` (nom, prénom, email, login, mot de passe hashé, rôle Client/Administrateur, `customer_number` en clé logique vers `erp_mock.customers`).

14. Écrire les changesets `app` pour les nouvelles demandes : `reclamations`, `quotation_requests`, `sample_requests`, `purchase_order_submissions`, `contact_messages`, `appointment_requests`, chacune avec un statut et un horodatage.

15. Écrire le changeset `documents` (id, customer_number, type, natural_key, chemin de fichier, statut, date) — pour ce MVP il ne stockera que les factures, mais permettra d'ajouter d'autres types de documents plus tard sans grosse migration.

16. Écrire le changeset `client_change_log` (traçabilité des modifications de la fiche client) et un petit catalogue statique `products` (code, nom, catégorie) utilisé par les formulaires de devis et d'échantillon.

17. Écrire un script de données de test peuplant `erp_mock` avec 3 à 5 clients fictifs, une dizaine de commandes à statuts variés, et des factures réglées/à régler.

18. Lancer une base vide, exécuter `liquibase update`, et vérifier dans pgAdmin/DBeaver que les deux schémas et toutes les tables sont créés correctement.

## Phase 3 : Fondations backend Spring Boot (Jours 9-12)

19. Organiser le projet par domaine fonctionnel (commande, client, réclamation, devis, échantillon, bon de commande, rendez-vous, contact) avec un découpage controller/service/repository/DTO par domaine, pour limiter les conflits Git entre les deux stagiaires.

20. Configurer les profils Spring (`dev`/`prod`) avec la connexion PostgreSQL, et vérifier que l'application se connecte bien aux deux schémas.

21. Mettre en place l'authentification : Spring Security + JWT, endpoint `POST /api/auth/login` vérifiant login/mot de passe hashé (BCrypt), token incluant le rôle et le `customer_number`.

22. Ne pas prévoir d'auto-inscription publique pour ce MVP (comptes créés via les données de test), et se concentrer sur connexion, déconnexion et vérification de session.

23. Définir l'interface `ErpOrderConnector` / `ErpCustomerConnector` (interface + implémentation mock lisant `erp_mock`), pensée comme le seul point à réimplémenter plus tard pour brancher le vrai SAP, sans toucher aux services au-dessus.

24. Mettre en place la gestion globale des erreurs, un format de réponse API standard, la validation des entrées, et la configuration CORS pour le frontend Next.js.

25. [x] Ajouter Swagger/OpenAPI pour documenter les endpoints au fil de l'eau et faciliter la coordination sur les contrats d'API entre les deux stagiaires.

26. Décider explicitement de ne pas mettre en place Redis (le mock répond instantanément) ni RabbitMQ (rien dans ce périmètre MVP n'a besoin d'asynchrone) — ce sont des optimisations pour plus tard, pas des prérequis.

## Phase 4 : Backend — commandes, statut, factures (Jours 13-16)

27. [x] Implémenter `GET /api/orders` : historique des commandes du client connecté via `ErpOrderConnector` (jointure commandes + statut de facture), filtré par `customer_number`.

28. [x] Implémenter `GET /api/orders/{id}/status` : statut en direct d'une commande, lu depuis `erp_mock.order_status`, sans cache pour le MVP.

29. Choisir une librairie PDF Java (OpenPDF ou Apache PDFBox) et créer un premier template de facture (en-tête, lignes de commande, montant, statut).

30. Implémenter la logique « générer une fois, puis réutiliser » sur `GET /api/invoices/{id}/file` : vérifier dans `app.documents` si le `natural_key` (numéro de facture) existe déjà, sinon générer le PDF, l'écrire sur disque local, insérer la ligne, puis renvoyer le fichier.

31. Tester manuellement (Postman/Insomnia) ces trois endpoints avec plusieurs clients et statuts de facture différents pour vérifier l'isolation des données.

## Phase 5 : Backend — infos client, réclamations, devis, échantillons, bon de commande, contact, rendez-vous (Jours 17-21)

32. Implémenter `GET`/`PUT /api/client-info` : lecture fusionnée `app.users` + `erp_mock.customers`, écriture qui met à jour `erp_mock.customers` et journalise dans `app.client_change_log`.

33. Implémenter `POST /api/reclamations` (numéro de lot, description, pièce jointe optionnelle), statut initial « nouveau ».

34. Implémenter `POST /api/quotation-requests` (produit, quantité, mode de transport), statut « nouveau ».

35. Implémenter `POST /api/sample-requests` (type de produit, quantité, contact), statut « nouveau ».

36. Implémenter `POST /api/purchase-orders` (upload de fichier obligatoire), statut « nouveau », reproduisant le comportement d'origine (réception équivalente à un email).

37. Implémenter `POST /api/contact-messages` et `POST /api/appointments` (créneau souhaité, objet), chacun avec un statut simple.

38. Créer une interface `NotificationService` avec une implémentation de développement qui journalise chaque nouvelle soumission plutôt que d'envoyer un vrai email, prête à être remplacée par un envoi SMTP réel plus tard.

39. Ajouter des endpoints Administrateur minimalistes (`GET /api/admin/reclamations`, etc.) listant les soumissions par domaine, pour que Lesieur Cristal ait une vue dans le portail en plus des notifications.

40. Sécuriser tous les endpoints : un client ne doit jamais accéder aux données d'un autre `customer_number`, et `/api/admin/*` doit être réservé au rôle Administrateur.

## Phase 6 : Fondations frontend Next.js (Jours 13-16, en parallèle des Phases 4/5)

41. Configurer Tailwind avec la palette Cristal Avril/Lesieur (rouge, brun, crème) et des composants de base (bouton, champ, carte, badge de statut).

42. Mettre en place un client API centralisé avec l'URL du backend en variable d'environnement et un intercepteur attachant le token JWT.

43. Implémenter la page de connexion, le stockage du token, et un middleware Next.js protégeant les routes clientes.

44. Construire le layout partagé : en-tête avec logo, navigation vers chaque section, pied de page avec les coordonnées Lesieur Cristal.

45. Construire les composants réutilisables : tableau paginable, upload de fichier avec validation, badge de statut, toasts, états de chargement.

## Phase 7 : Frontend — écrans du portail (Jours 17-24)

46. Page « Accueil » : message de bienvenue et raccourcis vers les sections principales.

47. Page « Historique des commandes » : tableau sur `GET /api/orders`, statut réglé/à régler, bouton de téléchargement de facture directement dans cette page (pas de page « Mes documents » séparée pour ce MVP, un seul type de document existant).

48. Sur cette même page, afficher le suivi en direct d'une commande sélectionnée via `GET /api/orders/{id}/status`.

49. Page « Infos client » : formulaire pré-rempli, bouton Modifier vers `PUT /api/client-info`.

50. Page « Réclamation » : formulaire branché sur `POST /api/reclamations`.

51. Page « Demande de cotation » : formulaire produit/quantité/transport branché sur `POST /api/quotation-requests`.

52. Page « Demande d'échantillon » : formulaire branché sur `POST /api/sample-requests`.

53. Page « Bon de commande » : formulaire d'upload branché sur `POST /api/purchase-orders`.

54. Page « Rendez-vous » : formulaire de créneau branché sur `POST /api/appointments`, avec affichage du statut une fois soumis.

55. Page « Contact » : formulaire libre avec pièce jointe optionnelle, plus les coordonnées Lesieur Cristal.

56. (Optionnel si le temps le permet) Page « Plaquettes » : liste simple de PDF marketing statiques, gérée par une petite table admin-only.

57. Écrans admin minimalistes listant les soumissions reçues par domaine, en complément des notifications.

## Phase 8 : Intégration, tests et durcissement (Jours 25-28)

58. Parcourir manuellement chaque flux (connexion → commandes → facture → suivi → infos client → réclamation → devis → échantillon → bon de commande → rendez-vous → contact) avec deux ou trois comptes de test.

59. Vérifier les cas d'erreur et états vides sur chaque page et corriger les messages en français.

60. Ajouter un minimum de tests automatisés backend : `ErpOrderConnector` mock, génération de facture, authentification.

61. Repasser une vérification de sécurité : isolation des données entre clients, validation des fichiers uploadés, mots de passe jamais renvoyés par l'API.

62. Vérifier l'affichage sur les résolutions desktop principales et faire une vérification rapide sur mobile.

63. Corriger les bugs remontés et nettoyer le code (logs de debug, messages harmonisés).

## Phase 9 : Bonus hors MVP, uniquement si le temps le permet après la Phase 8 — Certificats

64. Si le calendrier le permet, ajouter le Certificat en le simplifiant : pas de RabbitMQ ni de worker, génération synchrone dans l'endpoint, en réutilisant le mécanisme « générer une fois, puis réutiliser » de `app.documents`.

65. Ajouter la table `certificates` (type, numéro de lot, produit, client, date, champ JSONB `technical_data` pour les données spécifiques par type) et son changeset Liquibase.

66. Ajouter le formulaire frontend « Certificat » et son affichage une fois prêt, en le traitant explicitement comme un bonus non bloquant pour la soutenance.

## Phase 10 : Déploiement, documentation et clôture (Jours 29-32)

67. Conteneuriser backend et frontend (Dockerfile pour chacun) et compléter `docker-compose.yml` pour lancer PostgreSQL + backend + frontend en une commande.

68. Documenter toutes les variables d'environnement nécessaires dans un `.env.example` et dans le README.

69. Préparer un jeu de données de démonstration stable pour que la présentation ne dépende pas d'un état de base imprévisible.

70. Rédiger une note de passation technique : ce qui est mocké (SAP), ce qui a été retiré du périmètre (Contrat, CDC, Alpha AT, Certificats si non traités), et ce qu'il faudra changer à l'arrivée du vrai SAP (uniquement l'implémentation de l'interface ErpConnector).

71. Faire une démonstration complète à blanc entre les deux stagiaires pour repérer les derniers accrocs avant la présentation.

72. Présenter la version au tuteur, recueillir les derniers retours, et n'appliquer que les correctifs mineurs qui ne remettent pas en cause le périmètre figé.

73. Tâche finale : geler le périmètre, repasser la checklist fonctionnelle de la tâche 2 pour confirmer que chaque ligne du MVP est bien implémentée de bout en bout, corriger les tout derniers bugs bloquants, et livrer la version — ce qui marque la fin et la complétude du projet.