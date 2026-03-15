# Modélisation et Vérification d'une Application Critique Distribuée
## avec Akka, Scala et Réseaux de Pétri

*6 février 2026*

## Résumé

Ce projet vise à modéliser et vérifier un système distribué critique utilisant Akka et Scala, en recourant aux réseaux de Pétri pour l'analyse formelle. L'objectif est de garantir la fiabilité, l'absence de deadlocks et le respect des invariants métier dans un contexte applicatif complexe.

---

## 1 Contexte et motivation

Les systèmes distribués critiques, tels que les systèmes financiers, industriels ou de télécommunication, nécessitent une conception robuste. Des erreurs comme les deadlocks ou la violation des invariants métier peuvent avoir des conséquences graves. Ce projet propose donc de combiner simulation et vérification formelle pour un système distribué complexe.

---

## 2 Objectifs

1. **État de l'art (vérification formelle et réseaux de Petri)**
   - Étudiez le concept de vérification formelle pour les systèmes critiques.
   - Étudiez la modélisation à l'aide des réseaux de Pétri. Vous devez expliquer comment ces réseaux permettent de représenter et d'analyser le comportement d'un système distribué, en identifiant les propriétés structurelles et les invariants critiques.

   > Remarque : l'utilisation d'outils logiciels de réseaux de Pétri n'est pas autorisée.

2. **Modélisation fonctionnelle et concurrente :**
   - Choisir une application distribuée où les erreurs sont critiques.
   - Définir l'architecture de votre application sous forme d'acteurs Akka.
   - Identifier les flux de messages critiques et les interactions.

3. **Traduction vers un modèle formel :**
   - Construire un réseau de Pétri modélisant votre application.
   - Capturer tous les chemins de communication et états possibles (espace d'états).

4. **Vérification de propriétés :**
   - Propriétés structurelles : validité des transitions, absence de deadlocks, cohérence des séquences.
   - Invariants métiers : contraintes toujours respectées (ex. : un compte ne peut jamais avoir un solde négatif).

5. **Simulation et validation :**
   - Simuler le système Akka/Scala pour observer le comportement réel.
   - Comparer avec les résultats du réseau de Pétri pour valider le modèle.

---

## 3 Approche technique

1. **Akka + Scala :**
   - Définition des acteurs et protocoles de communication.
   - Gestion de la concurrence, supervision, tolérance aux pannes, et tests unitaires.

2. **Réseaux de Petri :**
   - Génération et exploration de l'espace d'états des réseaux de Petri.
   - Conception et développement d'un analyseur pour l'étude des propriétés structurelles et des invariants métier.
   - Initiez-vous à la logique LTL (Linear Temporal Logic). C'est une logique utilisée surtout en vérification formelle et model checking pour exprimer des propriétés sur l'évolution d'un système dans le temps (par exemple : quelque chose finit toujours par arriver, quelque chose n'arrive jamais, etc.). Utilisez-la pour formaliser les propriétés de sûreté et de vivacité de votre application.

---

## 4 Livrables attendus

1. Quelles sources bibliographiques de référence permettent de cadrer théoriquement et méthodologiquement ce projet ?
2. Modèle Akka/Scala fonctionnel simulant le système distribué.
3. Réseau de Pétri représentant les comportements critiques de l'application choisie.
4. Rapport détaillé de vérification des propriétés structurelles et invariants de votre application.
5. Simulation comparée : comportement réel vs modèle formel.
6. Lien vers votre GitHub.
