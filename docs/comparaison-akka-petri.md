# Comparaison Akka vs Reseau de Petri - 3 scenarios critiques (modele etendu PSD)

> Livrable L6 du cahier des charges. Mise en correspondance ligne a ligne entre la simulation Akka et les transitions du reseau de Petri sur les 3 scenarios retenus apres extension PSD du 29/04/2026.
>
> **Source de verite du vocabulaire** : `petri/petri-troncon.md` (12 places, 12 transitions effectives) et `documentation/gouvernance/lexique.md` (4 etats par train, 6 messages cote controleurs/portes + 4 messages cote trains).
>
> Etat : finalise apres Phase 7. Les 3 scenarios Akka/Petri sont relies aux marquages M0..M19 ; l'analyseur confirme 20 marquages, 40 arcs, 6 invariants, 0 deadlock et 5 proprietes LTL PASSE.

---

## 1) Methode de comparaison

Pour chaque scenario, on liste l'evenement Akka (envoi/reception de message), on indique la transition Petri correspondante, et on note le marquage Petri resultant. La regle de coherence est : **chaque transition Petri tirable doit avoir un message Akka declencheur identifiable, et reciproquement**, sauf cas explicitement documentes (`Attente`, `PortesOuvertes`, `PortesFermees` qui sont des notifications de protocole).

Les scenarios 1 et 2 prolongent directement le **socle initial 2 trains / 1 troncon** : demande, attente, autorisation, exclusion mutuelle et progression apres liberation. Le scenario 3 ajoute la couche de surete apparue lors de l'upgrade final : portes palieres PSD et refus des ouvertures impossibles.

Notation compacte des marquages : on liste uniquement les places ayant un jeton, separees par virgules.

Marquage initial M0 (5 jetons) :
```
M0 = (Canton_libre, Quai_libre, Portes_fermees, T1_hors, T2_hors)
```

---

## 2) Scenario 1 - Cycle nominal complet (un train, canton + quai + portes)

**Description metier** : un seul train demande l'acces au canton, l'obtient, traverse, arrive a quai, ouvre les portes, ferme les portes, repart. Cycle complet sans concurrence.

**Conditions initiales** : M0.

| Etape | Evenement Akka                                                                 | Transition Petri          | Marquage resultant                                                              |
|------:|--------------------------------------------------------------------------------|---------------------------|---------------------------------------------------------------------------------|
| 1     | Train1 envoie `Demande(Train1, replyTo)` au SectionController                  | `T1_demande`              | `(Canton_libre, Quai_libre, Portes_fermees, T1_attente, T2_hors)`               |
| 2     | SectionController repond `Autorisation` a Train1                               | `T1_entree_canton`        | `(Quai_libre, Portes_fermees, T1_sur_canton, T2_hors)`                          |
| 3     | Train1 envoie `ArriveeQuai(Train1, replyTo)` au QuaiController ; apres `Autorisation`, il envoie `Sortie(Train1)` au SectionController | `T1_arrivee_quai` | `(Canton_libre, Portes_fermees, T1_a_quai, T2_hors)`                            |
| 4     | Train1 envoie `OuverturePortes(Train1)` au GestionnairePortes, qui repond `PortesOuvertes` | `Ouverture_portes_T1`     | `(Canton_libre, Portes_ouvertes, T1_a_quai, T2_hors)`                           |
| 5     | Train1 envoie `FermeturePortes(Train1)` au GestionnairePortes, qui repond `PortesFermees` | `Fermeture_portes_T1`     | `(Canton_libre, Portes_fermees, T1_a_quai, T2_hors)`                            |
| 6     | Train1 envoie `DepartQuai(Train1)` au QuaiController                           | `T1_depart_quai`          | `(Canton_libre, Quai_libre, Portes_fermees, T1_hors, T2_hors)` (= M0)           |

**Verifications cle** :
- A l'etape 4 : `Portes_ouvertes=1` ET `T1_a_quai=1` -> invariant PSD-Open OK.
- A l'etape 6 : `T1_depart_quai` n'est tirable que parce que `Portes_fermees=1` -> invariant PSD-Departure OK.
- Marquage final = M0 : le systeme revient a l'etat initial, le cycle est ferme.

---

## 3) Scenario 2 - Concurrence canton + quai

