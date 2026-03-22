package m14.troncon

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object GestionnairePortes {

  import Protocol._

  def apply(): Behavior[MessagePourPortes] = portesFermees()

  private def portesFermees(): Behavior[MessagePourPortes] = Behaviors.receiveMessage {

    case OuverturePortes(emetteur, repondreA) =>
      repondreA ! PortesOuvertes
      portesOuvertes(emetteur)

    case FermeturePortes(_, _) =>
      Behaviors.same
  }

  private def portesOuvertes(occupant: IdTrain): Behavior[MessagePourPortes] =
    Behaviors.receiveMessage {

      case OuverturePortes(emetteur, _) if emetteur == occupant =>
        Behaviors.same

      case OuverturePortes(_, _) =>
        // un autre train tente pendant que c'est deja ouvert: on ignore.
        Behaviors.same

      case FermeturePortes(emetteur, repondreA) if emetteur == occupant =>
        repondreA ! PortesFermees
        portesFermees()

      case FermeturePortes(_, _) =>
        Behaviors.same
    }
}
