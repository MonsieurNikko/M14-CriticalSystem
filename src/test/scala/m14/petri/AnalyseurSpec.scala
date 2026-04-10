package m14.petri

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class AnalyseurSpec extends AnyWordSpec with Matchers {

  import PetriNet._
  import Analyseur._

  "PetriNet (extension PSD)" should {

    "avoir 12 places et 12 transitions" in {
      toutesLesPlaces.size shouldBe 12
      toutesLesTransitions.size shouldBe 12
    }

    "avoir un marquage initial avec 5 jetons" in {
      val totalJetons = marquageInitial.values.sum
      totalJetons shouldBe 5
    }

    "M0 contient les jetons attendus (Canton/Quai/Portes libres + 2 trains hors)" in {
      marquageInitial(CantonLibre)   shouldBe 1
      marquageInitial(QuaiLibre)     shouldBe 1
      marquageInitial(PortesFermees) shouldBe 1
      marquageInitial(T1Hors)        shouldBe 1
      marquageInitial(T2Hors)        shouldBe 1
    }

    "rendre T1_demande et T2_demande tirables depuis M0" in {
      estTirable(t1Demande, marquageInitial) shouldBe true
      estTirable(t2Demande, marquageInitial) shouldBe true
    }

    "ne pas rendre T1_entree_canton tirable depuis M0" in {
      estTirable(t1EntreeCanton, marquageInitial) shouldBe false
    }

    "ne pas rendre Ouverture_portes_T1 tirable depuis M0 (T1_a_quai = 0)" in {
      estTirable(ouvertureT1, marquageInitial) shouldBe false
    }

    "calculer correctement M0 --T1_demande--> M1" in {
      val m1 = tirer(t1Demande, marquageInitial).get
      m1(T1Hors)       shouldBe 0
      m1(T1Attente)    shouldBe 1
      m1(CantonLibre)  shouldBe 1
    }

    "calculer correctement la sequence T1_demande -> T1_entree_canton" in {
      val m1 = tirer(t1Demande, marquageInitial).get
      val m2 = tirer(t1EntreeCanton, m1).get
      m2(T1Attente)    shouldBe 0
      m2(T1SurCanton)  shouldBe 1
      m2(CantonLibre)  shouldBe 0
    }

    "calculer correctement T1 jusqu'a a_quai (cycle canton -> quai)" in {
      val m1 = tirer(t1Demande, marquageInitial).get
      val m2 = tirer(t1EntreeCanton, m1).get
      val m3 = tirer(t1ArriveeQuai, m2).get
      m3(T1SurCanton)  shouldBe 0
      m3(T1AQuai)      shouldBe 1
      m3(QuaiLibre)    shouldBe 0
      m3(CantonLibre)  shouldBe 1
    }
  }

  "Analyseur (extension PSD)" should {

    "trouver exactement 20 marquages atteignables" in {
      val resultat = analyser(reseauTroncon)
      resultat.nombreEtats shouldBe 20
    }

    "verifier l'invariant canton (5.1)" in {
      analyser(reseauTroncon).invariantCantonOk shouldBe true
    }

    "verifier l'invariant quai (5.2)" in {
      analyser(reseauTroncon).invariantQuaiOk shouldBe true
    }

    "verifier l'invariant portes (5.3)" in {
      analyser(reseauTroncon).invariantPortesOk shouldBe true
    }

    "verifier les invariants par train" in {
      analyser(reseauTroncon).invariantsParTrainOk shouldBe true
    }

    "verifier PSD-Open Safety (6.1) sur tous les marquages [CRITIQUE]" in {
      analyser(reseauTroncon).invariantPsdOpenOk shouldBe true
    }

    "verifier PSD-Departure Safety (6.2) sur tous les marquages [CRITIQUE]" in {
      analyser(reseauTroncon).invariantPsdDepartureOk shouldBe true
    }

    "ne detecter aucun deadlock" in {
      analyser(reseauTroncon).deadlocks shouldBe empty
    }

    "garantir exclusion mutuelle canton (jamais T1+T2 sur le canton)" in {
      val resultat = analyser(reseauTroncon)
      val collision = resultat.marquagesAtteignables.exists { m =>
        m.getOrElse(T1SurCanton, 0) >= 1 && m.getOrElse(T2SurCanton, 0) >= 1
      }
      collision shouldBe false
    }

    "garantir exclusion mutuelle quai (jamais T1+T2 a quai)" in {
      val resultat = analyser(reseauTroncon)
      val collision = resultat.marquagesAtteignables.exists { m =>
        m.getOrElse(T1AQuai, 0) >= 1 && m.getOrElse(T2AQuai, 0) >= 1
      }
      collision shouldBe false
    }

    "Portes_ouvertes implique toujours qu un train est a quai (PSD-Open instantiation)" in {
      val resultat = analyser(reseauTroncon)
      resultat.marquagesAtteignables.foreach { m =>
        if (m.getOrElse(PortesOuvertes, 0) >= 1) {
          val sommeAQuai = m.getOrElse(T1AQuai, 0) + m.getOrElse(T2AQuai, 0)
          sommeAQuai shouldBe 1
        }
      }
    }
  }

  "Graphe d'accessibilite (Phase 7)" should {

    "produire exactement 40 arcs etiquetes" in {
      analyser(reseauTroncon).arcs.size shouldBe 40
    }

    "construire des arcs dont source et cible sont des marquages atteignables" in {
      val resultat = analyser(reseauTroncon)
      val atteignables = resultat.marquagesAtteignables.toSet
      resultat.arcs.foreach { arc =>
        atteignables should contain(arc.source)
        atteignables should contain(arc.cible)
      }
    }

    "que chaque arc respecte la semantique de tirage Petri" in {
      val resultat = analyser(reseauTroncon)
      resultat.arcs.foreach { arc =>
        val cibleCalculee = tirer(arc.transition, arc.source)
        cibleCalculee shouldBe Some(arc.cible)
      }
    }
  }

  "Verification LTL programmatique (Phase 7)" should {

    "verifier G safety canton (jamais T1+T2 sur le canton)" in {
      val resultat = analyser(reseauTroncon)
      val r = verifierGSafety(resultat.marquagesAtteignables, m =>
        !(m.getOrElse(T1SurCanton, 0) >= 1 && m.getOrElse(T2SurCanton, 0) >= 1))
      r shouldBe Right(true)
    }

    "verifier G safety quai (jamais T1+T2 a quai)" in {
      val resultat = analyser(reseauTroncon)
      val r = verifierGSafety(resultat.marquagesAtteignables, m =>
        !(m.getOrElse(T1AQuai, 0) >= 1 && m.getOrElse(T2AQuai, 0) >= 1))
      r shouldBe Right(true)
    }

    "verifier G safety PSD-Open via verifierGSafety" in {
      val resultat = analyser(reseauTroncon)
      val r = verifierGSafety(resultat.marquagesAtteignables, verifierSurteOuverturePortes)
      r shouldBe Right(true)
    }

    "fournir un contre-exemple si Safety est violee" in {
      val resultat = analyser(reseauTroncon)
      // petit test faux expres, pour verifier qu'on recupere bien un contre-exemple.
      val r = verifierGSafety(resultat.marquagesAtteignables, m => m.getOrElse(CantonLibre, 0) == 0)
      r.isLeft shouldBe true
    }

    "verifier G F liveness canton T1 (T1_attente -> F T1_sur_canton)" in {
      val resultat = analyser(reseauTroncon)
      // pas un vrai gros model checker, juste le graphe fini du projet.
      val r = verifierGFLiveness(resultat.marquagesAtteignables, resultat.arcs,
        source = m => m.getOrElse(T1Attente, 0) >= 1,
        cible  = m => m.getOrElse(T1SurCanton, 0) >= 1)
      r shouldBe Right(true)
    }

    "verifier G F liveness canton T2 (T2_attente -> F T2_sur_canton)" in {
      val resultat = analyser(reseauTroncon)
      val r = verifierGFLiveness(resultat.marquagesAtteignables, resultat.arcs,
        source = m => m.getOrElse(T2Attente, 0) >= 1,
        cible  = m => m.getOrElse(T2SurCanton, 0) >= 1)
      r shouldBe Right(true)
    }

    "verifier G F liveness PSD T1 (T1_a_quai -> F Portes_ouvertes)" in {
      val resultat = analyser(reseauTroncon)
      val r = verifierGFLiveness(resultat.marquagesAtteignables, resultat.arcs,
        source = m => m.getOrElse(T1AQuai, 0) >= 1,
        cible  = m => m.getOrElse(PortesOuvertes, 0) >= 1)
      r shouldBe Right(true)
    }
  }
}
