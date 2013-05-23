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

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetSocket;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public class YokeRequest implements HttpServerRequest {

    private static final Comparator<String> ACCEPT_X_COMPARATOR = new Comparator<String>() {
        float getQuality(String s) {
            if (s == null) {
                return 0;
            }

            String[] params = s.split(" *; *");
            for (int i = 1; i < params.length; i++) {
                String[] q = params[1].split(" *= *");
                if ("q".equals(q[0])) {
                    return Float.parseFloat(q[1]);
                }
            }
            return 1;
        }
        @Override
        public int compare(String o1, String o2) {
            // verify if there is a
            float f1 = getQuality(o1);
            float f2 = getQuality(o2);
            if (f1 < f2) {
                return 1;
            }
            if (f1 > f2) {
                return -1;
            }
            return 0;
        }
    };

    // the original request
    private final HttpServerRequest request;
    // the wrapped response
    private final YokeResponse response;
    // the request context
    private final Map<String, Object> context;
    // is this request secure
    private final boolean secure;

    // we can overrride the setMethod
    private String method;
    private long bodyLengthLimit = -1;
    private Object body;
    private Map<String, HttpServerFileUpload> files;
    private Set<YokeCookie> cookies;
    private String sessionId;

    public YokeRequest(HttpServerRequest request, YokeResponse response, boolean secure, Map<String, Object> context) {
        this.context = context;
        this.request = request;
        this.method = request.method();
        this.response = response;
        this.secure = secure;
    }

    /**
     * Allow getting properties in a generified way.
     *
     * @param name The key to get
     * @param <R> The type of the return
     * @return The found object
     */
    @SuppressWarnings("unchecked")
    public <R> R get(String name) {
        return (R) context.get(name);
    }

    /**
     * Allow getting properties in a generified way and return defaultValue if the key does not exist.
     *
     * @param name The key to get
     * @param defaultValue value returned when the key does not exist
     * @param <R> The type of the return
     * @return The found object
     */
    public <R> R get(String name, R defaultValue) {
        if (context.containsKey(name)) {
            return get(name);
        } else {
            return defaultValue;
        }
    }

    /**
     * Allows putting a value into the context
     *
     * @param name the key to store
     * @param value the value to store
     * @param <R> the type of the previous value if present
     * @return the previous value or null
     */
    @SuppressWarnings("unchecked")
    public <R> R put(String name, R value) {
        return (R) context.put(name, value);
    }

    /**
     * Allow getting headers in a generified way.
     *
     * @param name The key to get
     * @param <R> The type of the return
     * @return The found object
     */
    @SuppressWarnings("unchecked")
    public <R> R getHeader(String name) {
        return (R) headers().get(name);
    }

    /**
     * Allow getting headers in a generified way and return defaultValue if the key does not exist.
     *
     * @param name The key to get
     * @param defaultValue value returned when the key does not exist
     * @param <R> The type of the return
     * @return The found object
     */
    public <R> R getHeader(String name, R defaultValue) {
        if (headers().contains(name)) {
            return getHeader(name);
        } else {
            return defaultValue;
        }
    }

    /**
     * The original HTTP setMethod for the request. One of GET, PUT, POST, DELETE, TRACE, CONNECT, OPTIONS or HEAD
     */
    public String originalMethod() {
        return request.method();
    }

    /**
     * Package level mutator for the overrided setMethod
     * @param newMethod new setMethod GET, PUT, POST, DELETE, TRACE, CONNECT, OPTIONS or HEAD
     */
    void setMethod(String newMethod) {
        this.method = newMethod.toUpperCase();
    }

    /**
     * Package level mutator for the bodyLength
     */
    void setBodyLengthLimit(long limit) {
        bodyLengthLimit = limit;
    }

    /**
     * Holds the maximum allowed length for the setBody data. -1 for unlimited
     */
    public long bodyLengthLimit() {
        return bodyLengthLimit;
    }

    /**
     * Returns true if this request has setBody
     *
     * @return true if content-length or transfer-encoding is present
     */
    public boolean hasBody() {
        MultiMap headers = headers();
        return headers.contains("transfer-encoding") || headers.contains("content-length");
    }

    /**
     * Returns the content length of this request setBody or -1 if header is not present.
     */
    public long contentLength() {
        String contentLengthHeader = headers().get("content-length");
        if (contentLengthHeader != null) {
            return Long.parseLong(contentLengthHeader);
        } else {
            return -1;
        }
    }

    /**
     * The request setBody and eventually a parsed version of it in json or map
     */
    public Object body() {
        return body;
    }

    /**
     * The request setBody and eventually a parsed version of it in json or map
     */
    public JsonObject jsonBody() {
        if (body != null && body instanceof JsonObject) {
            return (JsonObject) body;
        }
        return null;
    }

    /**
     * The request setBody and eventually a parsed version of it in json or map
     */
    public Buffer bufferBody() {
        if (body != null && body instanceof Buffer) {
            return (Buffer) body;
        }
        return null;
    }

    /**
     * Mutator for the request setBody
     * The request setBody and eventually a parsed version of it in json or map
     */
    void setBody(Object body) {
        this.body = body;
    }

    /**
     * The uploaded setFiles
     */
    public Map<String, HttpServerFileUpload> files() {
        return files;
    }

    /**
     * The uploaded setFiles
     */
    void setFiles(Map<String, HttpServerFileUpload> files) {
        this.files = files;
    }

    /**
     * Cookies
     */
    public Set<YokeCookie> cookies() {
        return cookies;
    }

    /**
     * Cookies
     */
    void setCookies(Set<YokeCookie> cookies) {
        this.cookies = cookies;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isSecure() {
        return secure;
    }

    /**
     * Check if the given type(s) is acceptable, returning the best match when true, otherwise null, in which
     * case you should respond with 406 "Not Acceptable".
     *
     * The type value must be a single mime type string such as "application/json" and is validated by checking
     * if the request string starts with it.
     */
    public String accepts(String... types) {
        String accept = getHeader("accept");
        // accept anything when accept is not present
        if (accept == null) {
            return types[0];
        }

        // parse
        String[] acceptTypes = accept.split(" *, *");
        // sort on quality
        Arrays.sort(acceptTypes, ACCEPT_X_COMPARATOR);

        for (String senderAccept : acceptTypes) {
            for (String appAccept : types) {
                if (senderAccept.startsWith(appAccept)) {
                    return senderAccept;
                }
            }
        }

        return null;
    }

    /**
     * Returns the ip address of the client, when trust-proxy is true (default) then first look into X-Forward-For
     * Header
     */
    public String ip() {
        Boolean trustProxy = (Boolean) context.get("trust-proxy");
        if (trustProxy != null && trustProxy) {
            String xForwardFor = getHeader("x-forward-for");
            if (xForwardFor != null) {
                String[] ips = xForwardFor.split(" *, *");
                if (ips.length > 0) {
                    return ips[0];
                }
            }
        }

        return request.remoteAddress().getHostName();
    }

    /**
     * Return the real request
     */
    public HttpServerRequest vertxHttpServerRequest() {
        return request;
    }

    @Override
    public HttpVersion version() {
        return request.version();
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#method()
     */
    @Override
    public String method() {
        if (method != null) {
            return method;
        }
        return request.method();
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#uri()
     */
    @Override
    public String uri() {
        return request.uri();
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#path()
     */
    @Override
    public String path() {
        return request.path();
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#query()
     */
    @Override
    public String query() {
        return request.query();
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#response()
     */
    @Override
    public YokeResponse response() {
        return response;
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#headers()
     */
    @Override
    public MultiMap headers() {
        return request.headers();
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#params()
     */
    @Override
    public MultiMap params() {
        return request.params();
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#remoteAddress()
     */
    @Override
    public InetSocketAddress remoteAddress() {
        return request.remoteAddress();
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#peerCertificateChain()
     */
    @Override
    public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
        return request.peerCertificateChain();
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#absoluteURI()
     */
    @Override
    public URI absoluteURI() {
        return request.absoluteURI();
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#bodyHandler(org.vertx.java.core.Handler)
     */
    @Override
    public HttpServerRequest bodyHandler(Handler<Buffer> bodyHandler) {
        request.bodyHandler(bodyHandler);
        return this;
    }

    @Override
    public NetSocket netSocket() {
        return request.netSocket();
    }

    @Override
    public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler) {
        return request.uploadHandler(uploadHandler);
    }

    @Override
    public MultiMap formAttributes() {
        return request.formAttributes();
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#dataHandler(org.vertx.java.core.Handler)
     */
    @Override
    public HttpServerRequest dataHandler(Handler<Buffer> handler) {
        request.dataHandler(handler);
        return this;
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#pause()
     */
    @Override
    public HttpServerRequest pause() {
        request.pause();
        return this;
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#resume()
     */
    @Override
    public HttpServerRequest resume() {
        request.resume();
        return this;
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#endHandler(org.vertx.java.core.Handler)
     */
    @Override
    public HttpServerRequest endHandler(Handler<Void> endHandler) {
        request.endHandler(endHandler);
        return this;
    }

    /**
     * @see org.vertx.java.core.http.HttpServerRequest#exceptionHandler(org.vertx.java.core.Handler)
     */
    @Override
    public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
        request.exceptionHandler(handler);
        return this;
    }
}