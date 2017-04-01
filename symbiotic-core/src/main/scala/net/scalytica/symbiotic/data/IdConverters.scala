package net.scalytica.symbiotic.data

trait IdConverters[A <: Id] {

  implicit def asId(s: String): A

  implicit def asOptId(maybeId: Option[String]): Option[A] =
    maybeId.map(s => asId(s))

  implicit def asOptId(s: String): Option[A] = asOptId(Option(s))

  def create(): A = asId(java.util.UUID.randomUUID.toString)

  def createOpt(): Option[A] = asOptId(java.util.UUID.randomUUID.toString)

}
