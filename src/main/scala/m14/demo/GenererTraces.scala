package m14.demo

import m14.demo.TraceWriter._
import m14.petri.PetriNet._

object GenererTraces extends App {

  // les traces servent surtout pour la demo HTML.
  val scenarioA = new Constructeur(
    id = "nominal",
    titre = "Cycle nominal complet (canton + quai + portes)",
    description = "Un seul train. Demande canton -> entree -> arrivee quai -> ouverture portes -> fermeture portes -> depart quai."
  )
    .tirer("Train1 demande l'acces au canton",
      Some(MessageAkka("Train1", "SectionController", "Demande")), t1Demande)
    .tirer("Canton libre : Train1 entre dans le canton",
      Some(MessageAkka("SectionController", "Train1", "Autorisation")), t1EntreeCanton)
    .tirer("Train1 arrive au quai (libere le canton)",
      Some(MessageAkka("Train1", "QuaiController", "ArriveeQuai")), t1ArriveeQuai)
    .tirer("Train1 demande l'ouverture des portes (autorisee : il est a quai)",
      Some(MessageAkka("Train1", "GestionnairePortes", "OuverturePortes")), ouvertureT1)
    .tirer("Train1 demande la fermeture des portes",
      Some(MessageAkka("Train1", "GestionnairePortes", "FermeturePortes")), fermetureT1)
    .tirer("Train1 quitte le quai (autorise : portes fermees)",
      Some(MessageAkka("Train1", "QuaiController", "DepartQuai")), t1DepartQuai)
    .construire()

  val scenarioB = new Constructeur(
    id = "concurrence",
    titre = "Concurrence canton + quai (2 trains)",
    description = "Train1 et Train2 demandent quasi-simultanement. T2 doit attendre canton, puis quai."
  )
    .tirer("Train1 demande le canton",
      Some(MessageAkka("Train1", "SectionController", "Demande")), t1Demande)
    .tirer("Train1 entre dans le canton",
      Some(MessageAkka("SectionController", "Train1", "Autorisation")), t1EntreeCanton)
    .tirer("Train2 demande le canton (sera mis en attente)",
      Some(MessageAkka("Train2", "SectionController", "Demande")), t2Demande)
    .message("Canton occupe : Train2 recoit Attente",
      MessageAkka("SectionController", "Train2", "Attente"))
    .tirer("Train1 arrive au quai (libere le canton)",
      Some(MessageAkka("Train1", "QuaiController", "ArriveeQuai")), t1ArriveeQuai)
    .tirer("Canton libre : Train2 peut enfin entrer",
      Some(MessageAkka("SectionController", "Train2", "Autorisation")), t2EntreeCanton)
    .tirer("Train1 ouvre les portes (a quai)",
      Some(MessageAkka("Train1", "GestionnairePortes", "OuverturePortes")), ouvertureT1)
    .tirer("Train1 ferme les portes",
      Some(MessageAkka("Train1", "GestionnairePortes", "FermeturePortes")), fermetureT1)
    .tirer("Train1 quitte le quai (libere le quai)",
      Some(MessageAkka("Train1", "QuaiController", "DepartQuai")), t1DepartQuai)
    .tirer("Train2 arrive au quai a son tour",
      Some(MessageAkka("Train2", "QuaiController", "ArriveeQuai")), t2ArriveeQuai)
    .construire()

  val scenarioC = new Constructeur(
    id = "violation",
    titre = "Tentative PSD invalide (CRITIQUE - bloquee)",
    description = "Tentative d'ouverture des portes alors qu'aucun train n'est a quai. Petri refuse (transition non tirable) ; cote Akka, le message est hors protocole car seul un Train en etat a_quai emet OuverturePortes."
  )
    .violation(
      label = "Tentative hors protocole : OuverturePortes(Train1) alors qu'aucun train n'est a quai",
      akka = Some(MessageAkka("HorsProtocole", "GestionnairePortes", "OuverturePortes(Train1)")),
      transition = Some(ouvertureT1)
    )
    .violation(
      label = "Petri refuse : T1_a_quai=0 -> Ouverture_portes_T1 non tirable. TrainSpec garantit que le vrai Train n'emet pas ce message.",
      akka = None,
      transition = None
    )
    .construire()

