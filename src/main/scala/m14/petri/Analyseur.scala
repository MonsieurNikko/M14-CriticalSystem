package m14.petri

import scala.collection.immutable.Queue

object Analyseur {

  import PetriNet._

  final case class Arc(source: Marking, transition: Transition, cible: Marking)

  final case class ResultatAnalyse(
    marquagesAtteignables: List[Marking],
    invariantCantonOk: Boolean,
    invariantQuaiOk: Boolean,
    invariantPortesOk: Boolean,
    invariantPsdOpenOk: Boolean,
    invariantPsdDepartureOk: Boolean,
    invariantsParTrainOk: Boolean,
    deadlocks: List[Marking],
    nombreEtats: Int,
    arcs: List[Arc] = Nil
  )

  def explorerEspaceEtats(net: Net): List[Marking] =
    explorerAvecArcs(net)._1

  def explorerAvecArcs(net: Net): (List[Marking], List[Arc]) = {
    // BFS classique, pas besoin d'un outil Petri externe pour ce petit reseau.
    var visites: Set[Marking] = Set(net.marquageInitial)
    var file: Queue[Marking] = Queue(net.marquageInitial)
    var marquages: List[Marking] = List(net.marquageInitial)
    var arcs: List[Arc] = Nil

    while (file.nonEmpty) {
      val (marquageCourant, fileRestante) = file.dequeue
      file = fileRestante

      val tirables = transitionsTirables(net, marquageCourant)
      for (transition <- tirables) {
        tirer(transition, marquageCourant) match {
          case Some(nouveauMarquage) =>
            arcs = arcs :+ Arc(marquageCourant, transition, nouveauMarquage)
            if (!visites.contains(nouveauMarquage)) {
              visites = visites + nouveauMarquage
              file = file.enqueue(nouveauMarquage)
              marquages = marquages :+ nouveauMarquage
            }
          case None => ()
        }
      }
    }

    (marquages, arcs)
  }

  def verifierInvariantCanton(marquage: Marking): Boolean = {
    val somme = marquage.getOrElse(T1SurCanton, 0) +
                marquage.getOrElse(T2SurCanton, 0) +
                marquage.getOrElse(CantonLibre, 0)
    somme == 1
  }

  def verifierInvariantQuai(marquage: Marking): Boolean = {
    val somme = marquage.getOrElse(T1AQuai, 0) +
                marquage.getOrElse(T2AQuai, 0) +
                marquage.getOrElse(QuaiLibre, 0)
    somme == 1
  }

  def verifierInvariantPortes(marquage: Marking): Boolean = {
    val somme = marquage.getOrElse(PortesFermees, 0) +
                marquage.getOrElse(PortesOuvertes, 0)
    somme == 1
  }

  def verifierInvariantsParTrain(marquage: Marking): Boolean = {
    val sommeT1 = marquage.getOrElse(T1Hors, 0) +
                  marquage.getOrElse(T1Attente, 0) +
                  marquage.getOrElse(T1SurCanton, 0) +
                  marquage.getOrElse(T1AQuai, 0)
    val sommeT2 = marquage.getOrElse(T2Hors, 0) +
                  marquage.getOrElse(T2Attente, 0) +
                  marquage.getOrElse(T2SurCanton, 0) +
                  marquage.getOrElse(T2AQuai, 0)
    sommeT1 == 1 && sommeT2 == 1
  }

  def verifierSurteOuverturePortes(marquage: Marking): Boolean = {
    val portesOuvertes = marquage.getOrElse(PortesOuvertes, 0)
    if (portesOuvertes >= 1) {
      val sommeAQuai = marquage.getOrElse(T1AQuai, 0) + marquage.getOrElse(T2AQuai, 0)
      sommeAQuai == 1
    } else {
      true
    }
  }

