/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetdrone.vertx.yoke;

import com.jetdrone.vertx.yoke.middleware.YokeRequest;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.Map;

public interface RequestWrapper {

    /**
     * For other language bindings this method can be override.
     * @param request The Vertx HttpServerRequest
     * @param secure Is the server SSL?
     * @param context The shared context between request and response
     * @param engines the current list of render engines (this is an unmodifiable map)
     * @return an Implementation of YokeRequest
     */
    YokeRequest wrap(HttpServerRequest request, boolean secure, Map<String, Object> context, Map<String, Engine> engines);
}
