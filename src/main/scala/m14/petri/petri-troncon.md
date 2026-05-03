# Reseau de Petri - Canton + Quai + Portes palieres (M14)

> Livrable L3 du cahier des charges. Modele formel d'un sous-systeme critique du metro M14 automatique : controle d'acces concurrent de 2 trains a un canton de signalisation, suivi d'un arret en station avec gestion des portes palieres (PSD - Platform Screen Doors).
>
> Ce fichier est la **source de verite du vocabulaire** : les noms des places et transitions doivent etre repris a l'identique dans le code Scala (acteurs Akka et analyseur Petri) et dans les tests. Toute divergence de nommage est un bug de coherence.

---

## 1) Vue d'ensemble

Le reseau modelise deux mecanismes critiques imbriques d'un metro automatique sans conducteur :

1. **Block signaling** : un canton de signalisation que les 2 trains se disputent en exclusion mutuelle.
2. **PSD - Portes palieres** : a l'arrivee en station (sur le quai), des portes palieres synchronisees protegent les voyageurs des chutes sur les voies. **Les portes ne s'ouvrent que si un train est a l'arret a quai**, et **un train ne peut quitter le quai que portes fermees**.

Cette extension par rapport au modele initial (1 troncon seul) ajoute deux **proprietes de surete CRITIQUES** au sens reglementaire : une violation peut entrainer une chute mortelle (porte palieres ouverte sans train) ou un voyageur ecrase (depart avec portes ouvertes). C'est exactement ce que demande le cahier des charges sur les "systemes critiques".

### Lecture historique du modele

Pour le rendu, il faut lire ce fichier comme le **modele final**, obtenu apres un premier modele plus petit :

| Etape | Reseau Petri | Role |
|---|---|---|
| **Modele initial** | 7 places, 6 transitions, 8 marquages | 2 trains se partagent un seul troncon. On prouve l'exclusion mutuelle de base. |
| **Modele final PSD** | 12 places, 12 transitions, 20 marquages | Le troncon devient le `Canton_libre`, puis on ajoute `Quai_libre`, `Portes_fermees`, `Portes_ouvertes` et les transitions de quai/portes. |

Le reseau ci-dessous est donc l'upgrade du socle initial, pas un modele different sans lien avec lui.

**Scope strict** :
- 2 trains : T1, T2
- 1 canton de signalisation (entre amont et station)
- 1 quai en station
- 1 paire de portes palieres (modelisee comme un etat global du quai)
- 1 controleur de canton + 1 controleur de quai + 1 gestionnaire de portes
- Aucune notion de panne, de famine prolongee, ni de priorite

Le reseau est analysable a la main et par l'analyseur Scala : **20 marquages atteignables** (M0..M19), **40 arcs etiquetes**, **6 invariants PASSE**, **0 deadlock**.

---

## 2) Places (12)

### Ressources globales (4)

| Place             | Signification                                           | Marquage initial |
|-------------------|---------------------------------------------------------|-----------------:|
| `Canton_libre`    | Le canton de signalisation est libre                    | 1                |
| `Quai_libre`      | Le quai en station est libre                            | 1                |
| `Portes_fermees`  | Les portes palieres sont fermees                        | 1                |
| `Portes_ouvertes` | Les portes palieres sont ouvertes                       | 0                |

### Etats du Train 1 (4)

| Place              | Signification                                                          | Marquage initial |
|--------------------|------------------------------------------------------------------------|-----------------:|
| `T1_hors`          | Train 1 hors zone (en amont du canton ou apres avoir quitte le quai)   | 1                |
| `T1_attente`       | Train 1 a demande l'acces au canton et attend                          | 0                |
| `T1_sur_canton`    | Train 1 occupe le canton de signalisation                              | 0                |
| `T1_a_quai`        | Train 1 est a l'arret en station, sur le quai                          | 0                |

### Etats du Train 2 (4)

Symetrique du Train 1 : `T2_hors=1`, `T2_attente=0`, `T2_sur_canton=0`, `T2_a_quai=0`.

### Marquage initial M0 (5 jetons en circulation)

```
M0 = (Canton_libre=1, Quai_libre=1, Portes_fermees=1, Portes_ouvertes=0,
      T1_hors=1, T1_attente=0, T1_sur_canton=0, T1_a_quai=0,
      T2_hors=1, T2_attente=0, T2_sur_canton=0, T2_a_quai=0)
```