**Description metier** : Train1 entre dans le canton et avance jusqu'a quai. Pendant ce temps Train2 arrive, demande, et obtient le canton (libere par Train1 quand il atteint le quai). On verifie qu'il y a bien tuilage, qu'aucun marquage n'a 2 trains au meme endroit, et que les portes restent fermees pour Train2 puisqu'il n'est pas encore a quai.

**Conditions initiales** : M0.

| Etape | Evenement Akka                                                                 | Transition Petri          | Marquage resultant                                                                          |
|------:|--------------------------------------------------------------------------------|---------------------------|---------------------------------------------------------------------------------------------|
| 1     | Train1 envoie `Demande(Train1, ...)`                                           | `T1_demande`              | `(Canton_libre, Quai_libre, Portes_fermees, T1_attente, T2_hors)`                           |
| 2     | SectionController repond `Autorisation` a Train1                               | `T1_entree_canton`        | `(Quai_libre, Portes_fermees, T1_sur_canton, T2_hors)`                                      |
| 3     | Train2 envoie `Demande(Train2, ...)` -> SectionController repond `Attente`     | `T2_demande`              | `(Quai_libre, Portes_fermees, T1_sur_canton, T2_attente)`                                   |
| 4     | Train1 demande le quai via `ArriveeQuai`, recoit `Autorisation`, puis signale `Sortie` au SectionController | `T1_arrivee_quai` | `(Canton_libre, Portes_fermees, T1_a_quai, T2_attente)`                                     |
| 5     | SectionController depile la file FIFO et envoie `Autorisation` a Train2        | `T2_entree_canton`        | `(Portes_fermees, T1_a_quai, T2_sur_canton)`                                                |
| 6     | Train1 envoie `OuverturePortes(Train1)` -> `PortesOuvertes`                    | `Ouverture_portes_T1`     | `(Portes_ouvertes, T1_a_quai, T2_sur_canton)`                                               |
| 7     | Train1 envoie `FermeturePortes(Train1)` -> `PortesFermees`                     | `Fermeture_portes_T1`     | `(Portes_fermees, T1_a_quai, T2_sur_canton)`                                                |
| 8     | Train1 envoie `DepartQuai(Train1)` au QuaiController                           | `T1_depart_quai`          | `(Quai_libre, Portes_fermees, T1_hors, T2_sur_canton)`                                      |
| 9     | Train2 demande le quai via `ArriveeQuai`, recoit `Autorisation`, puis signale `Sortie` au SectionController | `T2_arrivee_quai`         | `(Canton_libre, Portes_fermees, T1_hors, T2_a_quai)`                                        |

**Verifications cle** :
- A toutes les etapes : `T1_sur_canton + T2_sur_canton + Canton_libre = 1` (exclusion canton).
- A toutes les etapes : `T1_a_quai + T2_a_quai + Quai_libre = 1` (exclusion quai).
- A l'etape 6 : `Portes_ouvertes=1` -> seul T1 est a quai (T2 sur canton). PSD-Open OK.
- A l'etape 8 : `T1_depart_quai` tirable car `Portes_fermees=1`. PSD-Departure OK.
- Au moment ou Train2 occupe le canton (etapes 5-8), Train1 est a quai et les portes peuvent etre ouvertes : aucune interference, les deux ressources sont independantes.

**Note sur le determinisme** : cote Akka, l'ordre est garanti par les files FIFO des deux controleurs (Section + Quai). Cote Petri, depuis le marquage de l'etape 4, plusieurs transitions sont tirables : `T2_entree_canton`, `Ouverture_portes_T1`. L'analyseur explore les deux branches.

---

## 4) Scenario 3 - Surete PSD (tentative invalide rejetee)

**Description metier** : on cherche a verifier que PSD-Open est effective. Cote Petri, aucune transition `Ouverture_portes_Ti` ne doit etre tirable depuis un marquage ou les deux trains sont hors quai. Cote Akka, ce message est **hors protocole** : le vrai acteur `Train` n'envoie `OuverturePortes` que depuis son etat `a_quai`, ce qui est verifie par `TrainSpec`.

**Conditions initiales** : M0 (aucun train a quai).

### 4.1 Cas Akka : tentative hors protocole

