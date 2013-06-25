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
package com.jetdrone.vertx.yoke.engine;

import com.jetdrone.vertx.yoke.util.YokeAsyncResult;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringPlaceholderEngine extends AbstractEngine<String> {

    private static final String placeholderPrefix = "${";
    private static final String placeholderSuffix = "}";
    private static final boolean ignoreUnresolvablePlaceholders = true;

    private static final String funcName = "([a-zA-Z0-9]+)";
    private static final String arguments = "\\((.*)\\)";
    private static final Pattern FUNCTION = Pattern.compile(funcName + "\\s*" + arguments);

    private static final String argument = "(.*?)";
    private static final String quote = "\'";
    private static final String sep = "(,\\s*)?";
    private static final Pattern ARG = Pattern.compile(quote + argument + quote + sep);

    /**
     * An interpreter for strings with named placeholders.
     *
     * For example given the string "hello ${myName}" and the map <code>
     *      <p>Map<String, Object> map = new HashMap<String, Object>();</p>
     *      <p>map.put("myName", "world");</p>
     * </code>
     *
     * the call returns "hello world"
     *
     * It replaces every occurrence of a named placeholder with its given value
     * in the map. If there is a named place holder which is not found in the
     * map then the string will retain that placeholder. Likewise, if there is
     * an entry in the map that does not have its respective placeholder, it is
     * ignored.
     */
    @Override
    public void render(final String file, final Map<String, Object> context, final Handler<AsyncResult<Buffer>> handler) {
        // verify if the file is still fresh in the cache
        read(file, new AsyncResultHandler<String>() {
            @Override
            public void handle(AsyncResult<String> asyncResult) {
                if (asyncResult.failed()) {
                    handler.handle(new YokeAsyncResult<Buffer>(asyncResult.cause()));
                } else {
                    try {
                        handler.handle(new YokeAsyncResult<>(parseStringValue(asyncResult.result(), context, new HashSet<String>())));
                    } catch (IllegalArgumentException iae) {
                        handler.handle(new YokeAsyncResult<Buffer>(iae));
                    }
                }
            }
        });
    }

    private Buffer parseStringValue(String template, Map<String, Object> context, Set<String> visitedPlaceholders) {
        StringBuilder buf = new StringBuilder(template);

        int startIndex = template.indexOf(placeholderPrefix);
        while (startIndex != -1) {
            int endIndex = findPlaceholderEndIndex(buf, startIndex);
            if (endIndex != -1) {
                String placeholder = buf.substring(startIndex + placeholderPrefix.length(), endIndex);
                if (!visitedPlaceholders.add(placeholder)) {
                    throw new IllegalArgumentException(
                            "Circular placeholder reference '" + placeholder + "' in property definitions");
                }
                // Recursive invocation, parsing placeholders contained in the placeholder key.
                placeholder = parseStringValue(placeholder, context, visitedPlaceholders).toString(contentEncoding());

                // Now obtain the value for the fully resolved key...
                Object propVal;
                boolean isFn = false;

                Matcher fn = FUNCTION.matcher(placeholder);
                if (fn.find()) {
                    // function syntax used, get the object from context with the proper name
                    propVal = context.get(fn.group(1));
                    isFn = true;
                } else {
                    propVal = context.get(placeholder);
                }

                if (propVal != null) {
                    // Recursive invocation, parsing placeholders contained in the
                    // previously resolved placeholder value.
                    String propValStr;
                    if (isFn && propVal instanceof Function) {
                        Matcher arg = ARG.matcher(fn.group(2));
                        List<Object> args = null;

                        while (arg.find()) {
                            if (args == null) {
                                args = new ArrayList<>();
                            }
                            args.add(arg.group(1));
                        }
                        if (args == null) {
                            propValStr = ((Function) propVal).exec(context);
                        } else {
                            propValStr = ((Function) propVal).exec(context, args.toArray());
                        }
                    } else {
                        propValStr = propVal.toString();
                    }
                    propValStr = parseStringValue(propValStr, context, visitedPlaceholders).toString(contentEncoding());
                    buf.replace(startIndex, endIndex + placeholderSuffix.length(), propValStr);

                    startIndex = buf.indexOf(placeholderPrefix, startIndex + propValStr.length());
                }
                else if (ignoreUnresolvablePlaceholders) {
                    // Proceed with unprocessed value.
                    startIndex = buf.indexOf(placeholderPrefix, endIndex + placeholderSuffix.length());
                }
                else {
                    throw new IllegalArgumentException("Could not resolve placeholder '" + placeholder + "'");
                }

                visitedPlaceholders.remove(placeholder);
            }
            else {
                startIndex = -1;
            }
        }

        return new Buffer(buf.toString());
    }

    private static int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
        int index = startIndex + placeholderPrefix.length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            if (substringMatch(buf, index, placeholderSuffix)) {
                if (withinNestedPlaceholder > 0) {
                    withinNestedPlaceholder--;
                    index = index + placeholderPrefix.length() - 1;
                }
                else {
                    return index;
                }
            }
            else if (substringMatch(buf, index, placeholderPrefix)) {
                withinNestedPlaceholder++;
                index = index + placeholderPrefix.length();
            }
            else {
                index++;
            }
        }
        return -1;
    }

    private static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        for (int j = 0; j < substring.length(); j++) {
            int i = index + j;
            if (i >= str.length() || str.charAt(i) != substring.charAt(j)) {
                return false;
            }
        }
        return true;
    }
}
