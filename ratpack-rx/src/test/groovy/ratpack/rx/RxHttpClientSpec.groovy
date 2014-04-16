/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.rx

import com.google.inject.AbstractModule
import ratpack.http.client.HttpClientSpec

import static ratpack.rx.RxRatpack.observe

class RxHttpClientSpec extends HttpClientSpec {

  def setup() {
    modules {
      RxRatpack.install(launchConfig.execController)
    }
  }

  def "can use rx with http client"() {
    given:
    otherApp {
      get("foo") { render "bar" }
    }

    when:
    handlers {
      get {
        observe(httpClient.get(otherAppUrl("foo"))) map {
          it.body.text.toUpperCase()
        } subscribe {
          render it
        }
      }
    }

    then:
    text == "BAR"
  }

}
