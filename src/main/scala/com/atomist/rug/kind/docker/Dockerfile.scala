package com.atomist.rug.kind.docker

import java.util.concurrent.atomic.AtomicInteger
import com.atomist.util.Utils.StringImprovements

class Dockerfile(val lines: Seq[DockerfileLine]) {

  def this() = this(Seq())

  var _lines: Seq[DockerfileLine] = lines

  object Command extends Enumeration(initial = 0) {
    type Command = Value
    val COMMENT, FROM, MAINTAINER, LABEL, RUN, ADD, COPY, ARG, ENV, EXPOSE, ONBUILD, STOPSIGNAL, USER, VOLUME, WORKDIR, ENTRYPOINT, CMD, HEALTHCHECK = Value
  }

  import Command._

  def getFrom: Option[String] = getLastString(FROM)

  def addOrUpdateFrom(arg: String): this.type = {
    addOrUpdate(FROM, arg)
    this
  }

  def getExposePorts: Set[Int] = {
    val matchingLines: Seq[DockerfileLine] = lines.filter(d => EXPOSE.toString.equals(d.name))
    val exposeLineToPorts: (DockerfileLine) => Set[Int] = l => {
      // This could be a single value, or a list of values. First trim off any extra whitespace
      val exposeValue: String = l.getRaw.stripPrefix(EXPOSE.toString).trim
      val portValues: Array[String] = exposeValue.split(' ')
      // A bit hacky, but need to skip entries which are whitespace (the split above leaves in whitespace lines)
      portValues.filter(v => v.trim.length > 0).map(v => Integer.parseInt(v)).toSet
    }

    matchingLines.flatMap(exposeLineToPorts).toSet
  }

  def addOrUpdateExpose(arg: String): this.type = {
    addOrUpdate(EXPOSE, arg)
    this
  }

  def addExpose(arg: String): this.type = {
    add(EXPOSE, arg)
    this
  }

  def getMaintainer: Option[String] = getLastString(MAINTAINER)

  def addOrUpdateMaintainer(arg: String): this.type = {
    addOrUpdate(MAINTAINER, arg)
    this
  }

  def addMaintainer(arg: String): this.type = {
    add(MAINTAINER, arg)
    this
  }

  def getLabels: Map[String, String] = getAllMap(LABEL)

  def addOrUpdateLabel(arg: String*): this.type = {
    addOrUpdate(LABEL, arg.mkString(" "))
    this
  }

  def addLabel(arg: String*): this.type = {
    add(LABEL, arg.mkString(" "))
    this
  }

  def getRuns: Seq[Either[String, Seq[String]]] = getAllStringOrArray(RUN)

  def addRun(arg: String): this.type = {
    add(RUN, arg)
    this
  }

  def getCopies: Seq[Seq[String]] = getAllArray(COPY)

  def addCopy(arg: String): this.type = {
    add(COPY, arg)
    this
  }

  def getAdds: Seq[Seq[String]] = getAllArray(ADD)

  def addAdd(arg: String): this.type = {
    add(ADD, arg)
    this
  }

  def getEnvs = getAllMap(ENV)

  def addEnv(arg: String): this.type = {
    add(ENV, arg)
    this
  }

  def getVolumes: Seq[Seq[String]] = getAllArray(VOLUME)

  def addVolume(arg: String): this.type = {
    add(VOLUME, arg)
    this
  }

  def getWorkdir: Option[String] = getLastString(WORKDIR)

  def addOrUpdateWorkdir(arg: String): this.type = {
    addOrUpdate(WORKDIR, arg)
    this
  }

  def getEntryPoint: Either[String, Seq[String]] = getLastStringOrArray(ENTRYPOINT)

  def addOrUpdateEntryPoint(arg: String): this.type = {
    addOrUpdate(ENTRYPOINT, arg)
    this
  }

  def getCmd: Either[String, Seq[String]] = getLastStringOrArray(CMD)

  def addOrUpdateCmd(arg: String): this.type = {
    addOrUpdate(CMD, arg)
    this
  }

  def getHealthCheck: Option[String] = getLastString(HEALTHCHECK)

  def addOrUpdateHealthcheck(arg: String): this.type = {
    addOrUpdate(HEALTHCHECK, arg)
    this
  }

  override def toString: String = {
    val builder = new StringBuilder
    val ai = new AtomicInteger(1)
    _lines.foreach(d => {
      val lb = "\n".r.findAllMatchIn(d.getRaw).length
      while (ai.get() < d.getLineno - lb) {
        builder.append("\n")
        ai.incrementAndGet()
      }
      for (_ <- 0 until lb) {
        ai.incrementAndGet()
      }
      builder.append(d.getRaw)
    })
    builder.toString.toSystem
  }

  /**
    * Return the value of the last occurrence of Dockerfile command `c`.
    *
    * @param c Dockerfile command
    * @return
    */
  private def getLastString(c: Command): Option[String] =
    lines.reverse.find(d => c.toString.equals(d.name)) match {
      case Some(d: DockerfileLine) => d.getArgs match {
        case s: String => Some(s)
        case _ => None
      }
      case None => None
    }

  /**
    * Return a sequence of the array values of '''all'''
    * occurrences of the Dockerfile command `c.`
    *
    * @param c Dockerfile command
    * @return
    */
  private def getAllArray(c: Command): Seq[Seq[String]] =
    lines.filter(d => c.toString.equals(d.name)).map(dl => dl.getArgs match {
      case m: Map[String @unchecked, String @unchecked] =>
        m.asInstanceOf[Map[String, String]].toSeq.sortBy(p => Integer.parseInt(p._1)).map(_._2)
      case _ => Seq.empty[String]
    })

