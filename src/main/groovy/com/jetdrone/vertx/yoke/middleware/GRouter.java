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
package com.jetdrone.vertx.yoke.middleware;

import com.jetdrone.vertx.yoke.Middleware;
import groovy.lang.Closure;
import org.vertx.java.core.Handler;

import java.util.regex.Pattern;

public class GRouter extends Middleware {

    private final Router jRouter = new Router();

    @Override
    public void handle(YokeRequest request, Handler<Object> next) {
        jRouter.handle(request, next);
    }

    private static Middleware wrapClosure(final Closure closure) {
        return new Middleware() {
            @Override
            public void handle(YokeRequest request, Handler<Object> next) {
                int params = closure.getMaximumNumberOfParameters();
                if (params == 1) {
                    closure.call(request);
                } else if (params == 2) {
                    closure.call(request, next);
                } else {
                    throw new RuntimeException("Cannot infer the closure signature, should be: request [, next]");
                }
            }
        };
    }

    /**
     * Specify a middleware that will be called for a matching HTTP GET
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter get(String pattern, Closure handler) {
        jRouter.get(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP PUT
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter put(String pattern, Closure handler) {
        jRouter.put(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP POST
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter post(String pattern, Closure handler) {
        jRouter.post(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP DELETE
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter delete(String pattern, Closure handler) {
        jRouter.delete(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP OPTIONS
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter options(String pattern, Closure handler) {
        jRouter.options(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP HEAD
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter head(String pattern, Closure handler) {
        jRouter.head(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP TRACE
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter trace(String pattern, Closure handler) {
        jRouter.trace(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP CONNECT
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter connect(String pattern, Closure handler) {
        jRouter.trace(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP PATCH
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter patch(String pattern, Closure handler) {
        jRouter.trace(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for all HTTP methods
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter all(String pattern, Closure handler) {
        jRouter.all(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP GET
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter get(Pattern pattern, Closure handler) {
        jRouter.get(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP PUT
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter put(Pattern pattern, Closure handler) {
        jRouter.put(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP POST
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter post(Pattern pattern, Closure handler) {
        jRouter.post(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP DELETE
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter delete(Pattern pattern, Closure handler) {
        jRouter.delete(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP OPTIONS
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter options(Pattern pattern, Closure handler) {
        jRouter.options(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP HEAD
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter head(Pattern pattern, Closure handler) {
        jRouter.head(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP TRACE
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter trace(Pattern pattern, Closure handler) {
        jRouter.trace(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP CONNECT
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter connect(Pattern pattern, Closure handler) {
        jRouter.trace(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for a matching HTTP PATCH
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter patch(Pattern pattern, Closure handler) {
        jRouter.trace(pattern, wrapClosure(handler));
        return this;
    }

    /**
     * Specify a middleware that will be called for all HTTP methods
     * @param pattern The simple pattern
     * @param handler The middleware to call
     */
    public GRouter all(Pattern pattern, Closure handler) {
        jRouter.all(pattern, wrapClosure(handler));
        return this;
    }
}
