package m14.troncon

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable.Queue

object SectionController {

  import Protocol._

  // on stocke aussi le ActorRef, sinon impossible de repondre plus tard.
  private type EnAttente = (IdTrain, ActorRef[MessagePourTrain])

  def apply(): Behavior[MessagePourControleur] = etatLibre()

  private def etatLibre(): Behavior[MessagePourControleur] = Behaviors.receiveMessage {

    case Demande(emetteur, repondreA) =>
      repondreA ! Autorisation
      etatOccupe(emetteur, Queue.empty)

    case Sortie(_) =>
      Behaviors.same
  }

  private def etatOccupe(occupant: IdTrain, fileAttente: Queue[EnAttente]): Behavior[MessagePourControleur] =
    Behaviors.receiveMessage {

      case Demande(emetteur, _) if emetteur == occupant =>
        Behaviors.same

      case Demande(emetteur, repondreA) =>
        // file FIFO toute simple.
        repondreA ! Attente
        etatOccupe(occupant, fileAttente.enqueue((emetteur, repondreA)))

      case Sortie(emetteur) if emetteur == occupant =>
        promouvoirOuLiberer(fileAttente)

      case Sortie(_) =>
        Behaviors.same
    }

  private def promouvoirOuLiberer(fileAttente: Queue[EnAttente]): Behavior[MessagePourControleur] = {
    fileAttente.dequeueOption match {
      case None =>
        etatLibre()
      case Some(((prochainTrain, prochainRepondreA), reste)) =>
        prochainRepondreA ! Autorisation
        etatOccupe(prochainTrain, reste)
    }
  }
}