  def verifierSurteDepartQuai(net: Net, marquage: Marking): Boolean = {
    val transitionsDepart = List(t1DepartQuai, t2DepartQuai)
    transitionsDepart.forall { t =>
      if (estTirable(t, marquage)) marquage.getOrElse(PortesFermees, 0) == 1
      else true
    }
  }

  def verifierGSafety(marquages: List[Marking], predicat: Marking => Boolean): Either[Marking, Boolean] = {
    marquages.find(m => !predicat(m)) match {
      case Some(contreExemple) => Left(contreExemple)
      case None                => Right(true)
    }
  }

  def verifierGFLiveness(
    marquages: List[Marking],
    arcs: List[Arc],
    source: Marking => Boolean,
    cible: Marking => Boolean
  ): Either[Marking, Boolean] = {
    // ltl version "petit projet": on cherche juste un chemin vers la cible.
    val successeurs: Map[Marking, List[Marking]] =
      arcs.groupBy(_.source).view.mapValues(_.map(_.cible).distinct).toMap.withDefaultValue(Nil)

    def atteignableDepuis(depart: Marking): Boolean = {
      var visites: Set[Marking] = Set(depart)
      var file: Queue[Marking] = Queue(depart)
      while (file.nonEmpty) {
        val (m, reste) = file.dequeue
        file = reste
        if (cible(m)) return true
        for (s <- successeurs(m)) {
          if (!visites.contains(s)) {
            visites = visites + s
            file = file.enqueue(s)
          }
        }
      }
      false
    }

    marquages.filter(source).find(m => !atteignableDepuis(m)) match {
      case Some(contreExemple) => Left(contreExemple)
      case None                => Right(true)
    }
  }

  def estDeadlock(net: Net, marquage: Marking): Boolean =
    transitionsTirables(net, marquage).isEmpty

  def analyser(net: Net): ResultatAnalyse = {
    val (marquages, arcs) = explorerAvecArcs(net)
    ResultatAnalyse(
      marquagesAtteignables = marquages,
      invariantCantonOk = marquages.forall(verifierInvariantCanton),
      invariantQuaiOk = marquages.forall(verifierInvariantQuai),
      invariantPortesOk = marquages.forall(verifierInvariantPortes),
      invariantPsdOpenOk = marquages.forall(verifierSurteOuverturePortes),
      invariantPsdDepartureOk = marquages.forall(m => verifierSurteDepartQuai(net, m)),
      invariantsParTrainOk = marquages.forall(verifierInvariantsParTrain),
      deadlocks = marquages.filter(m => estDeadlock(net, m)),
      nombreEtats = marquages.size,
      arcs = arcs
    )
  }

