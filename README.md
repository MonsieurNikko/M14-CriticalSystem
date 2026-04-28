# M14 - Modele d'un troncon critique (Akka + Petri)

Projet de Scala 2026 - GIA1 Ing2 S2.

## Sujet

Modelisation et verification d'un troncon critique de la ligne 14 du metro
parisien (deux trains, un canton, un quai, des portes palieres) avec Akka
Typed et un reseau de Petri verifie par exploration exhaustive.

## Structure

- `src/main/scala/m14/` - acteurs Akka (Train, SectionController,
  QuaiController, GestionnairePortes) + analyseur Petri.
- `src/test/scala/m14/` - tests ScalaTest (unitaires + analyse Petri).
- `demo/` - simulateur web HTML/JS pour rejouer des traces.
- `cahier-des-charges.md` - sujet d'origine.

## Lancer

`sbt run`        - demo nominal (deux trains se relayant sur le quai).
`sbt test`       - execute la suite de tests.
`sbt "runMain m14.petri.Analyseur"` - verification d'invariants.

## Auteurs

Duy, Alice, Axel, Ostreann.
