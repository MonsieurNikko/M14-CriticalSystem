package m14.troncon

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable.Queue

object QuaiController {

  import Protocol._

  // meme principe que SectionController, mais pour le quai.
  private type EnAttente = (IdTrain, ActorRef[MessagePourTrain])

  def apply(): Behavior[MessagePourQuai] = quaiLibre()

  private def quaiLibre(): Behavior[MessagePourQuai] = Behaviors.receiveMessage {

    case ArriveeQuai(emetteur, repondreA) =>
      repondreA ! Autorisation
      quaiOccupe(emetteur, Queue.empty)

    case DepartQuai(_) =>
      Behaviors.same
  }

  private def quaiOccupe(occupant: IdTrain, fileAttente: Queue[EnAttente]): Behavior[MessagePourQuai] =
    Behaviors.receiveMessage {

      case ArriveeQuai(emetteur, _) if emetteur == occupant =>
        Behaviors.same

      case ArriveeQuai(emetteur, repondreA) =>
        // Quai pris, donc on attend son tour.
        repondreA ! Attente
        quaiOccupe(occupant, fileAttente.enqueue((emetteur, repondreA)))

      case DepartQuai(emetteur) if emetteur == occupant =>
        promouvoirOuLiberer(fileAttente)

      case DepartQuai(_) =>
        Behaviors.same
    }

  private def promouvoirOuLiberer(fileAttente: Queue[EnAttente]): Behavior[MessagePourQuai] = {
    fileAttente.dequeueOption match {
      case None =>
        quaiLibre()
      case Some(((prochainTrain, prochainRepondreA), reste)) =>
        prochainRepondreA ! Autorisation
        quaiOccupe(prochainTrain, reste)
    }
  }
}
