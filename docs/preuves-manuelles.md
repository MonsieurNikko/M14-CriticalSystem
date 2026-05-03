# Carnet de preuves manuelles - CriticalSystemModel (modele etendu PSD)

> **A qui s'adresse ce document** : aux contributeurs de l'equipe qui ne touchent pas au code Scala mais qui prennent en charge la verification formelle a la main, conformement au cahier des charges (interdiction d'utiliser un outil logiciel de Petri).
>
> **Pourquoi ce travail est central** : le sujet exige une preuve manuelle des proprietes, l'analyseur Scala (livrable L5) ne fait que **confirmer programmatiquement** ce qui aura ete prouve ici. Sans ce carnet rempli, le rapport (L4) n'a pas de fondations.
>
> **Mise a jour majeure 29/04** : carnet etendu pour le modele Canton + Quai + Portes palieres (PSD). 12 places, 12 transitions, **20 marquages atteignables** (M0..M19) confirmes par l'analyseur Scala le 30/04/2026. Le carnet initial a 8 marquages est archive ci-dessous a titre indicatif.
>
> **Mise a jour 30/04** : tableaux remplis avec la sortie reelle de l'analyseur (40 arcs etiquetes, 6 invariants PASSE, 0 deadlock).

---

## 0) Ce qui est deja fait (a relire d'abord)

Avant de remplir ce carnet, lire dans cet ordre :

1. **`petri/petri-troncon.md`** sections 1 a 8 : definition complete du nouveau reseau (Canton + Quai + PSD), invariants prouves par induction.
2. **`documentation/gouvernance/lexique.md`** : 4 etats par train, 4 ressources globales, 10 messages Akka (6 vers controleurs/portes + 4 vers trains).
3. **`documentation/gouvernance/protocole-coordination.md`** Q11-Q15 : decisions liees a l'extension PSD.

Tout ce qui suit dans ce carnet **prolonge** ces documents avec les preuves manuelles complementaires.

---

## Tache 1 - Enumeration exhaustive de l'espace d'etats (BFS a la main)

### Objectif

Lister **tous** les marquages atteignables depuis M0 en explorant en largeur, et les indexer M0, M1, M2, ... Le nombre reel du modele final est **20 marquages**, confirme par l'analyseur Scala.

### Methode

A chaque marquage, on liste les transitions tirables (celles dont toutes les places de pre ont un jeton), on calcule les marquages successeurs, on ajoute ceux qui sont nouveaux a la file d'exploration.

**Notation compacte** : on liste seulement les places marquees a 1 (les autres sont a 0). Les ressources globales sont prefixees pour la lisibilite.

### Tableau complet des 20 marquages atteignables (verifie par l'analyseur 30/04)

| Index | Marquage (places a 1)                                                       | Atteint via                                | Transitions tirables                                  | Successeurs        |
|------:|-----------------------------------------------------------------------------|--------------------------------------------|-------------------------------------------------------|--------------------|
| **M0**  | `Canton_libre, Quai_libre, Portes_fermees, T1_hors, T2_hors`              | (initial)                                  | `T1_demande`, `T2_demande`                            | M1, M2             |
| **M1**  | `Canton_libre, Quai_libre, Portes_fermees, T1_attente, T2_hors`           | M0 + `T1_demande`                          | `T1_entree_canton`, `T2_demande`                      | M3, M4             |
| **M2**  | `Canton_libre, Quai_libre, Portes_fermees, T1_hors, T2_attente`           | M0 + `T2_demande`                          | `T1_demande`, `T2_entree_canton`                      | M4, M5             |
| **M3**  | `Quai_libre, Portes_fermees, T1_sur_canton, T2_hors`                      | M1 + `T1_entree_canton`                    | `T1_arrivee_quai`, `T2_demande`                       | M6, M7             |
| **M4**  | `Canton_libre, Quai_libre, Portes_fermees, T1_attente, T2_attente`        | M1 + `T2_demande` (ou M2 + `T1_demande`)   | `T1_entree_canton`, `T2_entree_canton`                | M7, M8             |
| **M5**  | `Quai_libre, Portes_fermees, T1_hors, T2_sur_canton`                      | M2 + `T2_entree_canton`                    | `T1_demande`, `T2_arrivee_quai`                       | M8, M9             |
| **M6**  | `Canton_libre, Portes_fermees, T1_a_quai, T2_hors`                        | M3 + `T1_arrivee_quai`                     | `T1_depart_quai`, `T2_demande`, `Ouverture_portes_T1` | M0, M10, M11       |
| **M7**  | `Quai_libre, Portes_fermees, T1_sur_canton, T2_attente`                   | M3 + `T2_demande` (ou M4 + `T1_entree_canton`) | `T1_arrivee_quai`                                 | M10                |
| **M8**  | `Quai_libre, Portes_fermees, T1_attente, T2_sur_canton`                   | M4 + `T2_entree_canton` (ou M5 + `T1_demande`) | `T2_arrivee_quai`                                 | M12                |
| **M9**  | `Canton_libre, Portes_fermees, T1_hors, T2_a_quai`                        | M5 + `T2_arrivee_quai`                     | `T1_demande`, `T2_depart_quai`, `Ouverture_portes_T2` | M0, M12, M13       |
| **M10** | `Canton_libre, Portes_fermees, T1_a_quai, T2_attente`                     | M6 + `T2_demande` (ou M7 + `T1_arrivee_quai`) | `T1_depart_quai`, `T2_entree_canton`, `Ouverture_portes_T1` | M2, M14, M15 |
| **M11** | `Canton_libre, Portes_ouvertes, T1_a_quai, T2_hors`                       | M6 + `Ouverture_portes_T1`                 | `T2_demande`, `Fermeture_portes_T1`                   | M6, M15            |
| **M12** | `Canton_libre, Portes_fermees, T1_attente, T2_a_quai`                     | M8 + `T2_arrivee_quai` (ou M9 + `T1_demande`) | `T1_entree_canton`, `T2_depart_quai`, `Ouverture_portes_T2` | M1, M16, M17 |
| **M13** | `Canton_libre, Portes_ouvertes, T1_hors, T2_a_quai`                       | M9 + `Ouverture_portes_T2`                 | `T1_demande`, `Fermeture_portes_T2`                   | M9, M17            |
| **M14** | `Portes_fermees, T1_a_quai, T2_sur_canton`                                | M10 + `T2_entree_canton`                   | `T1_depart_quai`, `Ouverture_portes_T1`               | M5, M18            |
| **M15** | `Canton_libre, Portes_ouvertes, T1_a_quai, T2_attente`                    | M10 + `Ouverture_portes_T1` (ou M11 + `T2_demande`) | `T2_entree_canton`, `Fermeture_portes_T1`     | M10, M18           |
| **M16** | `Portes_fermees, T1_sur_canton, T2_a_quai`                                | M12 + `T1_entree_canton`                   | `T2_depart_quai`, `Ouverture_portes_T2`               | M3, M19            |
| **M17** | `Canton_libre, Portes_ouvertes, T1_attente, T2_a_quai`                    | M12 + `Ouverture_portes_T2` (ou M13 + `T1_demande`) | `T1_entree_canton`, `Fermeture_portes_T2`    | M12, M19           |
| **M18** | `Portes_ouvertes, T1_a_quai, T2_sur_canton`                               | M14 + `Ouverture_portes_T1` (ou M15 + `T2_entree_canton`) | `Fermeture_portes_T1`                  | M14                |
| **M19** | `Portes_ouvertes, T1_sur_canton, T2_a_quai`                               | M16 + `Ouverture_portes_T2` (ou M17 + `T1_entree_canton`) | `Fermeture_portes_T2`                  | M16                |

**Total : 40 arcs etiquetes** dans le graphe d'accessibilite (sortie analyseur 30/04, cf `comparaison.md` section 6).

**Verifications croisees (toutes vraies)** :
- Tous les marquages avec `Portes_ouvertes=1` (M11, M13, M15, M17, M18, M19) contiennent `T1_a_quai=1` ou `T2_a_quai=1` (PSD-Open).
- Aucun marquage ne contient `T1_sur_canton=1` ET `T2_sur_canton=1` simultanement (exclusion canton).
- Aucun marquage ne contient `T1_a_quai=1` ET `T2_a_quai=1` simultanement (exclusion quai).
- Aucun marquage n'est un deadlock : chacun a au moins 1 transition tirable (cf colonne "Transitions tirables").

**Ecart avec l'estimation initiale** : la cible "15 a 18 marquages" etait sous-estimee. Le chiffre reel **20** vient de l'independance entre l'etat des portes (ouvertes/fermees) et la file d'attente du canton/quai. Six marquages portent `Portes_ouvertes` ; ils correspondent aux configurations ou un train est a quai et l'autre est dans l'un des 4 etats compatibles (`hors`, `attente`, `sur_canton`, et bien sur l'absence de `a_quai` deja exclue par l'invariant quai).

---

## Tache 2 - Verification des invariants de ressource

Pour chaque marquage atteignable Mi, verifier les **3 invariants de ressource**.

### 2.1 Invariant canton

Confirmer : `M(T1_sur_canton) + M(T2_sur_canton) + M(Canton_libre) = 1`

| Marquage | T1_sur_canton | T2_sur_canton | Canton_libre | Somme | OK ? |
|----------|--------------:|--------------:|-------------:|------:|:----:|
| M0       | 0 | 0 | 1 | 1 | ✓ |
| M1       | 0 | 0 | 1 | 1 | ✓ |
| M2       | 0 | 0 | 1 | 1 | ✓ |
| M3       | 1 | 0 | 0 | 1 | ✓ |
| M4       | 0 | 0 | 1 | 1 | ✓ |
| M5       | 0 | 1 | 0 | 1 | ✓ |
| M6       | 0 | 0 | 1 | 1 | ✓ |
| M7       | 1 | 0 | 0 | 1 | ✓ |
| M8       | 0 | 1 | 0 | 1 | ✓ |
| M9       | 0 | 0 | 1 | 1 | ✓ |
| M10      | 0 | 0 | 1 | 1 | ✓ |
| M11      | 0 | 0 | 1 | 1 | ✓ |
| M12      | 0 | 0 | 1 | 1 | ✓ |
| M13      | 0 | 0 | 1 | 1 | ✓ |
| M14      | 0 | 1 | 0 | 1 | ✓ |
| M15      | 0 | 0 | 1 | 1 | ✓ |
| M16      | 1 | 0 | 0 | 1 | ✓ |
| M17      | 0 | 0 | 1 | 1 | ✓ |
| M18      | 0 | 1 | 0 | 1 | ✓ |
| M19      | 1 | 0 | 0 | 1 | ✓ |

**Conclusion 2.1** : invariant canton verifie sur les 20 marquages atteignables. Confirme par l'analyseur (`verifierInvariantCanton`).

### 2.2 Invariant quai

Confirmer : `M(T1_a_quai) + M(T2_a_quai) + M(Quai_libre) = 1`

| Marquage | T1_a_quai | T2_a_quai | Quai_libre | Somme | OK ? |
|----------|----------:|----------:|-----------:|------:|:----:|
| M0       | 0 | 0 | 1 | 1 | ✓ |
| M1       | 0 | 0 | 1 | 1 | ✓ |
| M2       | 0 | 0 | 1 | 1 | ✓ |
| M3       | 0 | 0 | 1 | 1 | ✓ |
| M4       | 0 | 0 | 1 | 1 | ✓ |
| M5       | 0 | 0 | 1 | 1 | ✓ |
| M6       | 1 | 0 | 0 | 1 | ✓ |
| M7       | 0 | 0 | 1 | 1 | ✓ |
| M8       | 0 | 0 | 1 | 1 | ✓ |
| M9       | 0 | 1 | 0 | 1 | ✓ |
| M10      | 1 | 0 | 0 | 1 | ✓ |
| M11      | 1 | 0 | 0 | 1 | ✓ |
| M12      | 0 | 1 | 0 | 1 | ✓ |
| M13      | 0 | 1 | 0 | 1 | ✓ |
| M14      | 1 | 0 | 0 | 1 | ✓ |
| M15      | 1 | 0 | 0 | 1 | ✓ |
| M16      | 0 | 1 | 0 | 1 | ✓ |
| M17      | 0 | 1 | 0 | 1 | ✓ |
| M18      | 1 | 0 | 0 | 1 | ✓ |
| M19      | 0 | 1 | 0 | 1 | ✓ |

**Conclusion 2.2** : invariant quai verifie sur les 20 marquages. Aucune ligne avec deux trains a quai. Confirme par l'analyseur (`verifierInvariantQuai`).

### 2.3 Invariant portes

Confirmer : `M(Portes_fermees) + M(Portes_ouvertes) = 1`

| Marquage | Portes_fermees | Portes_ouvertes | Somme | OK ? |
|----------|---------------:|----------------:|------:|:----:|
| M0..M10  | 1              | 0               | 1     | ✓    |
| M11      | 0              | 1               | 1     | ✓    |
| M12      | 1              | 0               | 1     | ✓    |
| M13      | 0              | 1               | 1     | ✓    |
| M14      | 1              | 0               | 1     | ✓    |
| M15      | 0              | 1               | 1     | ✓    |
| M16      | 1              | 0               | 1     | ✓    |
| M17      | 0              | 1               | 1     | ✓    |
| M18      | 0              | 1               | 1     | ✓    |
| M19      | 0              | 1               | 1     | ✓    |

**Conclusion 2.3** : invariant portes verifie. Six marquages portent `Portes_ouvertes` (M11, M13, M15, M17, M18, M19) ; les 14 autres portent `Portes_fermees`. Aucun n'a les deux a 1 ou les deux a 0. Confirme par l'analyseur (`verifierInvariantPortes`).

---

## Tache 2bis (NEW) - Verification des invariants critiques de surete PSD

C'est **le coeur du nouveau livrable**. Sans cette tache, l'extension PSD n'a pas de valeur formelle.

### 2bis.1 Surete PSD-Open : portes ouvertes implique train a quai

Pour chaque marquage Mi tel que `M(Portes_ouvertes) = 1`, verifier que `M(T1_a_quai) + M(T2_a_quai) = 1`.

| Marquage avec Portes_ouvertes=1 | T1_a_quai | T2_a_quai | Train a quai ? | OK ? |
|---------------------------------|----------:|----------:|----------------|:----:|
| M11 (= M6 + Ouverture_portes_T1) | 1 | 0 | T1 | ✓ |
| M13 (= M9 + Ouverture_portes_T2) | 0 | 1 | T2 | ✓ |
| M15 (= M10 + Ouverture_portes_T1, ou M11 + T2_demande) | 1 | 0 | T1 | ✓ |
| M17 (= M12 + Ouverture_portes_T2, ou M13 + T1_demande) | 0 | 1 | T2 | ✓ |
| M18 (= M14 + Ouverture_portes_T1, ou M15 + T2_entree_canton) | 1 | 0 | T1 | ✓ |
| M19 (= M16 + Ouverture_portes_T2, ou M17 + T1_entree_canton) | 0 | 1 | T2 | ✓ |

**Conclusion 2bis.1** : les **6 marquages avec portes ouvertes** ont tous strictement un train a quai. Aucune chute possible. **CRITIQUE M14**. Confirme programmatiquement par `verifierSurteOuverturePortes` sur les 20 marquages.

### 2bis.2 Surete PSD-Departure : depart possible implique portes fermees

Pour chaque marquage Mi, identifier si la transition `Ti_depart_quai` est tirable. Si oui, verifier que `M(Portes_fermees) = 1`. Cette propriete est en realite **garantie structurellement** par le pre `{Ti_a_quai, Portes_fermees}` de la transition : la verification programmatique sert de defense en profondeur.

| Marquage | T1_a_quai | T2_a_quai | Portes_fermees | t1_depart_quai tirable ? | t2_depart_quai tirable ? | OK PSD-Departure ? |
|----------|----------:|----------:|---------------:|:------------------------:|:------------------------:|:------------------:|
| M0..M5   | 0 | 0 | 1 | NON (T1_a_quai=0) | NON (T2_a_quai=0) | trivial |
| M6       | 1 | 0 | 1 | OUI | NON | ✓ (Portes_fermees=1) |
| M7, M8   | 0 | 0 | 1 | NON | NON | trivial |
| M9       | 0 | 1 | 1 | NON | OUI | ✓ |
| M10      | 1 | 0 | 1 | OUI | NON | ✓ |
| M11      | 1 | 0 | 0 | NON (Portes_fermees=0) | NON | non tirable, surete OK |
| M12      | 0 | 1 | 1 | NON | OUI | ✓ |
| M13      | 0 | 1 | 0 | NON | NON (Portes_fermees=0) | non tirable, surete OK |
| M14      | 1 | 0 | 1 | OUI | NON | ✓ |
| M15      | 1 | 0 | 0 | NON | NON | non tirable, surete OK |
| M16      | 0 | 1 | 1 | NON | OUI | ✓ |
| M17      | 0 | 1 | 0 | NON | NON | non tirable, surete OK |
| M18      | 1 | 0 | 0 | NON | NON | non tirable, surete OK |
| M19      | 0 | 1 | 0 | NON | NON | non tirable, surete OK |

**Conclusion 2bis.2** : un train ne peut quitter le quai que portes fermees. Pour les 6 marquages ou Ti_depart_quai est tirable (M6, M9, M10, M12, M14, M16), `Portes_fermees=1` partout. **CRITIQUE M14**. Confirme programmatiquement par `verifierSurteDepartQuai`.

---

## Tache 3 - Cohérence par train (sur 4 etats au lieu de 3)

Pour chaque train i ∈ {1, 2}, confirmer : `M(Ti_hors) + M(Ti_attente) + M(Ti_sur_canton) + M(Ti_a_quai) = 1`.

### Train 1 (les 20 marquages)

| Marquage | T1_hors | T1_attente | T1_sur_canton | T1_a_quai | Somme |
|----------|--------:|-----------:|--------------:|----------:|------:|
| M0       | 1 | 0 | 0 | 0 | 1 |
| M1       | 0 | 1 | 0 | 0 | 1 |
| M2       | 1 | 0 | 0 | 0 | 1 |
| M3       | 0 | 0 | 1 | 0 | 1 |
| M4       | 0 | 1 | 0 | 0 | 1 |
| M5       | 1 | 0 | 0 | 0 | 1 |
| M6       | 0 | 0 | 0 | 1 | 1 |
| M7       | 0 | 0 | 1 | 0 | 1 |
| M8       | 0 | 1 | 0 | 0 | 1 |
| M9       | 1 | 0 | 0 | 0 | 1 |
| M10      | 0 | 0 | 0 | 1 | 1 |
| M11      | 0 | 0 | 0 | 1 | 1 |
| M12      | 0 | 1 | 0 | 0 | 1 |
| M13      | 1 | 0 | 0 | 0 | 1 |
| M14      | 0 | 0 | 0 | 1 | 1 |
| M15      | 0 | 0 | 0 | 1 | 1 |
| M16      | 0 | 0 | 1 | 0 | 1 |
| M17      | 0 | 1 | 0 | 0 | 1 |
| M18      | 0 | 0 | 0 | 1 | 1 |
| M19      | 0 | 0 | 1 | 0 | 1 |

**Conclusion T1** : exactement un etat T1 actif a chaque marquage. Cohérent avec la machine d'etats du `Train1` cote Akka.

### Train 2 (les 20 marquages)

| Marquage | T2_hors | T2_attente | T2_sur_canton | T2_a_quai | Somme |
|----------|--------:|-----------:|--------------:|----------:|------:|
| M0       | 1 | 0 | 0 | 0 | 1 |
| M1       | 1 | 0 | 0 | 0 | 1 |
| M2       | 0 | 1 | 0 | 0 | 1 |
| M3       | 1 | 0 | 0 | 0 | 1 |
| M4       | 0 | 1 | 0 | 0 | 1 |
| M5       | 0 | 0 | 1 | 0 | 1 |
| M6       | 1 | 0 | 0 | 0 | 1 |
| M7       | 0 | 1 | 0 | 0 | 1 |
| M8       | 0 | 0 | 1 | 0 | 1 |
| M9       | 0 | 0 | 0 | 1 | 1 |
| M10      | 0 | 1 | 0 | 0 | 1 |
| M11      | 1 | 0 | 0 | 0 | 1 |
| M12      | 0 | 0 | 0 | 1 | 1 |
| M13      | 0 | 0 | 0 | 1 | 1 |
| M14      | 0 | 0 | 1 | 0 | 1 |
| M15      | 0 | 1 | 0 | 0 | 1 |
| M16      | 0 | 0 | 0 | 1 | 1 |
| M17      | 0 | 0 | 0 | 1 | 1 |
| M18      | 0 | 0 | 1 | 0 | 1 |
| M19      | 0 | 0 | 0 | 1 | 1 |

**Conclusion T2** : exactement un etat T2 actif a chaque marquage. Confirme programmatiquement par `verifierInvariantsParTrain`.

---

## Tache 4 - Absence de deadlock par exhaustion

Pour chaque marquage Mi, lister au moins une transition tirable. Aucun marquage atteignable ne doit etre un deadlock (sauf eventuellement le marquage initial, mais ici M0 a deja 2 transitions tirables).

| Marquage | Au moins une transition tirable ? | Laquelle (parmi celles enumerees) ? |
|----------|:---------------------------------:|--------------------------------------|
| M0       | OUI (2) | `T1_demande`, `T2_demande` |
| M1       | OUI (2) | `T1_entree_canton`, `T2_demande` |
| M2       | OUI (2) | `T1_demande`, `T2_entree_canton` |
| M3       | OUI (2) | `T1_arrivee_quai`, `T2_demande` |
| M4       | OUI (2) | `T1_entree_canton`, `T2_entree_canton` |
| M5       | OUI (2) | `T1_demande`, `T2_arrivee_quai` |
| M6       | OUI (3) | `T1_depart_quai`, `T2_demande`, `Ouverture_portes_T1` |
| M7       | OUI (1) | `T1_arrivee_quai` |
| M8       | OUI (1) | `T2_arrivee_quai` |
| M9       | OUI (3) | `T1_demande`, `T2_depart_quai`, `Ouverture_portes_T2` |
| M10      | OUI (3) | `T1_depart_quai`, `T2_entree_canton`, `Ouverture_portes_T1` |
| M11      | OUI (2) | `T2_demande`, `Fermeture_portes_T1` |
| M12      | OUI (3) | `T1_entree_canton`, `T2_depart_quai`, `Ouverture_portes_T2` |
| M13      | OUI (2) | `T1_demande`, `Fermeture_portes_T2` |
| M14      | OUI (2) | `T1_depart_quai`, `Ouverture_portes_T1` |
| M15      | OUI (2) | `T2_entree_canton`, `Fermeture_portes_T1` |
| M16      | OUI (2) | `T2_depart_quai`, `Ouverture_portes_T2` |
| M17      | OUI (2) | `T1_entree_canton`, `Fermeture_portes_T2` |
| M18      | OUI (1) | `Fermeture_portes_T1` |
| M19      | OUI (1) | `Fermeture_portes_T2` |

**Total des arcs (= somme du nombre de transitions tirables) : 40**, conforme a la sortie de l'analyseur (cf `comparaison.md` section 6.1).

**Conclusion** : aucun marquage n'est un deadlock. Le systeme peut toujours progresser. Confirme programmatiquement par `Analyseur.deadlocks` qui renvoie une liste vide.

---

## Tache 5 - Formalisation LTL des proprietes

### Proprietes existantes (a maintenir)

**Safety canton (existant)** :
```
G !(T1_sur_canton AND T2_sur_canton)
```
Lecture : "il n'arrive jamais que les deux trains soient simultanement sur le canton".

**Liveness canton (existant, sous fairness)** :
```
G (T1_attente -> F T1_sur_canton)
G (T2_attente -> F T2_sur_canton)
```

### Proprietes ajoutees pour le quai et les portes

**Safety quai** :
```
G !(T1_a_quai AND T2_a_quai)
```

**Safety PSD-Open (CRITIQUE)** :
```
G (Portes_ouvertes -> (T1_a_quai OR T2_a_quai))
```
Lecture : "chaque fois que les portes sont ouvertes, un train est a quai". C'est la propriete qui empeche les chutes mortelles.

**Safety PSD-Departure (CRITIQUE, traitee comme invariant de tirabilite)** :
```
G ((T1_a_quai AND X(T1_hors)) -> Portes_fermees)
G ((T2_a_quai AND X(T2_hors)) -> Portes_fermees)
```
Lecture : "si un train passe de a_quai a hors (donc execute Ti_depart_quai), alors les portes etaient fermees a l'instant du passage". C'est la propriete qui empeche un voyageur d'etre coince.

**Liveness PSD (sous fairness des portes, propriete documentaire complementaire)** :
```
G (T1_a_quai -> F Portes_ouvertes)
G (T2_a_quai -> F Portes_ouvertes)
```
Lecture : un train a quai finit par avoir les portes ouvertes (les voyageurs peuvent monter/descendre).

### Justification par model-checking sur etat fini (verifie programmatiquement 30/04)

Le graphe d'accessibilite est **fini** (20 marquages, 40 arcs etiquetes, cf section 6 de `comparaison.md`). Les 5 proprietes LTL principales affichees par `Analyseur.main` sont les 3 Safety ci-dessous et les 2 Liveness canton T1/T2. PSD-Departure est verifiee en 2bis.2 comme invariant critique de tirabilite ; la Liveness PSD est conservee comme propriete documentaire complementaire.

| Propriete LTL                                     | Type     | Reduction sur etat fini                                                                  | Methode (`Analyseur.scala`)                  | Resultat |
|---------------------------------------------------|----------|------------------------------------------------------------------------------------------|----------------------------------------------|:--------:|
| `G !(T1_sur_canton AND T2_sur_canton)`            | Safety   | Aucun M_i parmi M0..M19 n'a `T1_sur_canton=1 AND T2_sur_canton=1` (cf tableau 2.1).      | `verifierGSafety` + predicat                 | PASSE    |
| `G !(T1_a_quai AND T2_a_quai)`                    | Safety   | Aucun M_i n'a `T1_a_quai=1 AND T2_a_quai=1` (cf tableau 2.2).                            | `verifierGSafety` + predicat                 | PASSE    |
| `G (Portes_ouvertes -> (T1_a_quai OR T2_a_quai))` | Safety   | Les 6 marquages avec `Portes_ouvertes=1` (M11, M13, M15, M17, M18, M19) ont chacun `T1_a_quai=1` ou `T2_a_quai=1` (cf tache 2bis.1). | `verifierGSafety(_, verifierSurteOuverturePortes)` | PASSE    |
| `G (T1_attente -> F T1_sur_canton)`               | Liveness | Depuis chaque M_i avec `T1_attente=1`, BFS dans le graphe d'arcs trouve un descendant avec `T1_sur_canton=1` (sous fairness FIFO du `SectionController`, cf tache 6). | `verifierGFLiveness`                         | PASSE    |
| `G (T2_attente -> F T2_sur_canton)`               | Liveness | Depuis chaque M_i avec `T2_attente=1`, BFS dans le graphe d'arcs trouve un descendant avec `T2_sur_canton=1` (sous fairness FIFO du `SectionController`, cf tache 6). | `verifierGFLiveness`                         | PASSE    |

**Validite du raisonnement** : pour Safety, la propriete `G p` est equivalente a "tout marquage atteignable verifie p" car le graphe est complet. Pour Liveness `G (p -> F q)`, on verifie que tout marquage source (p tient) admet un chemin dans le graphe d'accessibilite vers un marquage cible (q tient) ; sous l'hypothese de fairness FIFO documentee (Q11 du `protocole-coordination.md`), ce chemin sera effectivement emprunte. La verification programmatique est cross-validee par 7 tests dans `AnalyseurSpec` (suite "Verification LTL programmatique (Phase 7)").

**Reduction PSD-Departure -> graphe d'accessibilite** : le predicat `X(Ti_hors)` est encode par "il existe un arc sortant de M_i etiquete `Ti_depart_quai`" (chaque arc transporte la transition tiree). C'est exactement la semantique de Kripke induite par le reseau de Petri, et c'est pourquoi cette propriete est verifiee comme invariant de tirabilite dans `verifierSurteDepartQuai`.

---

## Tache 6 - Argumentation Liveness sous fairness (etendue)

### Le probleme (rappel)

En Petri pur, depuis un marquage avec 2 trains en attente, l'execution adversaire peut toujours servir le meme train, affamant l'autre.

### La parade (etendue avec PSD)

Cote Akka :
- Le `SectionController` a une file FIFO (existant).
- Le `QuaiController` a une file FIFO. Quand un train sort du canton et trouve le quai libre, il y entre directement. Sinon il attend dans la file du quai.
- Le `GestionnairePortes` n'a pas de file (un seul train a quai a la fois grace a l'invariant 2.2). Pas de famine sur les portes.

