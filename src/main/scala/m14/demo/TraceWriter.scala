package m14.demo

import m14.petri.PetriNet
import m14.petri.PetriNet.{Marking, Transition}

import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets

object TraceWriter {

  final case class MessageAkka(de: String, vers: String, libelle: String)

  final case class Etape(
    label: String,
    akka: Option[MessageAkka],
    transition: Option[String],
    marquage: Marking,
    violation: Boolean
  )

  final case class Trace(id: String, titre: String, description: String, etapes: List[Etape])

  final class Constructeur(id: String, titre: String, description: String) {
    private var marquageCourant: Marking = PetriNet.marquageInitial
    private val etapes = scala.collection.mutable.ListBuffer.empty[Etape]

    etapes += Etape(
      label = "Etat initial M0",
      akka = None,
      transition = None,
      marquage = marquageCourant,
      violation = false
    )

    def tirer(label: String, akka: Option[MessageAkka], transition: Transition): this.type = {
      PetriNet.tirer(transition, marquageCourant) match {
        case Some(nouveau) =>
          marquageCourant = nouveau
          etapes += Etape(label, akka, Some(transition.nom), marquageCourant, violation = false)
        case None =>
          etapes += Etape(label, akka, Some(transition.nom), marquageCourant, violation = true)
      }
      this
    }

    def message(label: String, akka: MessageAkka): this.type = {
      etapes += Etape(label, Some(akka), None, marquageCourant, violation = false)
      this
    }

    def violation(label: String, akka: Option[MessageAkka], transition: Option[Transition]): this.type = {
      etapes += Etape(label, akka, transition.map(_.nom), marquageCourant, violation = true)
      this
    }

    def construire(): Trace = Trace(id, titre, description, etapes.toList)
  }

  private def echapper(s: String): String =
    s.flatMap {
      case '"'  => "\\\""
      case '\\' => "\\\\"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case c    => c.toString
    }

  private def chaine(s: String): String = "\"" + echapper(s) + "\""

  // json fait a la main, flemme d'ajouter une lib juste pour ca.
  private def jsonMessage(m: MessageAkka): String =
    s"""{"de": ${chaine(m.de)}, "vers": ${chaine(m.vers)}, "libelle": ${chaine(m.libelle)}}"""

  private def jsonMarquage(m: Marking): String = {
    val ordre = List(
      PetriNet.CantonLibre, PetriNet.QuaiLibre, PetriNet.PortesFermees, PetriNet.PortesOuvertes,
      PetriNet.T1Hors, PetriNet.T1Attente, PetriNet.T1SurCanton, PetriNet.T1AQuai,
      PetriNet.T2Hors, PetriNet.T2Attente, PetriNet.T2SurCanton, PetriNet.T2AQuai
    )
    ordre.map(p => s"${chaine(p)}: ${m.getOrElse(p, 0)}").mkString("{", ", ", "}")
  }

  private def jsonEtape(e: Etape): String = {
    val champs = List(
      Some(s""""label": ${chaine(e.label)}"""),
      e.akka.map(a => s""""akka": ${jsonMessage(a)}"""),
      e.transition.map(t => s""""transition": ${chaine(t)}"""),
      Some(s""""marquage": ${jsonMarquage(e.marquage)}"""),
      Some(s""""violation": ${e.violation}""")
    ).flatten
    champs.map("      " + _).mkString("{\n", ",\n", "\n    }")
  }

  def jsonTrace(t: Trace): String = {
    val etapesJson = t.etapes.map(jsonEtape).mkString("[\n    ", ",\n    ", "\n  ]")
    s"""{
  "id": ${chaine(t.id)},
  "titre": ${chaine(t.titre)},
  "description": ${chaine(t.description)},
  "etapes": $etapesJson
}
"""
  }

  def ecrire(trace: Trace, chemin: String): Unit = {
    val path = Paths.get(chemin)
    Option(path.getParent).foreach(Files.createDirectories(_))
    Files.write(path, jsonTrace(trace).getBytes(StandardCharsets.UTF_8))
  }
}
