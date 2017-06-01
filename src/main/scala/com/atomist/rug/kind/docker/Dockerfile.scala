package com.atomist.rug.kind.docker

import java.util.concurrent.atomic.AtomicInteger
import com.atomist.util.Utils.StringImprovements

class Dockerfile(val lines: Seq[DockerfileLine]) {

  def this() = this(Seq())

  var _lines = lines

  object Command extends Enumeration(initial = 0) {
    type Command = Value
    val COMMENT, FROM, MAINTAINER, LABEL, RUN, ADD, COPY, ARG, ENV, EXPOSE, ONBUILD, STOPSIGNAL, USER, VOLUME, WORKDIR, ENTRYPOINT, CMD, HEALTHCHECK = Value
  }

  import Command._

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

  def addOrUpdateFrom(arg: String): this.type = {
    addOrUpdate(FROM, arg)
    this
  }

  def addOrUpdateExpose(arg: String): this.type = {
    addOrUpdate(EXPOSE, arg)
    this
  }

  def addExpose(arg: String): this.type = {
    add(EXPOSE, arg)
    this
  }

  def addOrUpdateMaintainer(arg: String): this.type = {
    addOrUpdate(MAINTAINER, arg)
    this
  }

  def addMaintainer(arg: String): this.type = {
    add(MAINTAINER, arg)
    this
  }

  def addOrUpdateLabel(arg: String*): this.type = {
    addOrUpdate(LABEL, arg.mkString(" "))
    this
  }

  def addLabel(arg: String*): this.type = {
    add(LABEL, arg.mkString(" "))
    this
  }

  def addRun(arg: String): this.type = {
    add(RUN, arg)
    this
  }

  def addCopy(arg: String): this.type = {
    add(COPY, arg)
    this
  }

  def addAdd(arg: String): this.type = {
    add(ADD, arg)
    this
  }

  def addEnv(arg: String): this.type = {
    add(ENV, arg)
    this
  }

  def addVolume(arg: String): this.type = {
    add(VOLUME, arg)
    this
  }

  def addOrUpdateWorkdir(arg: String): this.type = {
    addOrUpdate(WORKDIR, arg)
    this
  }

  def addOrUpdateEntryPoint(arg: String): this.type = {
    addOrUpdate(ENTRYPOINT, arg)
    this
  }

  def addOrUpdateCmd(arg: String): this.type = {
    addOrUpdate(CMD, arg)
    this
  }

  def addOrUpdateHealthcheck(arg: String): this.type = {
    addOrUpdate(HEALTHCHECK, arg)
    this
  }

  override def toString = {
    val builder = new StringBuilder
    val ai = new AtomicInteger(1)
    _lines.foreach(d => {
      val lb = "\n".r.findAllMatchIn(d.getRaw).length
      while (ai.get() < d.getLineno - lb) {
        builder.append("\n")
        ai.incrementAndGet()
      }
      for (i <- 0 until lb) {
        ai.incrementAndGet()
      }
      builder.append(d.getRaw)
    })
    builder.toString.toSystem
  }

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