### Limites assumees (renseignees 30/04)

- Le modele Petri pur ne garantit pas la Liveness sans hypothese de fairness : depuis M4 (deux trains en attente), une execution adversaire qui ne tirerait jamais `T2_entree_canton` laisse T2 indefiniment dans `T2_attente`. Le graphe d'accessibilite contient bien le chemin `M4 -> M8 -> M12 -> M9` (T2 finit a quai), mais sans equite ce chemin n'est pas force d'etre emprunte.
- L'implementation Akka avec FIFO la garantit pour toute sequence finie : la file `attente` du `SectionController` (resp. `QuaiController`) est servie dans l'ordre d'arrivee, donc tout train mis en attente est eventuellement servi. C'est documente dans `protocole-coordination.md` Q11.
- Sur les 3 scenarios retenus, la Liveness est verifiable par inspection des marquages traverses (cf `comparaison.md` section 6.4) : scenario 1 atteint M6 et revient a M0 ; scenario 2 atteint M9 (T2 a quai) ; scenario 3 ne quitte pas M0 (refus silencieux).
- **Limite specifique PSD** : si les portes ne se ferment jamais (bug dans le `GestionnairePortes` qui n'envoie jamais `Fermeture_portes_Ti`), le train reste indefiniment a quai. `Portes_ouvertes` est deja atteint, mais la **Liveness de retour** (`Ti_a_quai -> F Ti_hors`) echoue. Ce cas est defendu cote Akka par les tests `GestionnairePortesSpec` qui valident le cycle `Ouverture -> Fermeture -> reset`, et cote Petri par les arcs de fermeture obligatoires depuis les noeuds M11, M13, M15, M17, M18 et M19.

---

## Tache 7 - Diagramme d'espace d'etats (graphe a la main)

### Objectif

Dessiner un graphe ou les noeuds sont les **20 marquages (M0..M19)** et les arcs sont les **40 transitions etiquetees**. Cette representation est centrale pour le rapport (livrable L4 section 5).

### Format produit

Liste exhaustive des arcs, groupee par marquage source (sortie verbatim de l'analyseur le 30/04, cf section "Graphe d'accessibilite" du `runMain m14.petri.Analyseur`). Total : **40 arcs**, exactement la somme des transitions tirables de la tache 4.

```
M0  --T1_demande-->        M1
M0  --T2_demande-->        M2
M1  --T1_entree_canton-->  M3
M1  --T2_demande-->        M4
M2  --T1_demande-->        M4
M2  --T2_entree_canton-->  M5
M3  --T1_arrivee_quai-->   M6
M3  --T2_demande-->        M7
M4  --T1_entree_canton-->  M7
M4  --T2_entree_canton-->  M8
M5  --T1_demande-->        M8
M5  --T2_arrivee_quai-->   M9
M6  --T1_depart_quai-->    M0   (boucle de retour)
M6  --T2_demande-->        M10
M6  --Ouverture_portes_T1--> M11
M7  --T1_arrivee_quai-->   M10
M8  --T2_arrivee_quai-->   M12
M9  --T1_demande-->        M12
M9  --T2_depart_quai-->    M0   (boucle de retour)
M9  --Ouverture_portes_T2--> M13
M10 --T1_depart_quai-->    M2
M10 --T2_entree_canton-->  M14
M10 --Ouverture_portes_T1--> M15
M11 --T2_demande-->        M15
M11 --Fermeture_portes_T1->M6
M12 --T1_entree_canton-->  M16
M12 --T2_depart_quai-->    M1
M12 --Ouverture_portes_T2--> M17
M13 --T1_demande-->        M17
M13 --Fermeture_portes_T2->M9
M14 --T1_depart_quai-->    M5
M14 --Ouverture_portes_T1--> M18
M15 --T2_entree_canton-->  M18
M15 --Fermeture_portes_T1->M10
M16 --T2_depart_quai-->    M3
M16 --Ouverture_portes_T2--> M19
M17 --T1_entree_canton-->  M19
M17 --Fermeture_portes_T2->M12
M18 --Fermeture_portes_T1->M14
M19 --Fermeture_portes_T2->M16
```

### Vue ASCII condensee (cycle nominal + branches PSD)

```
                        M0
                       /  \
                T1_dem    T2_dem
                    /        \
                  M1          M2
                 /  \         /  \
           T1_ec   T2_dem  T1_dem T2_ec
              /      \      /       \
            M3        M4 (sym)        M5
           /  \                      /  \
       T1_aq  T2_dem              T1_dem T2_aq
          /     \                    /      \
        M6       M7                M8        M9
       /|\        |                 |       /|\
   T1_dq | Ouv_T1 T1_aq         T2_aq   T2_dq | Ouv_T2
     /   |   \      |              |      \   |    \
   M0   M10  M11   M10            M12     M0  M12   M13
        /|\    \                   /|\          \
   T1_dq Ouv  Ferm           Ouv_T2 T2_dq        Ferm
    /    T1   T1                T2    \           T2
   M2   M15   M6              M17     M16          M9
        / \                    / \
     T2_ec Ferm            Ferm  T1_ec
        |    T1               T2     |
       M18   M10              M12   M19
        |                            |
       Ferm                         Ferm
        T1                           T2
        |                            |
       M14                          M16
```

(Legende compacte : `T1_dem`=`T1_demande`, `T1_ec`=`T1_entree_canton`, `T1_aq`=`T1_arrivee_quai`, `T1_dq`=`T1_depart_quai`, `Ouv_T1`=`Ouverture_portes_T1`, `Ferm_T1`=`Fermeture_portes_T1` ; idem T2.)

### Mise en evidence des marquages "Portes_ouvertes" (criticite PSD-Open)

Marquages portant `Portes_ouvertes=1` : **M11, M13, M15, M17, M18, M19** (6 marquages sur 20). Tous contiennent `T1_a_quai=1` ou `T2_a_quai=1` -> aucune ouverture sans train present. Ce sont les seuls noeuds atteints depuis une transition `Ouverture_portes_Ti`, et ils ont chacun exactement une sortie `Fermeture_portes_Ti` qui boucle vers le marquage portes-fermees correspondant. Aucun cycle infini avec portes ouvertes.

### Insertion finale

Ce diagramme est repris en **annexe A1 du rapport** (`rapport/Rapport_CriticalSystemModel.docx.md`) avec :
- la liste des 40 arcs (verbatim ci-dessus, sortie de `Analyseur`) ;
- la cartographie des 6 marquages "portes ouvertes" en couleur ;
- les 2 boucles de retour vers M0 (M6 et M9) qui ferment le cycle complet d'un train.

---

## Synchronisation avec la piste codage

### Analyseur Scala etendu (livrable L5) operationnel

Il produit :
- [x] Une liste de **20 marquages** atteignables (correspondance avec la tache 1).
- [x] Verification des **3 invariants de ressource** sur chaque marquage (taches 2.1, 2.2, 2.3).
- [x] Verification des **2 invariants critiques PSD** (taches 2bis.1, 2bis.2).
- [x] Verification des invariants par train sur 4 etats (tache 3).
- [x] Confirmation qu'aucun marquage n'est un deadlock (tache 4).
- [x] Graphe d'accessibilite complet : **40 arcs etiquetes**.
- [x] Verification LTL programmatique : **5 proprietes PASSE** sur le graphe fini.

Si le code et le carnet divergent, **on debugge le code en priorite** (le carnet est la reference). En cas d'erreur dans le carnet, on corrige et on re-explique dans `documentation/suivi/historique.md`.

### Ou inserer les sorties dans le rapport

| Section du rapport       | Source carnet | Source code |
|--------------------------|---------------|-------------|
| 5.1 Invariant canton     | Tache 2.1     | Sortie analyseur (`verifierInvariantCanton`) |
| 5.2 Invariant quai       | Tache 2.2     | Sortie analyseur (`verifierInvariantQuai`)   |
| 5.3 Invariant portes     | Tache 2.3     | Sortie analyseur (`verifierInvariantPortes`) |
| 5.4 Cohérence par train  | Tache 3       | Sortie analyseur                             |
| **5.5.1 Surete PSD-Open**  | Tache 2bis.1  | Sortie analyseur (`verifierSurteOuverturePortes`) **CRITIQUE** |
| **5.5.2 Surete PSD-Departure** | Tache 2bis.2 | Sortie analyseur (`verifierSurteDepartQuai`) **CRITIQUE** |
| 5.7 Absence de deadlock  | Tache 4       | Sortie analyseur                             |
| 5.8 LTL                  | Taches 5 + 6  | `verifierGSafety`, `verifierGFLiveness`      |
| Annexe A1 (graphe)       | Tache 7       | Sortie brute analyseur                       |

---

## Repartition possible dans l'equipe

Suggestion (modulable selon les disponibilites) :

| Membre        | Taches recommandees |
|---------------|---------------------|
| Contributeur 1 | Taches 1 (enumeration M0-M9) + 5 (LTL) |
| Contributeur 2 | Taches 1 (enumeration M10-M17) + 4 (deadlock) |
| Contributeur 3 | Taches 2 (3 invariants ressources) + 3 (cohérence par train) |
| Contributeur 4 (Nikko) | Code Scala + tache 7 (diagramme final) + integration des resultats dans le rapport |

Les taches 1, 2, 4 sont sequentielles (la 1 produit les marquages que les autres utilisent), les taches 5, 6, 7 sont independantes apres la tache 1. La **tache 2bis (PSD)** est la plus importante academiquement et doit etre relue par 2 contributeurs.

---

## Definition of Done de ce carnet

Le carnet est complet quand :
- [x] Tous les tableaux sont remplis (pas de `_` ou `?` restants).
- [x] Le diagramme de la tache 7 est joint (image ou ASCII).
- [x] Une entree `historique.md` a ete ajoutee a chaque tache terminee.
- [x] Les resultats convergent avec ceux de l'analyseur Scala en Phase D / Phase 7.
- [x] **La tache 2bis (PSD) est integralement verifiee et chaque marquage avec `Portes_ouvertes=1` a son train a quai documente.**
- [x] Les sections 5.1 a 5.8 du rapport ont ete remplies a partir de ce carnet.

---

## Annexe : carnet initial (modele a 7 places, archive)

L'enumeration originale a 8 marquages pour le modele "1 troncon, 7 places, 6 transitions" est conservee a titre historique dans le commit `bf7fbc1` ainsi que dans l'historique git. Elle a servi de base pedagogique avant l'extension PSD.