| Etape | Evenement Akka                                                                 | Transition Petri attendue       | Resultat                                                                          |
|------:|--------------------------------------------------------------------------------|---------------------------------|-----------------------------------------------------------------------------------|
| 1     | Un acteur externe tente `OuverturePortes(Train1)` alors que Train1 est `hors`  | (aucune)                        | Message hors protocole : le vrai `Train1` ne peut pas emettre cela avant l'etat `a_quai` |

**Verification cote tests** : `TrainSpec` verifie que le train n'envoie pas `OuverturePortes` tant qu'il n'a pas recu l'autorisation du `QuaiController`; `GestionnairePortesSpec` verifie en defense que les portes deja ouvertes pour un occupant refusent silencieusement les demandes d'un autre train.

### 4.2 Cas Petri : analyse de tirabilite

Depuis M0, les transitions tirables sont **uniquement** `T1_demande` et `T2_demande`. La transition `Ouverture_portes_T1` exige `Portes_fermees=1` (OK) ET `T1_a_quai=1` (KO car `T1_a_quai=0` dans M0). Donc `Ouverture_portes_T1` n'est **pas** tirable. Idem pour T2.

**L'analyseur Scala produit** :
- Pour M0 : enumeration des transitions tirables = `{T1_demande, T2_demande}`. Si l'analyseur signalait `Ouverture_portes_Ti` comme tirable depuis M0, ce serait un bug du modele (cas D du protocole-coordination).

### 4.3 Cas Petri : verification globale de la propriete PSD-Open

Pour chaque marquage M atteignable depuis M0, l'analyseur verifie :
```
M(Portes_ouvertes) = 1  =>  M(T1_a_quai) + M(T2_a_quai) = 1
```

Une violation = bug du modele. L'absence de violation sur les **20 marquages atteignables** est la preuve programmatique de la surete PSD-Open.

