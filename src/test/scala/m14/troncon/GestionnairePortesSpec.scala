package m14.troncon

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import m14.troncon.Protocol._
import org.scalatest.wordspec.AnyWordSpecLike

class GestionnairePortesSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "GestionnairePortes" should {

    "ouvrir les portes a la demande du premier train (etat ferme)" in {
      val gp = spawn(GestionnairePortes())
      val probeT1 = createTestProbe[MessagePourTrain]()

      gp ! OuverturePortes(Train1, probeT1.ref)
      probeT1.expectMessage(PortesOuvertes)
    }

    "fermer les portes apres demande du train occupant" in {
      val gp = spawn(GestionnairePortes())
      val probeT1 = createTestProbe[MessagePourTrain]()

      gp ! OuverturePortes(Train1, probeT1.ref)
      probeT1.expectMessage(PortesOuvertes)

      gp ! FermeturePortes(Train1, probeT1.ref)
      probeT1.expectMessage(PortesFermees)
    }

    "REFUSER SILENCIEUSEMENT OuverturePortes d un autre train (PSD-Open Safety)" in {
      val gp = spawn(GestionnairePortes())
      val probeT1 = createTestProbe[MessagePourTrain]()
      val probeT2 = createTestProbe[MessagePourTrain]()

      gp ! OuverturePortes(Train1, probeT1.ref)
      probeT1.expectMessage(PortesOuvertes)

      gp ! OuverturePortes(Train2, probeT2.ref)
      probeT2.expectNoMessage()
    }

    "REFUSER SILENCIEUSEMENT FermeturePortes d un train non occupant" in {
      val gp = spawn(GestionnairePortes())
      val probeT1 = createTestProbe[MessagePourTrain]()
      val probeT2 = createTestProbe[MessagePourTrain]()

      gp ! OuverturePortes(Train1, probeT1.ref)
      probeT1.expectMessage(PortesOuvertes)

      gp ! FermeturePortes(Train2, probeT2.ref)
      probeT2.expectNoMessage()

      gp ! FermeturePortes(Train1, probeT1.ref)
      probeT1.expectMessage(PortesFermees)
    }

    "etre reutilisable apres un cycle complet (ouvre/ferme par train different)" in {
      val gp = spawn(GestionnairePortes())
      val probeT1 = createTestProbe[MessagePourTrain]()
      val probeT2 = createTestProbe[MessagePourTrain]()

      gp ! OuverturePortes(Train1, probeT1.ref)
      probeT1.expectMessage(PortesOuvertes)
      gp ! FermeturePortes(Train1, probeT1.ref)
      probeT1.expectMessage(PortesFermees)

      gp ! OuverturePortes(Train2, probeT2.ref)
      probeT2.expectMessage(PortesOuvertes)
      gp ! FermeturePortes(Train2, probeT2.ref)
      probeT2.expectMessage(PortesFermees)
    }
  }
}