  def main(args: Array[String]): Unit = {
    println("=== Analyseur Petri - Canton + Quai + Portes palieres (M14) ===")
    println()

    val resultat = analyser(reseauTroncon)

    println(s"Nombre de marquages atteignables : ${resultat.nombreEtats}")
    println()

    println("--- Marquages atteignables ---")
    resultat.marquagesAtteignables.zipWithIndex.foreach { case (marquage, index) =>
      val placesActives = marquage.filter(_._2 > 0).keys.toList.sorted.mkString(", ")
      println(s"  M$index = ($placesActives)")
    }
    println()

    println("--- Invariants ---")
    println(s"  Invariant canton  (5.1)         : ${statut(resultat.invariantCantonOk)}")
    println(s"  Invariant quai    (5.2)         : ${statut(resultat.invariantQuaiOk)}")
    println(s"  Invariant portes  (5.3)         : ${statut(resultat.invariantPortesOk)}")
    println(s"  Invariants par train            : ${statut(resultat.invariantsParTrainOk)}")
    println(s"  PSD-Open Safety  (6.1) CRITIQUE : ${statut(resultat.invariantPsdOpenOk)}")
    println(s"  PSD-Departure    (6.2) CRITIQUE : ${statut(resultat.invariantPsdDepartureOk)}")
    println()

    if (resultat.deadlocks.isEmpty) {
      println("Deadlocks : AUCUN (le systeme peut toujours progresser)")
    } else {
      println(s"Deadlocks : ${resultat.deadlocks.size} DETECTE(S) !")
      resultat.deadlocks.foreach { m =>
        val placesActives = m.filter(_._2 > 0).keys.toList.sorted.mkString(", ")
        println(s"  DEADLOCK : ($placesActives)")
      }
    }

    println()
    val collisionCanton = resultat.marquagesAtteignables.exists { m =>
      m.getOrElse(T1SurCanton, 0) >= 1 && m.getOrElse(T2SurCanton, 0) >= 1
    }
    val collisionQuai = resultat.marquagesAtteignables.exists { m =>
      m.getOrElse(T1AQuai, 0) >= 1 && m.getOrElse(T2AQuai, 0) >= 1
    }
    println(s"Exclusion mutuelle canton : ${statut(!collisionCanton)}")
    println(s"Exclusion mutuelle quai   : ${statut(!collisionQuai)}")

    println()
    println("--- Graphe d'accessibilite (arcs etiquetes M_i --transition--> M_j) ---")
    val indexParMarquage: Map[Marking, Int] =
      resultat.marquagesAtteignables.zipWithIndex.toMap
    println(s"  ${resultat.arcs.size} arcs au total")
    resultat.arcs.foreach { arc =>
      val i = indexParMarquage.getOrElse(arc.source, -1)
      val j = indexParMarquage.getOrElse(arc.cible, -1)
      println(s"  M$i --${arc.transition.nom}--> M$j")
    }
    println()

    println("--- Verification LTL programmatique (Phase 7) ---")
    val safetyCanton = verifierGSafety(resultat.marquagesAtteignables,
      m => !(m.getOrElse(T1SurCanton, 0) >= 1 && m.getOrElse(T2SurCanton, 0) >= 1))
    val safetyQuai = verifierGSafety(resultat.marquagesAtteignables,
      m => !(m.getOrElse(T1AQuai, 0) >= 1 && m.getOrElse(T2AQuai, 0) >= 1))
    val safetyPsdOpen = verifierGSafety(resultat.marquagesAtteignables,
      verifierSurteOuverturePortes)
    val livenessCantonT1 = verifierGFLiveness(resultat.marquagesAtteignables, resultat.arcs,
      source = m => m.getOrElse(T1Attente, 0) >= 1,
      cible  = m => m.getOrElse(T1SurCanton, 0) >= 1)
    val livenessCantonT2 = verifierGFLiveness(resultat.marquagesAtteignables, resultat.arcs,
      source = m => m.getOrElse(T2Attente, 0) >= 1,
      cible  = m => m.getOrElse(T2SurCanton, 0) >= 1)
    println(s"  G safety canton           (G !(T1 sur AND T2 sur))    : ${statutLtl(safetyCanton)}")
    println(s"  G safety quai             (G !(T1 quai AND T2 quai))  : ${statutLtl(safetyQuai)}")
    println(s"  G safety PSD-Open         (G ouvertes -> Ti a quai)   : ${statutLtl(safetyPsdOpen)}")
    println(s"  G F liveness canton T1    (T1_attente -> F sur_canton): ${statutLtl(livenessCantonT1)}")
    println(s"  G F liveness canton T2    (T2_attente -> F sur_canton): ${statutLtl(livenessCantonT2)}")

    println()
    println("=== Fin de l'analyse ===")
  }

  private def statutLtl(r: Either[Marking, Boolean]): String = r match {
    case Right(_)            => "PASSE"
    case Left(contreExemple) =>
      val places = contreExemple.filter(_._2 > 0).keys.toList.sorted.mkString(", ")
      s"ECHEC (contre-exemple : ($places))"
  }

  private def statut(ok: Boolean): String = if (ok) "PASSE" else "ECHEC"
}
