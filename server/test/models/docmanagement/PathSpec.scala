/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import org.specs2.mutable.Specification

class PathSpec extends Specification {

  "Path" should {
    "return the name of last the last path segment" in {
      val pv = Path("/root/foo/bar/fizz/")

      pv.nameOfLast must_== "fizz"
    }

    "return the path to the parent node" in {
      val p = Path("/root/foo/bar/fizz")
      val expected = Path("/root/foo/bar")

      p.parentPath must_== expected
    }
  }

  "A collection of Paths" should {
    "compose to a hierarchy of distinct PathNodes" in {
      val paths = Seq(
        Path("/root"),
        Path("/root/a"),
        Path("/root/a/foo"),
        Path("/root/a/foo/bar"),
        Path("/root/b"),
        Path("/root/b/fizz"),
        Path("/root/b/buzz")
      )

      val expected = PathNode("root", Path("/root"), Seq(
        PathNode("a", Path("/root/a"), Seq(
          PathNode("foo", Path("/root/a/foo"), Seq(
            PathNode("bar", Path("/root/a/foo/bar"))
          ))
        )),
        PathNode("b", Path("/root/b"), Seq(
          PathNode("fizz", Path("/root/b/fizz")),
          PathNode("buzz", Path("/root/b/buzz"))
        ))
      ))

      val result: PathNode = PathNode.fromPaths(paths)
      result must_== expected
    }
  }

}
