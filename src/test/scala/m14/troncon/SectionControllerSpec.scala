package m14.troncon

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import m14.troncon.Protocol._
import org.scalatest.wordspec.AnyWordSpecLike

class SectionControllerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "SectionController" should {

    "autoriser un train seul puis revenir a l'etat libre apres sa sortie" in {
      val controleur = spawn(SectionController())
      val probeTrain1 = createTestProbe[MessagePourTrain]()

      controleur ! Demande(Train1, probeTrain1.ref)
      probeTrain1.expectMessage(Autorisation)

      controleur ! Sortie(Train1)

      val probeTrain2 = createTestProbe[MessagePourTrain]()
      controleur ! Demande(Train2, probeTrain2.ref)
      probeTrain2.expectMessage(Autorisation)
    }

    "autoriser le premier train et mettre le second en attente lors d'une demande concurrente" in {
      val controleur = spawn(SectionController())
      val probeTrain1 = createTestProbe[MessagePourTrain]()
      val probeTrain2 = createTestProbe[MessagePourTrain]()

      controleur ! Demande(Train1, probeTrain1.ref)
      probeTrain1.expectMessage(Autorisation)

      controleur ! Demande(Train2, probeTrain2.ref)
      probeTrain2.expectMessage(Attente)
    }

    "promouvoir le train en attente quand l'occupant sort du troncon" in {
      val controleur = spawn(SectionController())
      val probeTrain1 = createTestProbe[MessagePourTrain]()
      val probeTrain2 = createTestProbe[MessagePourTrain]()

      controleur ! Demande(Train1, probeTrain1.ref)
      probeTrain1.expectMessage(Autorisation)

      controleur ! Demande(Train2, probeTrain2.ref)
      probeTrain2.expectMessage(Attente)

      controleur ! Sortie(Train1)
      probeTrain2.expectMessage(Autorisation)

      controleur ! Sortie(Train2)
      val probeVerification = createTestProbe[MessagePourTrain]()
      controleur ! Demande(Train1, probeVerification.ref)
      probeVerification.expectMessage(Autorisation)
    }
  }
}
