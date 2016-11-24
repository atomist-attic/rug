package com.atomist.rug.kind.http

import org.springframework.context.annotation.{Bean, Configuration, Primary}
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.{RestOperations, RestTemplate}

object MockRestServer {
  val restTemplate = new RestTemplate()
  val server = MockRestServiceServer.bindTo(restTemplate).build()
}

@Configuration
class MockRestConfig {

  @Bean
  @Primary
  def httpClientMock: RestOperations = {
    MockRestServer.restTemplate
  }
}