---

## 3) Transitions (12)

Notation : `pre -> post` consomme 1 jeton dans chaque place de `pre`, produit 1 jeton dans chaque place de `post`.

### Cycle de mouvement Train 1 (4 transitions)

| Transition                | Pre                              | Post                              | Sens metier                                          |
|---------------------------|----------------------------------|-----------------------------------|------------------------------------------------------|
| `T1_demande`              | `T1_hors`                        | `T1_attente`                      | Train 1 demande l'acces au canton                    |
| `T1_entree_canton`        | `T1_attente`, `Canton_libre`     | `T1_sur_canton`                   | Train 1 entre dans le canton (autorisation accordee) |
| `T1_arrivee_quai`         | `T1_sur_canton`, `Quai_libre`    | `T1_a_quai`, `Canton_libre`       | Train 1 sort du canton et arrive au quai             |
| `T1_depart_quai`          | `T1_a_quai`, `Portes_fermees`    | `T1_hors`, `Quai_libre`, `Portes_fermees` | Train 1 quitte le quai (portes fermees REQUIS)  |

Note sur `T1_depart_quai` : la place `Portes_fermees` est **lue mais reproduite** (read-arc emule via consommation+production). La transition n'est tirable que si `Portes_fermees=1`.

### Cycle Train 2 (4 transitions)

`T2_demande`, `T2_entree_canton`, `T2_arrivee_quai`, `T2_depart_quai` : symetriques de T1.

### Gestion des portes palieres (4 transitions)

| Transition                | Pre                                              | Post                                              | Sens metier                                                 |
|---------------------------|--------------------------------------------------|---------------------------------------------------|-------------------------------------------------------------|
| `Ouverture_portes_T1`     | `Portes_fermees`, `T1_a_quai`                    | `Portes_ouvertes`, `T1_a_quai`                    | Portes ouvrent car T1 est arrive a quai (CRITIQUE PSD)      |
| `Ouverture_portes_T2`     | `Portes_fermees`, `T2_a_quai`                    | `Portes_ouvertes`, `T2_a_quai`                    | idem T2                                                     |
| `Fermeture_portes_T1`     | `Portes_ouvertes`, `T1_a_quai`                   | `Portes_fermees`, `T1_a_quai`                     | Portes ferment alors que T1 toujours a quai                 |
| `Fermeture_portes_T2`     | `Portes_ouvertes`, `T2_a_quai`                   | `Portes_fermees`, `T2_a_quai`                     | idem T2 (la place `Ti_a_quai` est lue+reproduite)           |

**Pourquoi dedoubler en T1/T2** : en Petri ordinaire, on n'a pas de "garde OU" sur les places de pre-condition. Pour exprimer "ouverture autorisee si UN train (T1 OU T2) est a quai", on instancie 2 transitions, chacune liee a un train. C'est equivalent semantiquement et reste analysable.

### Total transitions

- 4 mouvement T1 (`T1_demande`, `T1_entree_canton`, `T1_arrivee_quai`, `T1_depart_quai`)
- 4 mouvement T2 (symetriques)
- 2 ouvertures (`Ouverture_portes_T1`, `Ouverture_portes_T2`)
- 2 fermetures (`Fermeture_portes_T1`, `Fermeture_portes_T2`)

= **12 transitions effectives**.

---

## 4) Schema ASCII

