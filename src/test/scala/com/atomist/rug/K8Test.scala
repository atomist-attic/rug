package com.atomist.rug

import com.atomist.param.SimpleParameterValues
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.rug.ts.{RugTranspiler, TypeScriptBuilder}
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class K8Test extends FlatSpec with Matchers {

  val spec =
    """
      |{
      |  "kind": "Deployment",
      |  "apiVersion": "extensions/v1beta1",
      |  "metadata": {
      |    "name": "project-operation",
      |    "namespace": "default",
      |    "labels": {
      |      "service": "project-operation"
      |    }
      |  },
      |  "spec": {
      |    "replicas": 1,
      |    "selector": {
      |      "matchLabels": {
      |        "service": "project-operation"
      |      }
      |    },
      |    "template": {
      |      "metadata": {
      |        "labels": {
      |          "service": "project-operation"
      |        },
      |        "annotations": {
      |          "atomist.config": "{secret/artifactory read secret/templates read secret/teams/* read}",
      |          "atomist.version" : "0.0.1"
      |        }
      |      },
      |      "spec": {
      |        "containers": [
      |          {
      |            "name": "project-operation",
      |            "image": "sforzando-docker-dockerv2-local.artifactoryonline.com/project-operation:6a6916a",
      |            "ports": [
      |              {
      |                "name": "http",
      |                "containerPort": 8080,
      |                "protocol": "TCP"
      |              }
      |            ],
      |            "resources": {
      |              "limits": {
      |                "cpu": 0.5,
      |                "memory": "1200Mi"
      |              },
      |              "requests": {
      |                "cpu": 0.1,
      |                "memory": "1200Mi"
      |              }
      |            },
      |            "livenessProbe": {
      |              "httpGet": {
      |                "path": "/health",
      |                "port": 8080,
      |                "scheme": "HTTP"
      |              },
      |              "initialDelaySeconds": 30,
      |              "timeoutSeconds": 3,
      |              "periodSeconds": 10,
      |              "successThreshold": 1,
      |              "failureThreshold": 3
      |            },
      |            "readinessProbe": {
      |              "httpGet": {
      |                "path": "/health",
      |                "port": 8080,
      |                "scheme": "HTTP"
      |              },
      |              "initialDelaySeconds": 30,
      |              "timeoutSeconds": 3,
      |              "periodSeconds": 10,
      |              "successThreshold": 1,
      |              "failureThreshold": 3
      |            },
      |            "terminationMessagePath": "/dev/termination-log",
      |            "imagePullPolicy": "Always"
      |          }
      |        ],
      |        "restartPolicy": "Always",
      |        "terminationGracePeriodSeconds": 30,
      |        "dnsPolicy": "ClusterFirst",
      |        "securityContext": {},
      |        "imagePullSecrets": [
      |          {
      |            "name": "atomistregistrykey"
      |          }
      |        ]
      |      }
      |    },
      |    "strategy": {
      |      "type": "RollingUpdate",
      |      "rollingUpdate": {
      |        "maxUnavailable": 0,
      |        "maxSurge": 1
      |      }
      |    }
      |  }
      |}
    """.stripMargin

  import TestUtils._

  it should "update K8 spec without JavaScript" in {
    val prog =
      """
        |@description "Update Kube spec to redeploy a service"
        |editor Redeploy
        |
        |param service: ^[\w.\-_]+$
        |param new_sha: ^[a-f0-9]{7}$
        |
        |let regexp = ":[a-f0-9]{7}"
        |
        |with File f
        | when { f.name().indexOf("80-" + service + "-deployment") >= 0 };
        |do
        |  regexpReplace regexp { ":" + new_sha };
      """.stripMargin
    updateWith(prog)
  }

  it should "update K8 spec with native Rug predicate" in {
    val prog =
      """
        |@description "Update Kube spec to redeploy a service"
        |editor Redeploy
        |
        |param service: ^[\w.\-_]+$
        |param new_sha: ^[a-f0-9]{7}$
        |
        |let regexp = ":[a-f0-9]{7}"
        |
        |with File f
        | when nameContains { ("80-" + service + "-deployment") }
        |do
        |  regexpReplace regexp { ":" + new_sha };
      """.stripMargin
    updateWith(prog)
  }

  // 80-project-operation-deployment.json
  it should "update K8 spec with Rug predicate checking JavaScript" in {
    val prog =
      """
        |@description "Update Kube spec to redeploy a service"
        |editor Redeploy
        |
        |param service: ^[\w.\-_]+$
        |param new_sha: ^[a-f0-9]{7}$
        |
        |let regexp = ":[a-f0-9]{7}"
        |
        |with File f
        | when name = { ("80-" + service + "-deployment.json") }
        |do
        |  regexpReplace regexp { ":" + new_sha };
      """.stripMargin
    updateWith(prog)
  }

  it should "update K8 spec with JavaScript regexp replace" in {
    val prog =
      """
        |@description "Update Kube spec to redeploy a service"
        |editor Redeploy
        |
        |param service: ^[\w.\-_]+$
        |param new_sha: ^[a-f0-9]{7}$
        |
        |with File f
        | when { f.name().indexOf("80-" + service + "-deployment") >= 0 }
        |do
        |  setContent {
        |   var re = /:[a-f0-9]{7}/gi
        |   return f.content().replace(re, ":" + new_sha)
        |};
      """.stripMargin
    updateWith(prog)
  }

  // Note we probably don't want to use this one!
  it should "update K8 spec with global regexp replace" in {
    val prog =
      """
        |@description "Update Kube spec to redeploy a service"
        |editor Redeploy
        |
        |param service: ^[\w.\-_]+$
        |param new_sha: ^[a-f0-9]{7}$
        |
        |with Project p
        |do
        |  regexpReplace { return service + ":[a-f0-9]{7}" } { service + ":" + new_sha };
      """.stripMargin
    updateWith(prog)
  }

  it should "update K8 spec with JavaScript JSON" in {
    val prog =
      """
        |@description "Update Kube spec to redeploy a service"
        |editor Redeploy
        |
        |param service: ^[\w.\-_]+$
        |param new_sha: ^[a-f0-9]{7}$
        |
        |with File f
        | when { f.name().indexOf("80-" + service + "-deployment") >= 0 }
        |do
        |  setContent {
        |    var json = JSON.parse(f.content());
        |    var container = json.spec.template.spec.containers[0]
        |    var re = /:[a-f0-9]{7}/gi;
        |    container.image = container.image.replace(re, ":" + new_sha);
        |    return JSON.stringify(json, null, 4);
        |  }
      """.stripMargin
    updateWith(prog)
  }

  // Return new content
  private def updateWith(prog: String): String = {
    val filename = "80-project-operation-deployment.json"
    val as = new SimpleFileBasedArtifactSource("name",
      Seq(
        StringFileArtifact(filename, spec)
      )
    )
    val service = "project-operation"
    val newSha = "666aabb"
    val pipeline = new CompilerChainPipeline(Seq(TypeScriptBuilder.compiler, new RugTranspiler()))
    val pas = new SimpleFileBasedArtifactSource(DefaultRugArchive, StringFileArtifact(pipeline.defaultFilenameFor(prog), prog)) + TypeScriptBuilder.userModel
    val r = doModification(pas, as, EmptyArtifactSource(""), SimpleParameterValues(Map(
      "service" -> service,
      "new_sha" -> newSha
    )), pipeline)

    val f = r.findFile(filename).get
    f.content.contains(s"/$service:$newSha") should be(true)
    f.content
  }
}
