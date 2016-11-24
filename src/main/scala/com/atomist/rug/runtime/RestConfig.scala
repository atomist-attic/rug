package com.atomist.rug.runtime

import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.http.client.{ClientHttpRequestFactory, HttpComponentsClientHttpRequestFactory}
import org.springframework.web.client.{RestOperations, RestTemplate}

@Configuration
class RestConfig {

  @Bean
  def httpClient: RestOperations = {
    new RestTemplate(getClientHttpRequestFactory)
  }

  private def getClientHttpRequestFactory: ClientHttpRequestFactory  = {
    val timeout = 5000
    val clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory()
    clientHttpRequestFactory.setConnectTimeout(timeout)
    clientHttpRequestFactory
  }
}
