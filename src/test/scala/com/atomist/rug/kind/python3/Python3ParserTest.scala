package com.atomist.rug.kind.python3

object Python3ParserTest {

  val simplestDotPy =
    """import os
      |import sys
    """.stripMargin

  val simplestDotPyWithInitialNewLine =
    """
      |import os
      |import sys
    """.stripMargin

  val setupDotPy =
    """
      |import os
      |import sys
      |try:
      |    from setuptools import setup
      |except ImportError:
      |    from distutils.core import setup
      |
      |sys.path.insert(0, '.')
      |from botletpy import __version__
      |
      |
      |setup(
      |    name="botletpy",
      |    version=__version__,
      |    description="Library serving as a brick to build botlets in Python.",
      |    maintainer="Atomist",
      |    maintainer_email="sylvain@atomist.com",
      |    packages=["botletpy"],
      |    platforms=["any"],
      |    long_description=open(os.path.join(os.path.dirname(__file__), 'README.md')).read()
      |)
    """.stripMargin

  val dateParserDotPy =
    """
      |from datetime import datetime
      |
      |now = datetime.now()
      |
      |mm = str(now.month)
      |
      |dd = str(now.day)
      |
      |yyyy = str(now.year)
      |
      |hour = str(now.hour)
      |
      |mi = str(now.minute)
      |
      |ss = str(now.second)
      |
      |print mm + "/" + dd + "/" + yyyy + " " + hour + ":" + mi + ":" + ss
      |
      """.stripMargin

  val flask1 =
    """
      |from flask import Flask
      |app = Flask(__name__)
      |
      |@app.route("/")
      |def hello():
      |    return "Hello World!"
      |
      |if __name__ == "__main__":
      |    app.run()
    """.stripMargin
}
