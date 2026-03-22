package m14

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import m14.petri.Analyseur
import m14.petri.PetriNet
import m14.petri.PetriNet.{Marking, Transition, marquageInitial,
  t1Demande, t1EntreeCanton, t1ArriveeQuai, t1DepartQuai,
  t2Demande, ouvertureT1, fermetureT1}
import m14.troncon.Protocol._
import m14.troncon.SectionController
import m14.troncon.QuaiController
import m14.troncon.GestionnairePortes

object Main extends App {

  // petit acteur pour afficher les reponses, pas plus.
  private def loggeur(nom: String): Behavior[MessagePourTrain] =
    Behaviors.receiveMessage {
      case Autorisation =>
        println(s"  [$nom] recoit Autorisation")
        Behaviors.same
      case Attente =>
        println(s"  [$nom] recoit Attente")
        Behaviors.same
      case PortesOuvertes =>
        println(s"  [$nom] recoit PortesOuvertes")
        Behaviors.same
      case PortesFermees =>
        println(s"  [$nom] recoit PortesFermees")
        Behaviors.same
    }

  private def afficherMarquage(marquage: Marking): String =
    marquage.filter(_._2 > 0).keys.toList.sorted.mkString("(", ", ", ")")

  private def appliquerTransition(numero: Int, marquage: Marking, transition: Transition): Marking = {
    val nouveauMarquage = PetriNet.tirer(transition, marquage).getOrElse {
      throw new IllegalStateException(s"Transition non tirable dans la demo : ${transition.nom}")
    }
    println(s"  Petri M$numero --${transition.nom}--> ${afficherMarquage(nouveauMarquage)}")
    nouveauMarquage
  }

  private def afficherBilanPetri(): Unit = {
    val resultat = Analyseur.analyser(PetriNet.reseauTroncon)
    println("--- Bilan automatique Petri ---")
    println(s"  Etats atteignables             : ${resultat.nombreEtats}")
    println(s"  Invariant canton               : ${ok(resultat.invariantCantonOk)}")
    println(s"  Invariant quai                 : ${ok(resultat.invariantQuaiOk)}")
    println(s"  Invariant portes               : ${ok(resultat.invariantPortesOk)}")
    println(s"  Invariants par train           : ${ok(resultat.invariantsParTrainOk)}")
    println(s"  PSD-Open Safety (CRITIQUE)     : ${ok(resultat.invariantPsdOpenOk)}")
    println(s"  PSD-Departure Safety (CRITIQUE): ${ok(resultat.invariantPsdDepartureOk)}")
    println(s"  Deadlocks                      : ${resultat.deadlocks.size}")
    println()
  }

  private def ok(b: Boolean): String = if (b) "OK" else "ECHEC"

  private def demo(): Behavior[Unit] = Behaviors.setup { contexte =>

    println("=== Demonstration des 3 scenarios M14 (canton + quai + PSD) ===")
    println()
    println(s"Marquage initial Petri : ${afficherMarquage(marquageInitial)}")
    println()

    println("--- Scenario 1 - Cycle nominal complet (canton + quai + portes) ---")
    val sc1 = contexte.spawn(SectionController(), "sc1")
    val qc1 = contexte.spawn(QuaiController(), "qc1")
    val gp1 = contexte.spawn(GestionnairePortes(), "gp1")
    val log1 = contexte.spawn(loggeur("Train1"), "log-train1-s1")
    var m1 = marquageInitial
    println("  Train1 envoie Demande au canton")
    sc1 ! Demande(Train1, log1)
    m1 = appliquerTransition(1, m1, t1Demande)
    Thread.sleep(120)
    m1 = appliquerTransition(2, m1, t1EntreeCanton)
    Thread.sleep(120)
    println("  Train1 envoie ArriveeQuai")
    qc1 ! ArriveeQuai(Train1, log1)
    m1 = appliquerTransition(3, m1, t1ArriveeQuai)
    Thread.sleep(120)
    println("  Train1 envoie OuverturePortes (autorisee : il est a quai)")
    gp1 ! OuverturePortes(Train1, log1)
    m1 = appliquerTransition(4, m1, ouvertureT1)
    Thread.sleep(120)
    println("  Train1 envoie FermeturePortes")
    gp1 ! FermeturePortes(Train1, log1)
    m1 = appliquerTransition(5, m1, fermetureT1)
    Thread.sleep(120)
    println("  Train1 envoie DepartQuai (autorise : portes fermees)")
    qc1 ! DepartQuai(Train1)
    m1 = appliquerTransition(6, m1, t1DepartQuai)
    Thread.sleep(120)
    println()

    println("--- Scenario 2 - Concurrence canton + quai ---")
    val sc2 = contexte.spawn(SectionController(), "sc2")
    val qc2 = contexte.spawn(QuaiController(), "qc2")
    val log2a = contexte.spawn(loggeur("Train1"), "log-train1-s2")
    val log2b = contexte.spawn(loggeur("Train2"), "log-train2-s2")
    var m2 = marquageInitial
    println("  Train1 demande canton")
    sc2 ! Demande(Train1, log2a)
    m2 = appliquerTransition(1, m2, t1Demande)
    Thread.sleep(80)
    m2 = appliquerTransition(2, m2, t1EntreeCanton)
    Thread.sleep(80)
    println("  Train2 demande canton (sera mis en attente : Attente)")
    sc2 ! Demande(Train2, log2b)
    m2 = appliquerTransition(3, m2, t2Demande)
    Thread.sleep(80)
    println("  Petri : Attente est une notification, pas de transition Petri")
    println("  Train1 demande le quai (libre : Autorisation)")
    qc2 ! ArriveeQuai(Train1, log2a)
    m2 = appliquerTransition(4, m2, t1ArriveeQuai)
    Thread.sleep(80)
    println()

    println("--- Scenario 3 - Tentative PSD invalide (CRITIQUE) ---")
    // Attention: on force un message hors protocole pour comparer Akka et Petri.
    val gp3 = contexte.spawn(GestionnairePortes(), "gp3")
    val log3 = contexte.spawn(loggeur("Attaquant"), "log-attaquant")
    val m3 = marquageInitial
    println("  Attaquant envoie OuverturePortes(Train1) alors qu'aucun train n'est a quai")
    gp3 ! OuverturePortes(Train1, log3)
    println("  -> GestionnairePortes accepte le message (etat fermees -> ouvertes)")
    println("     mais cote Akka c'est une violation : aucun train physique a quai.")
    println("     Cote Petri, la transition correspondante n'est PAS tirable :")
    val tirable = PetriNet.estTirable(ouvertureT1, m3)
    println(s"     PetriNet.estTirable(Ouverture_portes_T1, M0) = $tirable")
    if (!tirable) println("     -> SURETE GARANTIE par le modele Petri (T1_a_quai = 0).")
    Thread.sleep(120)
    println()

    afficherBilanPetri()
    println("=== Fin de la demonstration ===")
    Behaviors.stopped
  }

  val system: ActorSystem[Unit] = ActorSystem(demo(), "demo-extension-psd")
  Thread.sleep(2500)
  system.terminate()
}
