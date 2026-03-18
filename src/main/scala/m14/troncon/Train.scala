package m14.troncon

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Train {

  import Protocol._

  def apply(
    id: IdTrain,
    sectionController: ActorRef[MessagePourControleur],
    quaiController: ActorRef[MessagePourQuai],
    gestionnairePortes: ActorRef[MessagePourPortes]
  ): Behavior[MessagePourTrain] =
    comportementHors(id, sectionController, quaiController, gestionnairePortes)

  private def comportementHors(
    id: IdTrain,
    sc: ActorRef[MessagePourControleur],
    qc: ActorRef[MessagePourQuai],
    gp: ActorRef[MessagePourPortes]
  ): Behavior[MessagePourTrain] =
    Behaviors.setup { context =>
      // A chaque retour hors zone, on recommence un cycle.
      sc ! Demande(id, context.self)
      comportementEnAttente(id, sc, qc, gp)
    }

  private def comportementEnAttente(
    id: IdTrain,
    sc: ActorRef[MessagePourControleur],
    qc: ActorRef[MessagePourQuai],
    gp: ActorRef[MessagePourPortes]
  ): Behavior[MessagePourTrain] =
    Behaviors.receiveMessage {
      case Autorisation =>
        comportementSurCanton(id, sc, qc, gp)

      case Attente =>
        Behaviors.same

      case PortesOuvertes | PortesFermees =>
        Behaviors.same
    }

  private def comportementSurCanton(
    id: IdTrain,
    sc: ActorRef[MessagePourControleur],
    qc: ActorRef[MessagePourQuai],
    gp: ActorRef[MessagePourPortes]
  ): Behavior[MessagePourTrain] =
    Behaviors.setup { context =>
      qc ! ArriveeQuai(id, context.self)
      Behaviors.receiveMessage {
        case Autorisation =>
          // autorisation du quai: le canton peut etre libere.
          sc ! Sortie(id)
          comportementAQuai(id, sc, qc, gp)

        case Attente =>
          Behaviors.same

        case PortesOuvertes | PortesFermees =>
          Behaviors.same
      }
    }

  private def comportementAQuai(
    id: IdTrain,
    sc: ActorRef[MessagePourControleur],
    qc: ActorRef[MessagePourQuai],
    gp: ActorRef[MessagePourPortes]
  ): Behavior[MessagePourTrain] =
    Behaviors.setup { context =>
      // version simplifiee: ouverture puis fermeture tout de suite.
      gp ! OuverturePortes(id, context.self)
      attendrePortesOuvertes(id, sc, qc, gp)
    }

  private def attendrePortesOuvertes(
    id: IdTrain,
    sc: ActorRef[MessagePourControleur],
    qc: ActorRef[MessagePourQuai],
    gp: ActorRef[MessagePourPortes]
  ): Behavior[MessagePourTrain] =
    Behaviors.receiveMessage {
      case PortesOuvertes =>
        Behaviors.setup { context =>
          gp ! FermeturePortes(id, context.self)
          attendrePortesFermees(id, sc, qc, gp)
        }
      case _ =>
        // ici on ignore, c'est pas le bon moment du cycle.
        Behaviors.same
    }

  private def attendrePortesFermees(
    id: IdTrain,
    sc: ActorRef[MessagePourControleur],
    qc: ActorRef[MessagePourQuai],
    gp: ActorRef[MessagePourPortes]
  ): Behavior[MessagePourTrain] =
    Behaviors.receiveMessage {
      case PortesFermees =>
        qc ! DepartQuai(id)
        comportementHors(id, sc, qc, gp)
      case _ =>
        // Pareil ici, on attend juste PortesFermees.
        Behaviors.same
    }
}
