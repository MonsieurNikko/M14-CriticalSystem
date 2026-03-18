package m14.troncon

import akka.actor.typed.ActorRef

object Protocol {

  // deux trains seulement, sinon le Petri devient vite penible a verifier.
  sealed trait IdTrain
  case object Train1 extends IdTrain
  case object Train2 extends IdTrain

  // Canton.
  sealed trait MessagePourControleur

  final case class Demande(emetteur: IdTrain, repondreA: ActorRef[MessagePourTrain]) extends MessagePourControleur
  final case class Sortie(emetteur: IdTrain) extends MessagePourControleur

  // quai.
  sealed trait MessagePourQuai

  final case class ArriveeQuai(emetteur: IdTrain, repondreA: ActorRef[MessagePourTrain]) extends MessagePourQuai
  final case class DepartQuai(emetteur: IdTrain) extends MessagePourQuai

  // Portes.
  sealed trait MessagePourPortes

  final case class OuverturePortes(emetteur: IdTrain, repondreA: ActorRef[MessagePourTrain]) extends MessagePourPortes
  final case class FermeturePortes(emetteur: IdTrain, repondreA: ActorRef[MessagePourTrain]) extends MessagePourPortes

  // reponses recues par les trains.
  sealed trait MessagePourTrain

  case object Autorisation extends MessagePourTrain
  case object Attente extends MessagePourTrain

  case object PortesOuvertes extends MessagePourTrain
  case object PortesFermees extends MessagePourTrain
}
