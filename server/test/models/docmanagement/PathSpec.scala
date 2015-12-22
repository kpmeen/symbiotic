/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package models.docmanagement

import org.specs2.mutable.Specification
import play.api.libs.json.Json

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

    "compose to a hierarchy in full scale" in {
      val paths = Seq(
        Path("/root/"),
        Path("/root/4Dar0xsnKH-1/"),
        Path("/root/4Dar0xsnKH-1/4Dar0xsnKH-2/"),
        Path("/root/4Dar0xsnKH-1/4Dar0xsnKH-2/4Dar0xsnKH-3/"),
        Path("/root/4Dar0xsnKH-1/4Dar0xsnKH-2/4Dar0xsnKH-3/4Dar0xsnKH-4/"),
        Path("/root/4Dar0xsnKH-1/4Dar0xsnKH-2/4Dar0xsnKH-3/4Dar0xsnKH-4/4Dar0xsnKH-5/"),
        Path("/root/9njpiYgqlV-1/"),
        Path("/root/Gt6sZ5UsZu-1/"),
        Path("/root/IEzPl9VirX-1/"),
        Path("/root/IEzPl9VirX-1/IEzPl9VirX-2/"),
        Path("/root/IVvIdXVLOP-1/")
      //        ,
      //        Path("/root/IVvIdXVLOP-1/IVvIdXVLOP-2/"),
      //        Path("/root/IVvIdXVLOP-1/IVvIdXVLOP-2/IVvIdXVLOP-3/"),
      //        Path("/root/IVvIdXVLOP-1/IVvIdXVLOP-2/IVvIdXVLOP-3/IVvIdXVLOP-4/"),
      //        Path("/root/IVvIdXVLOP-1/IVvIdXVLOP-2/IVvIdXVLOP-3/IVvIdXVLOP-4/IVvIdXVLOP-5/"),
      //        Path("/root/IVvIdXVLOP-1/IVvIdXVLOP-2/IVvIdXVLOP-3/IVvIdXVLOP-4/IVvIdXVLOP-5/IVvIdXVLOP-6/"),
      //        Path("/root/Q5U9Ys06t0-1/"),
      //        Path("/root/Q5U9Ys06t0-1/Q5U9Ys06t0-2/"),
      //        Path("/root/Q5U9Ys06t0-1/Q5U9Ys06t0-2/Q5U9Ys06t0-3/"),
      //        Path("/root/Q5U9Ys06t0-1/Q5U9Ys06t0-2/Q5U9Ys06t0-3/Q5U9Ys06t0-4/"),
      //        Path("/root/Q5U9Ys06t0-1/Q5U9Ys06t0-2/Q5U9Ys06t0-3/Q5U9Ys06t0-4/Q5U9Ys06t0-5/"),
      //        Path("/root/WTaHMidloi-1/"),
      //        Path("/root/WTaHMidloi-1/WTaHMidloi-2/"),
      //        Path("/root/WTaHMidloi-1/WTaHMidloi-2/WTaHMidloi-3/"),
      //        Path("/root/WTaHMidloi-1/WTaHMidloi-2/WTaHMidloi-3/WTaHMidloi-4/"),
      //        Path("/root/WTaHMidloi-1/WTaHMidloi-2/WTaHMidloi-3/WTaHMidloi-4/WTaHMidloi-5/"),
      //        Path("/root/WTaHMidloi-1/WTaHMidloi-2/WTaHMidloi-3/WTaHMidloi-4/WTaHMidloi-5/WTaHMidloi-6/"),
      //        Path("/root/Yl98ylvJwe-1/"),
      //        Path("/root/Yl98ylvJwe-1/Yl98ylvJwe-2/"),
      //        Path("/root/Yl98ylvJwe-1/Yl98ylvJwe-2/Yl98ylvJwe-3/"),
      //        Path("/root/Yl98ylvJwe-1/Yl98ylvJwe-2/Yl98ylvJwe-3/Yl98ylvJwe-4/"),
      //        Path("/root/Yl98ylvJwe-1/Yl98ylvJwe-2/Yl98ylvJwe-3/Yl98ylvJwe-4/Yl98ylvJwe-5/"),
      //        Path("/root/Yl98ylvJwe-1/Yl98ylvJwe-2/Yl98ylvJwe-3/Yl98ylvJwe-4/Yl98ylvJwe-5/Yl98ylvJwe-6/"),
      //        Path("/root/Yl98ylvJwe-1/Yl98ylvJwe-2/Yl98ylvJwe-3/Yl98ylvJwe-4/Yl98ylvJwe-5/Yl98ylvJwe-6/Yl98ylvJwe-7/"),
      //        Path("/root/Yl98ylvJwe-1/Yl98ylvJwe-2/Yl98ylvJwe-3/Yl98ylvJwe-4/Yl98ylvJwe-5/Yl98ylvJwe-6/Yl98ylvJwe-7/Yl98ylvJwe-8/"),
      //        Path("/root/Yl98ylvJwe-1/Yl98ylvJwe-2/Yl98ylvJwe-3/Yl98ylvJwe-4/Yl98ylvJwe-5/Yl98ylvJwe-6/Yl98ylvJwe-7/Yl98ylvJwe-8/Yl98ylvJwe-9/"),
      //        Path("/root/Yl98ylvJwe-1/Yl98ylvJwe-2/Yl98ylvJwe-3/Yl98ylvJwe-4/Yl98ylvJwe-5/Yl98ylvJwe-6/Yl98ylvJwe-7/Yl98ylvJwe-8/Yl98ylvJwe-9/Yl98ylvJwe-10/"),
      //        Path("/root/cK2WzAFMR8-1/"),
      //        Path("/root/cK2WzAFMR8-1/cK2WzAFMR8-2/"),
      //        Path("/root/cK2WzAFMR8-1/cK2WzAFMR8-2/cK2WzAFMR8-3/"),
      //        Path("/root/cK2WzAFMR8-1/cK2WzAFMR8-2/cK2WzAFMR8-3/cK2WzAFMR8-4/"),
      //        Path("/root/cK2WzAFMR8-1/cK2WzAFMR8-2/cK2WzAFMR8-3/cK2WzAFMR8-4/cK2WzAFMR8-5/"),
      //        Path("/root/cK2WzAFMR8-1/cK2WzAFMR8-2/cK2WzAFMR8-3/cK2WzAFMR8-4/cK2WzAFMR8-5/cK2WzAFMR8-6/"),
      //        Path("/root/cK2WzAFMR8-1/cK2WzAFMR8-2/cK2WzAFMR8-3/cK2WzAFMR8-4/cK2WzAFMR8-5/cK2WzAFMR8-6/cK2WzAFMR8-7/"),
      //        Path("/root/cK2WzAFMR8-1/cK2WzAFMR8-2/cK2WzAFMR8-3/cK2WzAFMR8-4/cK2WzAFMR8-5/cK2WzAFMR8-6/cK2WzAFMR8-7/cK2WzAFMR8-8/"),
      //        Path("/root/f6aSTNeiFj-1/"),
      //        Path("/root/f6aSTNeiFj-1/f6aSTNeiFj-2/"),
      //        Path("/root/f6aSTNeiFj-1/f6aSTNeiFj-2/f6aSTNeiFj-3/"),
      //        Path("/root/jjXUfPyZnH-1/"),
      //        Path("/root/jjXUfPyZnH-1/jjXUfPyZnH-2/"),
      //        Path("/root/jjXUfPyZnH-1/jjXUfPyZnH-2/jjXUfPyZnH-3/"),
      //        Path("/root/jjXUfPyZnH-1/jjXUfPyZnH-2/jjXUfPyZnH-3/jjXUfPyZnH-4/"),
      //        Path("/root/jjXUfPyZnH-1/jjXUfPyZnH-2/jjXUfPyZnH-3/jjXUfPyZnH-4/jjXUfPyZnH-5/"),
      //        Path("/root/jjXUfPyZnH-1/jjXUfPyZnH-2/jjXUfPyZnH-3/jjXUfPyZnH-4/jjXUfPyZnH-5/jjXUfPyZnH-6/"),
      //        Path("/root/jjXUfPyZnH-1/jjXUfPyZnH-2/jjXUfPyZnH-3/jjXUfPyZnH-4/jjXUfPyZnH-5/jjXUfPyZnH-6/jjXUfPyZnH-7/"),
      //        Path("/root/jjXUfPyZnH-1/jjXUfPyZnH-2/jjXUfPyZnH-3/jjXUfPyZnH-4/jjXUfPyZnH-5/jjXUfPyZnH-6/jjXUfPyZnH-7/jjXUfPyZnH-8/"),
      //        Path("/root/l4UmTQivWT-1/"),
      //        Path("/root/l4UmTQivWT-1/l4UmTQivWT-2/"),
      //        Path("/root/l4UmTQivWT-1/l4UmTQivWT-2/l4UmTQivWT-3/"),
      //        Path("/root/obHuI3jy8b-1/"),
      //        Path("/root/obHuI3jy8b-1/FooBar/"),
      //        Path("/root/obHuI3jy8b-1/obHuI3jy8b-2/"),
      //        Path("/root/obHuI3jy8b-1/obHuI3jy8b-2/obHuI3jy8b-3/"),
      //        Path("/root/obHuI3jy8b-1/obHuI3jy8b-2/obHuI3jy8b-3/obHuI3jy8b-4/"),
      //        Path("/root/obHuI3jy8b-1/obHuI3jy8b-2/obHuI3jy8b-3/obHuI3jy8b-4/obHuI3jy8b-5/"),
      //        Path("/root/obHuI3jy8b-1/obHuI3jy8b-2/obHuI3jy8b-3/obHuI3jy8b-4/obHuI3jy8b-5/obHuI3jy8b-6/"),
      //        Path("/root/obHuI3jy8b-1/obHuI3jy8b-2/obHuI3jy8b-3/obHuI3jy8b-4/obHuI3jy8b-5/obHuI3jy8b-6/obHuI3jy8b-7/"),
      //        Path("/root/obHuI3jy8b-1/uugg/"),
      //        Path("/root/vqO57xuoBF-1/"),
      //        Path("/root/vqO57xuoBF-1/vqO57xuoBF-2/"),
      //        Path("/root/vqO57xuoBF-1/vqO57xuoBF-2/vqO57xuoBF-3/"),
      //        Path("/root/vqO57xuoBF-1/vqO57xuoBF-2/vqO57xuoBF-3/vqO57xuoBF-4/"),
      //        Path("/root/vqO57xuoBF-1/vqO57xuoBF-2/vqO57xuoBF-3/vqO57xuoBF-4/vqO57xuoBF-5/"),
      //        Path("/root/vqO57xuoBF-1/vqO57xuoBF-2/vqO57xuoBF-3/vqO57xuoBF-4/vqO57xuoBF-5/vqO57xuoBF-6/"),
      //        Path("/root/vqO57xuoBF-1/vqO57xuoBF-2/vqO57xuoBF-3/vqO57xuoBF-4/vqO57xuoBF-5/vqO57xuoBF-6/vqO57xuoBF-7/"),
      //        Path("/root/vqO57xuoBF-1/vqO57xuoBF-2/vqO57xuoBF-3/vqO57xuoBF-4/vqO57xuoBF-5/vqO57xuoBF-6/vqO57xuoBF-7/vqO57xuoBF-8/"),
      //        Path("/root/vqO57xuoBF-1/vqO57xuoBF-2/vqO57xuoBF-3/vqO57xuoBF-4/vqO57xuoBF-5/vqO57xuoBF-6/vqO57xuoBF-7/vqO57xuoBF-8/vqO57xuoBF-9/"),
      //        Path("/root/vqO57xuoBF-1/vqO57xuoBF-2/vqO57xuoBF-3/vqO57xuoBF-4/vqO57xuoBF-5/vqO57xuoBF-6/vqO57xuoBF-7/vqO57xuoBF-8/vqO57xuoBF-9/vqO57xuoBF-10/"),
      //        Path("/root/w0T5beAxo4-1/"),
      //        Path("/root/w0T5beAxo4-1/w0T5beAxo4-2/"),
      //        Path("/root/w0T5beAxo4-1/w0T5beAxo4-2/w0T5beAxo4-3/"),
      //        Path("/root/w0T5beAxo4-1/w0T5beAxo4-2/w0T5beAxo4-3/w0T5beAxo4-4/"),
      //        Path("/root/w0T5beAxo4-1/w0T5beAxo4-2/w0T5beAxo4-3/w0T5beAxo4-4/w0T5beAxo4-5/"),
      //        Path("/root/w0T5beAxo4-1/w0T5beAxo4-2/w0T5beAxo4-3/w0T5beAxo4-4/w0T5beAxo4-5/w0T5beAxo4-6/"),
      //        Path("/root/w0T5beAxo4-1/w0T5beAxo4-2/w0T5beAxo4-3/w0T5beAxo4-4/w0T5beAxo4-5/w0T5beAxo4-6/w0T5beAxo4-7/"),
      //        Path("/root/xOPosstBnE-1/"),
      //        Path("/root/xOPosstBnE-1/xOPosstBnE-2/")
      )

      val result: PathNode = PathNode.fromPaths(paths)

      println(Json.prettyPrint(Json.toJson(result)))

      todo
    }
  }

}
