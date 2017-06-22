package com.atomist.rug

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.SuccessfulModification
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
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

  // Note we probably don't want to use this one in real life!
  it should "update K8 spec with global regexp replace" in {
    updateWith("K8Redeploy.ts")
  }

  // Return new content
  private def updateWith(ts: String): String = {
    val filename = "80-project-operation-deployment.json"
    val as = new SimpleFileBasedArtifactSource("name",
      Seq(
        StringFileArtifact(filename, spec)
      )
    )
    val service = "project-operation"
    val newSha = "666aabb"
    val pe = TestUtils.editorInSideFile(this, ts)
    val r = pe.modify(as, SimpleParameterValues(Map(
      "service" -> service,
      "new_sha" -> newSha
    )))

    r match {
      case sm: SuccessfulModification =>
        val r = sm.result
        val f = r.findFile(filename).get
        f.content.contains(s"/$service:$newSha") shouldBe true
        f.content
      case x => fail(s"Unexpcted: $x")
    }
  }
}