**Verifications cle** :
- Cote Akka : la machine d'etats du `Train` interdit l'emission legitime de `OuverturePortes` avant l'etat `a_quai`, puis `GestionnairePortes` refuse les demandes concurrentes d'un autre occupant pendant que les portes sont ouvertes.
- Cote Petri : la garde est structurelle (pre-condition de la transition). Sans `Ti_a_quai` dans le pre, le modele accepterait l'ouverture invalide (test rouge de l'analyseur).
- **Convergence des deux niveaux** : le code Akka et le modele Petri implementent la meme contrainte de surete, par deux moyens differents (machine d'etats + garde defensive vs pre-condition de transition).

---

## 5) Synthese des transitions couvertes par les 3 scenarios

| Transition Petri          | Couverte par scenario |
|---------------------------|-----------------------|
| `T1_demande`              | 1, 2                  |
| `T2_demande`              | 2                     |
| `T1_entree_canton`        | 1, 2                  |
| `T2_entree_canton`        | 2                     |
| `T1_arrivee_quai`         | 1, 2                  |
| `T2_arrivee_quai`         | 2                     |
| `Ouverture_portes_T1`     | 1, 2                  |
| `Ouverture_portes_T2`     | (non couverte explicitement, symetrique) |
| `Fermeture_portes_T1`     | 1, 2                  |
| `Fermeture_portes_T2`     | (non couverte explicitement, symetrique) |
| `T1_depart_quai`          | 1, 2                  |
| `T2_depart_quai`          | (non couverte explicitement, symetrique) |

Les 3 scenarios couvrent **9 transitions sur 12**. Les 3 non couvertes sont strictement symetriques de transitions T1 deja jouees ; aucune nouvelle propriete a verifier. Le scenario 3 verifie en plus la **non-tirabilite** des transitions `Ouverture_portes_Ti` depuis les marquages sans train a quai (preuve par contre-exemple pour la surete PSD).

---

## 6) Resultats de l'analyseur Petri (sortie reelle)

Sortie obtenue via `sbt "runMain m14.petri.Analyseur"` sur la branche `extension`. Le bloc ci-dessous donne la synthese console de base ; la liste exhaustive des 40 arcs etiquetes est reproduite dans `docs/preuves-manuelles.md` tache 7 et dans `rapport/Rapport_CriticalSystemModel.docx.md` annexe A1.

### 6.1 Sortie console brute

```
=== Analyseur Petri - Canton + Quai + Portes palieres (M14) ===

Nombre de marquages atteignables : 20

--- Marquages atteignables ---
  M0  = (Canton_libre, Portes_fermees, Quai_libre, T1_hors, T2_hors)
  M1  = (Canton_libre, Portes_fermees, Quai_libre, T1_attente, T2_hors)
  M2  = (Canton_libre, Portes_fermees, Quai_libre, T1_hors, T2_attente)
  M3  = (Portes_fermees, Quai_libre, T1_sur_canton, T2_hors)
  M4  = (Canton_libre, Portes_fermees, Quai_libre, T1_attente, T2_attente)
  M5  = (Portes_fermees, Quai_libre, T1_hors, T2_sur_canton)
  M6  = (Canton_libre, Portes_fermees, T1_a_quai, T2_hors)
  M7  = (Portes_fermees, Quai_libre, T1_sur_canton, T2_attente)
  M8  = (Portes_fermees, Quai_libre, T1_attente, T2_sur_canton)
  M9  = (Canton_libre, Portes_fermees, T1_hors, T2_a_quai)
  M10 = (Canton_libre, Portes_fermees, T1_a_quai, T2_attente)
  M11 = (Canton_libre, Portes_ouvertes, T1_a_quai, T2_hors)
  M12 = (Canton_libre, Portes_fermees, T1_attente, T2_a_quai)
  M13 = (Canton_libre, Portes_ouvertes, T1_hors, T2_a_quai)
  M14 = (Portes_fermees, T1_a_quai, T2_sur_canton)
  M15 = (Canton_libre, Portes_ouvertes, T1_a_quai, T2_attente)
  M16 = (Portes_fermees, T1_sur_canton, T2_a_quai)
  M17 = (Canton_libre, Portes_ouvertes, T1_attente, T2_a_quai)
  M18 = (Portes_ouvertes, T1_a_quai, T2_sur_canton)
  M19 = (Portes_ouvertes, T1_sur_canton, T2_a_quai)

--- Invariants ---
  Invariant canton  (5.1)         : PASSE
  Invariant quai    (5.2)         : PASSE
  Invariant portes  (5.3)         : PASSE
  Invariants par train            : PASSE
  PSD-Open Safety  (6.1) CRITIQUE : PASSE
  PSD-Departure    (6.2) CRITIQUE : PASSE

Deadlocks : AUCUN (le systeme peut toujours progresser)

Exclusion mutuelle canton : PASSE
Exclusion mutuelle quai   : PASSE

=== Fin de l'analyse ===
```

### 6.2 Lecture des resultats

- **20 marquages atteignables** (M0..M19) au lieu des 15-18 estimes a la main en Phase A. L'ecart est explique par l'**independance partielle** entre l'etat des portes et la file d'attente du quai : un train peut etre `a_quai` avec portes ouvertes pendant que l'autre est dans n'importe lequel de ses 4 etats compatibles. La cible documentaire de `preuves-manuelles.md` tache 1 a ete corrigee a 20.
- **3 invariants de ressource PASSE** sur les 20 marquages : verification automatique de `verifierInvariantCanton`, `verifierInvariantQuai`, `verifierInvariantPortes` (cf `Analyseur.scala`).
- **2 invariants critiques de surete PSD PASSE** :
  - `PSD-Open` : aucun marquage parmi M11, M13, M15, M17, M18, M19 (les seuls avec `Portes_ouvertes`) n'a `T1_a_quai = T2_a_quai = 0`. Verifiable visuellement : chacun de ces 6 marquages contient `Ti_a_quai` pour au moins un i.
  - `PSD-Departure` : pour tout marquage avec `Ti_a_quai`, la transition `Ti_depart_quai` n'est tirable que si `Portes_fermees=1`. Aucune violation detectee.
- **0 deadlock** : depuis chaque marquage atteignable, au moins une transition est tirable.
- **Exclusion mutuelle canton et quai PASSE** : aucun marquage n'a `T1_sur_canton + T2_sur_canton > 1` ou `T1_a_quai + T2_a_quai > 1`.
- **40 arcs etiquetes** : le graphe d'accessibilite complet est disponible en annexe A1 du rapport.
- **5 proprietes LTL PASSE** : 3 Safety (`canton`, `quai`, `PSD-Open`) + 2 Liveness bornees (`T1_attente -> F T1_sur_canton`, `T2_attente -> F T2_sur_canton`) verifiees sur le graphe fini. PSD-Departure est verifiee comme invariant critique de tirabilite.

### 6.3 Correspondance avec les scenarios

Les marquages observes a chaque etape des scenarios 1 et 2 (sections 2 et 3) sont tous presents dans la liste M0..M19 :

| Scenario  | Marquage etape  | Identifiant analyseur |
|-----------|-----------------|------------------------|
| Scenario 1 etape 0 (initial) | `(Canton_libre, Quai_libre, Portes_fermees, T1_hors, T2_hors)` | M0  |
| Scenario 1 etape 1 | `(Canton_libre, Quai_libre, Portes_fermees, T1_attente, T2_hors)` | M1  |
| Scenario 1 etape 2 | `(Quai_libre, Portes_fermees, T1_sur_canton, T2_hors)` | M3  |
| Scenario 1 etape 3 | `(Canton_libre, Portes_fermees, T1_a_quai, T2_hors)` | M6  |
| Scenario 1 etape 4 | `(Canton_libre, Portes_ouvertes, T1_a_quai, T2_hors)` | M11 |
| Scenario 1 etape 5 | `(Canton_libre, Portes_fermees, T1_a_quai, T2_hors)` | M6 (retour) |
| Scenario 1 etape 6 | `(Canton_libre, Quai_libre, Portes_fermees, T1_hors, T2_hors)` | M0 (cycle ferme) |
| Scenario 2 etape 4 | `(Canton_libre, Portes_fermees, T1_a_quai, T2_attente)` | M10 |
| Scenario 2 etape 5 | `(Portes_fermees, T1_a_quai, T2_sur_canton)` | M14 |
| Scenario 2 etape 6 | `(Portes_ouvertes, T1_a_quai, T2_sur_canton)` | M18 |

Tous les marquages produits par les simulations Akka des scenarios 1 et 2 appartiennent a l'espace d'etats calcule par l'analyseur. **Convergence simulation/modele formel confirmee.**

### 6.4 Marquages "interessants" pour la surete PSD

Les 6 marquages avec `Portes_ouvertes` (M11, M13, M15, M17, M18, M19) sont les temoins critiques de la propriete PSD-Open. Pour chacun, on verifie a la main que `T1_a_quai` ou `T2_a_quai` est present :
- M11 : T1_a_quai. OK
- M13 : T2_a_quai. OK
- M15 : T1_a_quai (T2 en attente, hors quai). OK
- M17 : T2_a_quai (T1 en attente, hors quai). OK
- M18 : T1_a_quai (T2 sur canton, hors quai). OK
- M19 : T2_a_quai (T1 sur canton, hors quai). OK

Aucun marquage n'a `Portes_ouvertes` sans train a quai. **Surete PSD-Open programmatiquement confirmee.**

---

## 7) Limites de la comparaison

- Les **files FIFO Akka** (SectionController + QuaiController) ne sont pas modelisees dans le reseau de Petri. Cela introduit une asymetrie : Akka est plus deterministe que le modele Petri sur le choix du train a servir. C'est volontaire pour garder le reseau compact (cf `petri/petri-troncon.md` section 9).
- Les **timings** ne sont pas captures, ni cote Akka (asynchrone mais sans retard explicite ; pas de `Thread.sleep` dans les tests), ni cote Petri (modele atemporel).
- Le **delai d'embarquement** (montee/descente voyageurs entre `OuverturePortes` et `FermeturePortes`) est encode comme un message immediat dans les tests pour garder le determinisme. En production reelle, ce serait un `context.scheduleOnce`.
- La **garde de surete** des portes est verifiee de deux manieres convergentes mais conceptuellement differentes : garde defensive cote Akka, pre-condition de transition cote Petri. Le rapport (L4 section 5.5) discute cette convergence comme triple preuve (structurelle + inductive + programmatique).
- La comparaison reste **qualitative** (correspondance de transitions et marquages) et non quantitative (pas de mesure de performance, latence, debit).

---

## 8) Annexe historique - scenarios du modele initial (7 places, 6 transitions)

L'enumeration originale a 3 scenarios pour le modele "1 troncon, 7 places" (avant extension PSD du 29/04) couvrait nominal / concurrence / sortie-progression sur 6 transitions. Cette version est conservee dans l'historique git (`documentation/suivi/historique.md` entrees du 27/04) et a servi de base pedagogique avant l'extension. Les noms de transitions de cette version (`T1_entree_autorisee`, `T1_sortie_liberation`) sont **caduques** apres renommage (cf `documentation/gouvernance/lexique.md` section 1).