```
                              Ressources globales
                +---------+   +---------+   +-----------+   +-----------+
                | Canton_ |   |  Quai_  |   |  Portes_  |   |  Portes_  |
                |  libre  |   |  libre  |   |  fermees  |   |  ouvertes |
                +---------+   +---------+   +-----------+   +-----------+

  Train 1                                                  Train 2
+----------+                                            +----------+
| T1_hors  |  <----+                            +---->  | T2_hors  |
+----------+       |                            |       +----------+
     |             | T1_depart_quai             | T2_depart_quai
T1_demande         | (consomme T1_a_quai +      |
     v             |  Portes_fermees [read])    |       T2_demande
+--------------+   |                            |     +--------------+
| T1_attente   |   |                            |     | T2_attente   |
+--------------+   |                            |     +--------------+
     |             |                            |          |
T1_entree_canton   |                            |     T2_entree_canton
(consomme Canton_  |                            |
 libre)            |                            |
     v             |                            |          v
+----------------+ |                            |   +----------------+
| T1_sur_canton  | |                            |   | T2_sur_canton  |
+----------------+ |                            |   +----------------+
     |             |                            |          |
T1_arrivee_quai    |                            |     T2_arrivee_quai
(consomme Quai_    |                            |
 libre, libere     |                            |
 Canton_libre)     |                            |
     v             |                            |          v
+----------------+ |                            |   +----------------+
| T1_a_quai      |-+                            +-->| T2_a_quai      |
+----------------+                                  +----------------+
     |  ^                                                |  ^
     |  | (ouverture/fermeture portes synchro avec      |  |
     |  |  presence d'un train a quai)                  |  |
     v  |                                                v  |
   +-----------+                                       +-----------+
   |  cycle    |                                       |  cycle    |
   |  portes   |                                       |  portes   |
   +-----------+                                       +-----------+
```

Lecture : un train est toujours dans **exactement une** des 4 places de sa colonne (`Ti_hors`, `Ti_attente`, `Ti_sur_canton`, `Ti_a_quai`). Les 4 ressources globales (canton, quai, portes ouvertes, portes fermees) sont partagees.

---

## 5) Invariants de ressource (P-invariants)

Trois invariants structurels (preuves par induction).

### 5.1 Invariant canton

```
M(T1_sur_canton) + M(T2_sur_canton) + M(Canton_libre) = 1
```

**Preuve sur M0** : `0 + 0 + 1 = 1`. OK.

**Preservation par transition** :

| Transition          | Effet sur la somme                                                        |
|---------------------|---------------------------------------------------------------------------|
| `T1_demande`        | aucun jeton sur les 3 places concernees : somme inchangee                 |
| `T2_demande`        | idem                                                                      |
| `T1_entree_canton`  | -1 sur `Canton_libre`, +1 sur `T1_sur_canton` : somme inchangee           |
| `T2_entree_canton`  | -1 sur `Canton_libre`, +1 sur `T2_sur_canton` : somme inchangee           |
| `T1_arrivee_quai`   | -1 sur `T1_sur_canton`, +1 sur `Canton_libre` : somme inchangee           |
| `T2_arrivee_quai`   | -1 sur `T2_sur_canton`, +1 sur `Canton_libre` : somme inchangee           |
| `T1_depart_quai`    | aucun jeton sur les 3 places concernees                                   |
| `T2_depart_quai`    | idem                                                                      |
| Transitions portes  | aucun jeton sur les 3 places concernees                                   |

L'invariant est preserve. **Consequence** : exclusion mutuelle sur le canton.

### 5.2 Invariant quai

```
M(T1_a_quai) + M(T2_a_quai) + M(Quai_libre) = 1
```

**Preuve sur M0** : `0 + 0 + 1 = 1`. OK.

**Preservation** :
- `T1_arrivee_quai` : -1 sur `Quai_libre`, +1 sur `T1_a_quai` : somme inchangee.
- `T2_arrivee_quai` : symetrique.
- `T1_depart_quai` : -1 sur `T1_a_quai`, +1 sur `Quai_libre` : somme inchangee.
- `T2_depart_quai` : symetrique.
- Toutes les autres transitions ne touchent pas ces 3 places : somme inchangee.

L'invariant est preserve. **Consequence** : exclusion mutuelle sur le quai.

### 5.3 Invariant portes (etat des portes)

```
M(Portes_fermees) + M(Portes_ouvertes) = 1
```

**Preuve sur M0** : `1 + 0 = 1`. OK.

**Preservation** :
- `Ouverture_portes_T1` / `Ouverture_portes_T2` : -1 sur `Portes_fermees`, +1 sur `Portes_ouvertes` : somme inchangee.
- `Fermeture_portes_T1` / `Fermeture_portes_T2` : -1 sur `Portes_ouvertes`, +1 sur `Portes_fermees` : somme inchangee.
- `T1_depart_quai` / `T2_depart_quai` : la place `Portes_fermees` est lue (consommee+reproduite), `Portes_ouvertes` n'est pas touchee : somme inchangee.
- Toutes les autres transitions ne touchent pas ces 2 places.

L'invariant est preserve. **Consequence** : les portes sont toujours dans exactement un etat (ouvertes ou fermees), jamais dans un etat indetermine.

