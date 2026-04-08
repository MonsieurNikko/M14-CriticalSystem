package m14.troncon

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import m14.troncon.Protocol._
import org.scalatest.wordspec.AnyWordSpecLike

class QuaiControllerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "QuaiController" should {

    "donner Autorisation au premier ArriveeQuai (quai libre)" in {
      val quai = spawn(QuaiController())
      val probeT1 = createTestProbe[MessagePourTrain]()

      quai ! ArriveeQuai(Train1, probeT1.ref)
      probeT1.expectMessage(Autorisation)
    }

    "envoyer Attente au second train quand le quai est occupe" in {
      val quai = spawn(QuaiController())
      val probeT1 = createTestProbe[MessagePourTrain]()
      val probeT2 = createTestProbe[MessagePourTrain]()

      quai ! ArriveeQuai(Train1, probeT1.ref)
      probeT1.expectMessage(Autorisation)

      quai ! ArriveeQuai(Train2, probeT2.ref)
      probeT2.expectMessage(Attente)
    }

    "promouvoir le train en attente apres DepartQuai de l occupant" in {
      val quai = spawn(QuaiController())
      val probeT1 = createTestProbe[MessagePourTrain]()
      val probeT2 = createTestProbe[MessagePourTrain]()

      quai ! ArriveeQuai(Train1, probeT1.ref)
      probeT1.expectMessage(Autorisation)
      quai ! ArriveeQuai(Train2, probeT2.ref)
      probeT2.expectMessage(Attente)

      quai ! DepartQuai(Train1)
      probeT2.expectMessage(Autorisation)
    }

    "ignorer DepartQuai d un train qui n est pas l occupant courant" in {
      val quai = spawn(QuaiController())
      val probeT1 = createTestProbe[MessagePourTrain]()
      val probeT2 = createTestProbe[MessagePourTrain]()

      quai ! ArriveeQuai(Train1, probeT1.ref)
      probeT1.expectMessage(Autorisation)

      quai ! DepartQuai(Train2)
      quai ! ArriveeQuai(Train2, probeT2.ref)
      probeT2.expectMessage(Attente)
    }

    "etre reutilisable apres un cycle complet" in {
      val quai = spawn(QuaiController())
      val probeT1 = createTestProbe[MessagePourTrain]()

      quai ! ArriveeQuai(Train1, probeT1.ref)
      probeT1.expectMessage(Autorisation)
      quai ! DepartQuai(Train1)

      quai ! ArriveeQuai(Train1, probeT1.ref)
      probeT1.expectMessage(Autorisation)
    }
  }
}