  /**
    * Return the value of the last occurrence of the Dockerfile command
    * `c` which could be either a string or array of strings.  Examples of
    * such commands are `CMD` and `ENTRYPOINT`.  If the command does not
    * appear in the Dockerfile, `Right(Seq.empty)` is returned.
    *
    * @param c Dockerfile command
    * @return
    */
  private def getLastStringOrArray(c: Command): Either[String, Seq[String]] =
    lines.reverse.find(d => c.toString.equals(d.name)) match {
      case Some(d: DockerfileLine) => d.getArgs match {
        case s: String => Left(s)
        case m: Map[String @unchecked, String @unchecked] =>
          Right(m.asInstanceOf[Map[String, String]].toSeq.sortBy(p => Integer.parseInt(p._1)).map(_._2))
      }
      case None => Right(Seq.empty[String])
    }

  /**
    * Return the value of the all occurrences of the Dockerfile command
    * `c`, each of which could be either a string or array of strings.  Examples of
    * such commands are `RUN` and `VOLUME`.  If the command does not
    * appear in the Dockerfile, `(Seq.empty)` is returned.
    *
    * @param c Dockerfile command
    * @return
    */
  private def getAllStringOrArray(c: Command): Seq[Either[String, Seq[String]]] =
    lines.filter(d => c.toString.equals(d.name)).map(dl => dl.getArgs match {
      case s: String => Left(s)
      case m: Map[String @unchecked, String @unchecked] =>
        Right(m.asInstanceOf[Map[String, String]].toSeq.sortBy(p => Integer.parseInt(p._1)).map(_._2))
    })

  /**
    * Return a single map that combines the key/value pair values of '''all'''
    * occurrences of the Dockerfile command `c`.
    *
    * @param c Dockerfile command
    * @return
    */
  private def getAllMap(c: Command): Map[String, String] =
    lines.filter(d => c.toString.equals(d.name)).flatMap(dl => {
      dl.getArgs match {
        case m: Map[String @unchecked, String @unchecked] => m.asInstanceOf[Map[String, String]]
        case _ => Map.empty[String, String]
      }
    }).toMap

  private def add(cmd: Command, arg: Any) = {
    val line: DockerfileLine = create(cmd)
    setRaw(line, cmd, arg)
  }

  private def addOrUpdate(cmd: Command, arg: Any): Unit = {
    val line = addOrUpdate(cmd)
    setRaw(line, cmd, arg)
  }

  private def setRaw(line: DockerfileLine, cmd: Command, arg: Any) {
    line.setArgs(arg)
    line.setRaw(cmd.toString + " " + arg)
  }

  private def addOrUpdate(cmd: Command): DockerfileLine =
    _lines.find(d => cmd.toString.equals(d.name)).getOrElse({
      val line = new DockerfileLine(cmd.toString)
      mergeLines(line)
      line
    })

  private def create(cmd: Command): DockerfileLine = {
    val line = new DockerfileLine(cmd.toString)
    mergeLines(line)
    line
  }

  private def mergeLines(line: DockerfileLine): Unit = {
    var _line = line

    // First get the next element in the order
    val newCmd = Command.values.find(_.toString == line.name).head

    var nextCmdLine: DockerfileLine = null
    var ix: Int = -1
    for (i <- _lines.indices; if nextCmdLine == null) {
      val cmd = Command.values.find(_.toString == _lines(i).name).head
      if (newCmd.id <= cmd.id) {
        nextCmdLine = _lines(i)
        ix = i
      }
    }
    if (nextCmdLine != null) {
      // Insert before
      val lb = "\n".r.findAllMatchIn(nextCmdLine.getRaw).length
      _line.setLineno(nextCmdLine.getLineno - lb)
    } else if (_lines.nonEmpty)
      _line.setLineno(_lines.last.getLineno + 1)
    else
      _line.setLineno(1)

    if (ix >= 0) {
      _lines = insertAt(_line, ix, _lines.toList)
      // Update line numbers in later lines
      for (i <- (ix + 1) until _lines.size) {
        _lines(i).setLineno(_lines(i).getLineno + 1)
      }
    } else
      _lines :+= _line
  }

  private def insertAt[T](n: T, pos: Int, list: List[T]): List[T] =
    if (pos >= list.length) {
      list ::: List(n)
    } else {
      list.zipWithIndex.foldLeft(List[T]()) { (acc, tuple) =>
        tuple match {
          case (item, idx) =>
            if (idx == pos) acc ::: List(n, item) else acc ::: List(item)
        }
      }
    }
}

object Dockerfile {
  /**
    * Convert an Either of String or Seq[String] to a String.  The Seq[String]
    * is made to look like a Dockerfile array.
    *
    * @param es stringify target
    * @return
    */
  def stringOrArrayToString(es: Either[String, Seq[String]]): String = es match {
    case Left(s) => s
    case Right(ss) => "[ \"" + ss.mkString("\", \"") + "\" ]"
  }

  /**
    * Convert a sequence of Either[String, Seq[String] ] to a Seq[String] using
    * stringOrArrayToString.
    *
    * @param ess sequence of things to stringify
    * @return
    */
  def seqStringOrArrayToString(ess: Seq[Either[String, Seq[String]]]): Seq[String] =
    ess map stringOrArrayToString
}