---

## 6) Invariants critiques de surete PSD

Ces invariants ne sont pas des P-invariants au sens strict (ils ne sont pas des combinaisons lineaires constantes), mais des **proprietes de surete** que l'analyseur doit verifier sur tout marquage atteignable.

### 6.1 Surete d'ouverture (PSD-Open Safety)

```
G ( Portes_ouvertes = 1  =>  T1_a_quai + T2_a_quai = 1 )
```

**Lecture** : a tout instant, si les portes palieres sont ouvertes, alors un train (et un seul, par exclusion mutuelle quai) est a l'arret a quai.

**Justification structurelle** : la place `Portes_ouvertes` ne peut etre marquee que par les transitions `Ouverture_portes_T1` ou `Ouverture_portes_T2`. Ces deux transitions ont `Ti_a_quai` dans leur pre, donc elles ne sont tirables que si un train est a quai. De plus, ces transitions ne consomment pas `Ti_a_quai` (read-arc), donc le train reste a quai. Tant que le train n'a pas applique `Fermeture_portes_Ti` (qui remet `Portes_fermees=1` et donc `Portes_ouvertes=0`), il ne peut pas tirer `Ti_depart_quai` (qui exige `Portes_fermees=1`). Donc tant que `Portes_ouvertes=1`, le train reste a quai.

**Verification programmatique** : l'analyseur Scala enumere les marquages atteignables et verifie pour chaque marquage M : `M(Portes_ouvertes) = 1 => M(T1_a_quai) + M(T2_a_quai) = 1`.

**Consequence** : les voyageurs ne peuvent jamais tomber sur les voies via les portes palieres. **Surete CRITIQUE M14**.

### 6.2 Surete au demarrage (PSD-Departure Safety)

```
Pour tout marquage M et tout train Ti, si la transition Ti_depart_quai est tirable depuis M,
alors M(Portes_fermees) = 1.
```

**Justification structurelle** : c'est inscrit dans le pre de `Ti_depart_quai` : `{Ti_a_quai, Portes_fermees}`. Si `Portes_fermees=0`, la transition n'est pas tirable. Donc un train ne peut pas demarrer alors que les portes sont ouvertes.

**Consequence** : aucun voyageur ne peut etre coince ou ecrase par un train qui demarre alors qu'il monte/descend. **Surete CRITIQUE M14**.

---

## 7) Espace d'etats obtenu

Depuis M0, l'analyseur Scala et le carnet de preuves enumerent **20 marquages atteignables** (M0..M19). La liste exhaustive est reproduite dans `docs/preuves-manuelles.md` tache 1 et la sortie brute dans `docs/comparaison-akka-petri.md` section 6.

**Lecture par projection** :
- Le canton seul (avec 2 trains) : 8 etats (comme dans le modele initial).
- Pour chaque etat de canton ou un train est `Ti_a_quai`, on multiplie par les 2 etats des portes (fermees/ouvertes).
- Beaucoup de combinaisons restent inaccessibles (par exemple : deux trains a quai, ou portes ouvertes sans train a quai).

La cible initiale "15 a 18" etait sous-estimee ; le chiffre reel **20** reste assez petit pour etre enumere dans le rapport et defendu a l'oral.

---

## 8) Correspondance avec les messages Akka

10 types de messages distincts au total : 6 vers les controleurs (`Demande`, `Sortie`, `ArriveeQuai`, `DepartQuai`, `OuverturePortes`, `FermeturePortes`) + 4 vers les trains (`Autorisation`, `Attente`, `PortesOuvertes`, `PortesFermees`). Cette table est la reference pour `documentation/livrables/comparaison.md` (livrable L6).

**Note de mapping** : la transition Petri `Ti_arrivee_quai` est atomique (consomme `Ti_sur_canton + Quai_libre`, produit `Ti_a_quai + Canton_libre`). Cote Akka elle se traduit par 2 messages emis par le train depuis l'etat `comportementSurCanton` apres reception de `Autorisation` du QuaiController : (1) `Sortie(Ti)` au `SectionController` pour liberer le canton, (2) la transition d'etat interne vers `comportementAQuai` apres l'acquisition du quai. La granularite Akka (deux envois sequentiels) reste compatible avec la granularite Petri (transition unique) car les invariants ne sont evalues qu'aux marquages stables, pas pendant le tir.