  val scenarioD = new Constructeur(
    id = "cycle-deux-trains",
    titre = "Cycle complet sequentiel des 2 trains (liveness)",
    description = "T1 effectue son cycle complet (canton -> quai -> portes -> depart). Le systeme revient a un etat ou T2 peut a son tour faire son cycle complet. Demonstration de la liveness."
  )
    .tirer("Train1 demande le canton",
      Some(MessageAkka("Train1", "SectionController", "Demande")), t1Demande)
    .tirer("Train1 entre dans le canton",
      Some(MessageAkka("SectionController", "Train1", "Autorisation")), t1EntreeCanton)
    .tirer("Train1 arrive au quai",
      Some(MessageAkka("Train1", "QuaiController", "ArriveeQuai")), t1ArriveeQuai)
    .tirer("Train1 ouvre les portes",
      Some(MessageAkka("Train1", "GestionnairePortes", "OuverturePortes")), ouvertureT1)
    .tirer("Train1 ferme les portes",
      Some(MessageAkka("Train1", "GestionnairePortes", "FermeturePortes")), fermetureT1)
    .tirer("Train1 quitte le quai (retour a un marquage T1_hors)",
      Some(MessageAkka("Train1", "QuaiController", "DepartQuai")), t1DepartQuai)
    .tirer("Train2 demande le canton (le systeme est libre)",
      Some(MessageAkka("Train2", "SectionController", "Demande")), t2Demande)
    .tirer("Train2 entre dans le canton",
      Some(MessageAkka("SectionController", "Train2", "Autorisation")), t2EntreeCanton)
    .tirer("Train2 arrive au quai",
      Some(MessageAkka("Train2", "QuaiController", "ArriveeQuai")), t2ArriveeQuai)
    .tirer("Train2 ouvre les portes",
      Some(MessageAkka("Train2", "GestionnairePortes", "OuverturePortes")), ouvertureT2)
    .tirer("Train2 ferme les portes",
      Some(MessageAkka("Train2", "GestionnairePortes", "FermeturePortes")), fermetureT2)
    .tirer("Train2 quitte le quai. Cycle complet des 2 trains termine.",
      Some(MessageAkka("Train2", "QuaiController", "DepartQuai")), t2DepartQuai)
    .construire()

  val scenarioE = new Constructeur(
    id = "violation-depart",
    titre = "Tentative depart portes ouvertes (CRITIQUE - bloquee)",
    description = "T1 est a quai portes ouvertes. Un signal DepartQuai est envoye prematurement. La transition Petri n'est pas tirable (Portes_fermees=0). Invariant PSD-Departure preserve."
  )
    .tirer("Train1 demande le canton",
      Some(MessageAkka("Train1", "SectionController", "Demande")), t1Demande)
    .tirer("Train1 entre dans le canton",
      Some(MessageAkka("SectionController", "Train1", "Autorisation")), t1EntreeCanton)
    .tirer("Train1 arrive au quai",
      Some(MessageAkka("Train1", "QuaiController", "ArriveeQuai")), t1ArriveeQuai)
    .tirer("Train1 ouvre les portes (legitime : a quai)",
      Some(MessageAkka("Train1", "GestionnairePortes", "OuverturePortes")), ouvertureT1)
    .violation(
      label = "Tentative DepartQuai alors que Portes_ouvertes=1 -> transition NON tirable",
      akka = Some(MessageAkka("Train1", "QuaiController", "DepartQuai (premature)")),
      transition = Some(t1DepartQuai)
    )
    .violation(
      label = "Garde de surete : Portes_fermees=0 -> message ignore. Invariant PSD-Departure preserve.",
      akka = None,
      transition = None
    )
    .tirer("Sequence corrigee : Train1 ferme d'abord les portes",
      Some(MessageAkka("Train1", "GestionnairePortes", "FermeturePortes")), fermetureT1)
    .tirer("Maintenant DepartQuai est legitime (Portes_fermees=1)",
      Some(MessageAkka("Train1", "QuaiController", "DepartQuai")), t1DepartQuai)
    .construire()

  val sortie = "demo"
  // on ecrase les anciens fichiers, c'est voulu.
  ecrire(scenarioA, s"$sortie/trace-nominal.json")
  ecrire(scenarioB, s"$sortie/trace-concurrence.json")
  ecrire(scenarioC, s"$sortie/trace-violation.json")
  ecrire(scenarioD, s"$sortie/trace-cycle-deux-trains.json")
  ecrire(scenarioE, s"$sortie/trace-violation-depart.json")

  println(s"Traces ecrites dans $sortie/ :")
  println(s"  - trace-nominal.json            (${scenarioA.etapes.size} etapes)")
  println(s"  - trace-concurrence.json        (${scenarioB.etapes.size} etapes)")
  println(s"  - trace-violation.json          (${scenarioC.etapes.size} etapes)")
  println(s"  - trace-cycle-deux-trains.json  (${scenarioD.etapes.size} etapes)")
  println(s"  - trace-violation-depart.json   (${scenarioE.etapes.size} etapes)")
}
