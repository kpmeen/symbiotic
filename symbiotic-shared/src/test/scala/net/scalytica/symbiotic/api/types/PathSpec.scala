package net.scalytica.symbiotic.api.types

import org.scalatest.{MustMatchers, WordSpec}

class PathSpec extends WordSpec with MustMatchers {

  "Path" should {
    "return the name of last the last path segment" in {
      val pv = Path("/root/foo/bar/fizz/")

      pv.nameOfLast mustBe "fizz"
    }

    "return the path to the parent node" in {
      val p        = Path("/root/foo/bar/fizz")
      val expected = Path("/root/foo/bar")

      p.parent mustBe expected
    }
  }

  "A collection of Paths" should {
    "compose to a hierarchy of distinct PathNodes" in {
      val fids = Seq(
        FileId.create(),
        FileId.create(),
        FileId.create(),
        FileId.create(),
        FileId.create(),
        FileId.create(),
        FileId.create()
      )

      val paths = Seq(
        (fids.head, Path("/root")),
        (fids(1), Path("/root/a")),
        (fids(2), Path("/root/a/foo")),
        (fids(3), Path("/root/a/foo/bar")),
        (fids(4), Path("/root/b")),
        (fids(5), Path("/root/b/fizz")),
        (fids.last, Path("/root/b/buzz"))
      )

      val expected = PathNode(
        fids.head,
        "root",
        Path("/root"),
        Seq(
          PathNode(
            fids(1),
            "a",
            Path("/root/a"),
            Seq(
              PathNode(
                fids(2),
                "foo",
                Path("/root/a/foo"),
                Seq(
                  PathNode(fids(3), "bar", Path("/root/a/foo/bar"))
                )
              )
            )
          ),
          PathNode(
            fids(4),
            "b",
            Path("/root/b"),
            Seq(
              PathNode(fids(5), "fizz", Path("/root/b/fizz")),
              PathNode(fids.last, "buzz", Path("/root/b/buzz"))
            )
          )
        )
      )

      val result: PathNode = PathNode.fromPaths(paths)
      result mustBe expected
    }
  }

}