La demande au QuaiController (`ArriveeQuai`) est emise **avant** le tir Petri ; elle correspond a la phase de protocole et n'a pas de transition Petri associee si le quai est libre (la transition `Ti_arrivee_quai` est tiree directement). Si le quai est occupe, le QuaiController repond `Attente` et le train reste sur le canton (pas de transition Petri non plus, simple mise en file).

| Transition Petri          | Message Akka declencheur               | Acteur emetteur -> recepteur          |
|---------------------------|----------------------------------------|---------------------------------------|
| `T1_demande`              | `Demande(Train1, replyTo)`             | Train1 -> SectionController           |
| `T2_demande`              | `Demande(Train2, replyTo)`             | Train2 -> SectionController           |
| `T1_entree_canton`        | `Autorisation` (reponse a Demande T1)  | SectionController -> Train1           |
| `T2_entree_canton`        | `Autorisation` (reponse a Demande T2)  | SectionController -> Train2           |
| `T1_arrivee_quai`         | `ArriveeQuai(Train1, replyTo)` + `Autorisation` retour + `Sortie(Train1)` | Train1 <-> QuaiController, Train1 -> SectionController |
| `T2_arrivee_quai`         | `ArriveeQuai(Train2, replyTo)` + `Autorisation` retour + `Sortie(Train2)` | Train2 <-> QuaiController, Train2 -> SectionController |
| `Ouverture_portes_T1`     | `OuverturePortes(Train1)`              | Train1 -> GestionnairePortes          |
| `Ouverture_portes_T2`     | `OuverturePortes(Train2)`              | Train2 -> GestionnairePortes          |
| `Fermeture_portes_T1`     | `FermeturePortes(Train1)`              | Train1 -> GestionnairePortes          |
| `Fermeture_portes_T2`     | `FermeturePortes(Train2)`              | Train2 -> GestionnairePortes          |
| `T1_depart_quai`          | `DepartQuai(Train1)`                   | Train1 -> QuaiController              |
| `T2_depart_quai`          | `DepartQuai(Train2)`                   | Train2 -> QuaiController              |

Le message `Attente` (refus temporaire envoye par les controleurs) **n'a pas de transition Petri associee** (notification de protocole), conformement au modele initial. Il s'applique au SectionController et au QuaiController.

---

## 9) Limites assumees

- **Pas de fairness explicite** : si T1 demande en boucle juste apres avoir libere, T2 peut attendre indefiniment dans le modele Petri pur. La preuve de Liveness suppose une hypothese d'arbitrage non pathologique des controleurs (FIFO en pratique cote Akka).
- **Pas de notion de temps** : le modele est purement combinatoire, aucune duree n'est attachee aux transitions (en particulier, pas de duree minimale d'arret a quai).
- **Pas de pannes** : aucune transition ne modelise la perte de message, le crash d'acteur, ou le timeout.
- **Modele PSD simplifie** : en realite, la M14 a aussi des **portes du train** (pas seulement palieres), avec une synchronisation electronique des deux. Ici on modelise un seul niveau de portes pour rester analysable.
- **Quai unique** : on modelise un seul quai au lieu de 2 (un par sens). Cela impose que les 2 trains se "partagent" un quai, ce qui est une abstraction. En realite, M14 a 2 quais opposes par station.

---

## 10) Verification effectuee avec l'analyseur

L'analyseur Scala confirme programmatiquement :
- [x] Tous les marquages atteignables verifient l'invariant canton (5.1).
- [x] Tous les marquages atteignables verifient l'invariant quai (5.2).
- [x] Tous les marquages atteignables verifient l'invariant portes (5.3).
- [x] **Tous les marquages avec `Portes_ouvertes=1` ont `T1_a_quai + T2_a_quai = 1` (PSD safety, 6.1).**
- [x] Tous les marquages atteignables verifient les invariants par train (`Ti_hors + Ti_attente + Ti_sur_canton + Ti_a_quai = 1`).
- [x] Aucun marquage atteignable n'est un deadlock.
- [x] Le graphe d'accessibilite contient exactement **20 marquages** et **40 arcs etiquetes**.
- [x] Les proprietes LTL bornees (3 Safety + 2 Liveness) passent sur le graphe fini.
