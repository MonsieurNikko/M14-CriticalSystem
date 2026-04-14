# Demo M14 - Simulation animee (canton + quai + portes palieres)

Cette demo rejoue les scenarios canoniques du sous-systeme critique M14
(extension PSD : 2 trains + canton + quai + portes palieres) sous forme
d'une animation HTML/JS autonome.

Pour la soutenance, la demo se lit aussi comme l'histoire du projet :
les scenarios A/B reprennent l'esprit du socle initial **2 trains / 1 troncon**
(demande, attente, exclusion mutuelle), puis les scenarios C/D/E montrent
l'upgrade final avec quai, portes palieres et proprietes PSD.

## Principe

Une seule source de verite : le code Scala.

```
[Scala] m14.demo.GenererTraces (utilise m14.petri.PetriNet.tirer)
    |
    |  produit
    v
trace-nominal.json | trace-concurrence.json | trace-cycle-deux-trains.json
trace-violation.json | trace-violation-depart.json
    |
    |  fetch() au chargement
    v
[Browser] index.html  -> animations + popups + invariants en direct
```

Le navigateur ne simule rien : il **rejoue** une trace pre-calculee a partir
du vrai reseau de Petri verifie par les tests Scala (49/49 verts).

## Comment lancer

### Recommande pour la soutenance

```bash
sbt "runMain m14.demo.LancerDemo"
```

`LancerDemo` :

1. regenere les 5 fichiers JSON depuis le modele Petri verifie ;
2. demarre un mini serveur HTTP local (port 8000-8010) qui sert le dossier `demo/` ;
3. ouvre automatiquement le navigateur sur `http://localhost:<port>/index.html`.

Pour arreter, revenir dans le terminal et appuyer sur Entree.

### Manuel

Si le port est indisponible ou que le navigateur ne s'ouvre pas :

```bash
sbt "runMain m14.demo.GenererTraces"
cd demo && python3 -m http.server 8000
# puis ouvrir http://localhost:8000 dans un navigateur
```

> Alternative VS Code : extension "Live Server", clic droit sur `index.html` -> Open with Live Server.

## Scenarios disponibles

| Scenario | Trace | Etapes | Demontre |
|----------|-------|-------:|----------|
| **A - Cycle nominal complet** | `trace-nominal.json` | 7 | Train1 enchaine demande -> canton -> quai -> ouverture -> fermeture -> depart |
| **B - Concurrence canton + quai** | `trace-concurrence.json` | 11 | Train2 arrive en parallele, attend canton puis quai, FIFO respecte |
| **C - Cycle complet 2 trains (liveness)** | `trace-cycle-deux-trains.json` | 13 | T1 fait son cycle entier puis T2 enchaine son propre cycle. Demontre la symetrie + le retour a un marquage de type "depart" |
| **D - Tentative PSD-Open invalide (CRITIQUE)** | `trace-violation.json` | 3 | Tentative d'ouverture sans train a quai. Refus Petri (transition non tirable) + message Akka marque hors protocole (le vrai `Train` n'emet `OuverturePortes` qu'en etat `a_quai`, cf `TrainSpec`). Overlay rouge anime. |
| **E - Tentative PSD-Departure portes ouvertes (CRITIQUE)** | `trace-violation-depart.json` | 9 | T1 a quai portes ouvertes, signal DepartQuai premature. Transition Petri non tirable (`Portes_fermees=0`). Sequence corrigee ensuite. |

## Elements visuels

- **Trains animes** : T1 (bleu) et T2 (vert) glissent entre les zones (hors-amont / attente / canton / quai / hors-aval) sur une voie unique unidirectionnelle.
- **Portes palieres** : 2 panneaux qui s'ecartent quand `Portes_ouvertes=1`, changent de couleur (gris -> vert).
- **Reseau de Petri** : 12 places affichees a droite, jetons synchronises avec les transitions.
- **Popups Akka** : chaque message envoye genere un toast violet (`Train1 -> SectionController : Demande`) en bas a droite. Toasts ROUGES pour les transitions non tirables ou messages hors protocole.
- **Bandeau invariants** : 4 pastilles pour les invariants verifiables localement sur un seul marquage (canton, quai, portes, PSD-Open). PSD-Departure est temporel et garanti structurellement par la pre-condition de `Ti_depart_quai` ; il n'a pas de pastille mais est verifie par l'analyseur Scala (cf `Analyseur.verifierSurteDepartQuai`).
- **Overlay violation** : badge rouge "TENTATIVE BLOQUEE par garde de surete" + clignotement quand `violation: true`.

## Controles

- **Lecture / Pause** : auto-play pas par pas.
- **Pas suivant** : avance manuellement.
- **Reset** : revient a M0.
- **Vitesse** : lente (2s) / normale (1.2s) / rapide (0.6s) / pas-a-pas (manuel).
- **Clic sur une etape** dans la timeline : saute directement a ce marquage.

## Format des traces JSON

Voir `src/main/scala/m14/demo/TraceWriter.scala` pour la specification.

```json
{
  "id": "nominal",
  "titre": "...",
  "description": "...",
  "etapes": [
    {
      "label": "Train1 demande l'acces au canton",
      "akka": {"de": "Train1", "vers": "SectionController", "libelle": "Demande"},
      "transition": "T1_demande",
      "marquage": {"Canton_libre": 1, "T1_attente": 1, ...},
      "violation": false
    }
  ]
}
```

## Pour la soutenance

Ordre suggere :

1. **A** (nominal) en vitesse normale -> "voici le cycle de vie d'un train".
2. **B** (concurrence) -> "voici comment SectionController et QuaiController arbitrent en FIFO".
3. **C** (cycle 2 trains) -> "le systeme revient a un marquage de depart, liveness symetrique".
4. **D** (PSD-Open) en vitesse lente -> "propriete CRITIQUE PSD-Open : Petri refuse l'ouverture sans `Ti_a_quai`; cote Akka le vrai Train ne peut pas emettre ce message avant l'etat quai".
5. **E** (PSD-Departure) -> "deuxieme propriete CRITIQUE : un train ne demarre jamais portes ouvertes".

L'ancien prototype standalone (sans Scala) reste en `index-old.html.bak`.
