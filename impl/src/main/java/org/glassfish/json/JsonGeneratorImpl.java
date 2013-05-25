/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.json;

import javax.json.*;
import javax.json.stream.JsonGenerationException;
import javax.json.stream.JsonGenerator;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * @author Jitendra Kotamraju
 */
class JsonGeneratorImpl implements JsonGenerator {
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    protected final GeneratorBufferedWriter writer;
    protected Context currentContext = new Context(Scope.IN_NONE);
    private final Deque<Context> stack = new ArrayDeque<Context>();

    JsonGeneratorImpl(Writer writer) {
        this.writer = new GeneratorBufferedWriter(writer);
    }

    JsonGeneratorImpl(OutputStream out) {
        this(out, UTF_8);
    }

    JsonGeneratorImpl(OutputStream out, Charset encoding) {
        this(new OutputStreamWriter(out, encoding));
    }

    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            throw new JsonException("I/O error while flushing JsonGenerator", e);
        }
    }

    private static enum Scope {
        IN_NONE,
        IN_OBJECT,
        IN_ARRAY
    }

    @Override
    public JsonGenerator writeStartObject() {
        if (currentContext.scope == Scope.IN_OBJECT) {
            throw new JsonGenerationException("writeStartObject() cannot be called in object context");
        }
        if (currentContext.scope == Scope.IN_NONE && !currentContext.first) {
            throw new JsonGenerationException("writeStartObject() cannot be called in no context more than once");
        }
        try {
            writeComma();
            writer.write("{");
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing start object", ioe);
        }
        stack.push(currentContext);
        currentContext = new Context(Scope.IN_OBJECT);
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(String name) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException("writeStartObject(String) can only be called in object context");
        }
        try {
            writeName(name);
            writer.write("{");
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing start of object in JSON object", ioe);
        }
        stack.push(currentContext);
        currentContext = new Context(Scope.IN_OBJECT);
        return this;
    }

    private JsonGenerator writeName(String name) throws IOException {
        writeComma();
        writeEscapedString(writer, name);
        writer.write(":");
        return this;
    }

    @Override
    public JsonGenerator write(String name, String fieldValue) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException("write(String, String) can only be called in object context");
        }
        try {
            writeName(name);
            writeEscapedString(writer, fieldValue);
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing (name, String) pair in JSON object", ioe);
        }
        return this;
    }

    @Override
    public JsonGenerator write(String name, int value) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException("write(String, int) can only be called in object context");
        }
        try {
            writeName(name);
            writer.write(value);
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing (name, int) pair in JSON object", ioe);
        }
        return this;
    }

    @Override
    public JsonGenerator write(String name, long value) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException("write(String, long) can only be called in object context");
        }
        try {
            writeName(name);
            writer.write(String.valueOf(value));
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing (name, long) pair in JSON object",ioe);
        }
        return this;
    }

    @Override
    public JsonGenerator write(String name, double value) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException("write(String, double) can only be called in object context");
        }
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new NumberFormatException("write(String, double) value cannot be Infinite or NaN");
        }
        try {
            writeName(name);
            writer.write(String.valueOf(value));
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing (name, double) pair in JSON object", ioe);
        }
        return this;
    }

    @Override
    public JsonGenerator write(String name, BigInteger value) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException("write(String, BigInteger) can only be called in object context");
        }
        try {
            writeName(name);
            writer.write(String.valueOf(value));
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing (name, BigInteger) pair in JSON object", ioe);
        }
        return this;
    }

    @Override
    public JsonGenerator write(String name, BigDecimal value) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException("write(String, BigDecimal) can only be called in object context");
        }
        try {
            writeName(name);
            writer.write(String.valueOf(value));
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing (name, BigDecimal) pair in JSON object", ioe);
        }
        return this;
    }

    @Override
    public JsonGenerator write(String name, boolean value) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException("write(String, boolean) can only be called in object context");
        }
        try {
            writeName(name);
            writer.write(value? "true" : "false");
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing (name, boolean) pair in JSON object", ioe);
        }
        return this;
    }

    @Override
    public JsonGenerator writeNull(String name) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException("writeNull(String) can only be called in object context");
        }
        try {
            writeName(name);
            writer.write("null");
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing null value in JSON object", ioe);
        }
        return this;
    }

    @Override
    public JsonGenerator write(JsonValue value) {
        if (currentContext.scope != Scope.IN_ARRAY) {
            throw new JsonGenerationException("write(JsonValue) can only be called in array context");
        }
        switch (value.getValueType()) {
            case ARRAY:
                JsonArray array = (JsonArray)value;
                writeStartArray();
                for(JsonValue child: array) {
                    write(child);
                }
                writeEnd();
                break;
            case OBJECT:
                JsonObject object = (JsonObject)value;
                writeStartObject();
                for(Map.Entry<String, JsonValue> member: object.entrySet()) {
                    write(member.getKey(), member.getValue());
                }
                writeEnd();
                break;
            case STRING:
                JsonString str = (JsonString)value;
                write(str.getString());
                break;
            case NUMBER:
                JsonNumber number = (JsonNumber)value;
                try {
                    writeValue(number.toString());
                } catch(IOException ioe) {
                    throw new JsonException("I/O error while writing a number", ioe);
                }
                break;
            case TRUE:
                write(true);
                break;
            case FALSE:
                write(false);
                break;
            case NULL:
                writeNull();
                break;
        }

        return this;
    }

    @Override
    public JsonGenerator writeStartArray() {
        if (currentContext.scope == Scope.IN_OBJECT) {
            throw new JsonGenerationException("writeStartArray() cannot be called in object context");
        }
        if (currentContext.scope == Scope.IN_NONE && !currentContext.first) {
            throw new JsonGenerationException("writeStartArray() cannot be called in no context more than once");
        }
        try {
            writeComma();
            writer.write("[");
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing start of JSON array", ioe);
        }
        stack.push(currentContext);
        currentContext = new Context(Scope.IN_ARRAY);
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(String name) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException("writeStartArray(String) can only be called in object context");
        }
        try {
            writeName(name);
            writer.write("[");
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing start of array in JSON object", ioe);
        }
        stack.push(currentContext);
        currentContext = new Context(Scope.IN_ARRAY);
        return this;
    }

    @Override
    public JsonGenerator write(String name, JsonValue value) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException("write(String, JsonValue) can only be called in object context");
        }
        switch (value.getValueType()) {
            case ARRAY:
                JsonArray array = (JsonArray)value;
                writeStartArray(name);
                for(JsonValue child: array) {
                    write(child);
                }
                writeEnd();
                break;
            case OBJECT:
                JsonObject object = (JsonObject)value;
                writeStartObject(name);
                for(Map.Entry<String, JsonValue> member: object.entrySet()) {
                    write(member.getKey(), member.getValue());
                }
                writeEnd();
                break;
            case STRING:
                JsonString str = (JsonString)value;
                write(name, str.getString());
                break;
            case NUMBER:
                JsonNumber number = (JsonNumber)value;
                try {
                    writeValue(name, number.toString());
                } catch (IOException ioe) {
                    throw new JsonException("I/O error while writing a number in JSON object", ioe);
                }
                break;
            case TRUE:
                write(name, true);
                break;
            case FALSE:
                write(name, false);
                break;
            case NULL:
                writeNull(name);
                break;
        }
        return this;
    }

    public JsonGenerator write(String value) {
        if (currentContext.scope != Scope.IN_ARRAY) {
            throw new JsonGenerationException("write(String) can only be called in array context");
        }
        try {
            writeComma();
            writeEscapedString(writer, value);
        } catch (IOException e) {
            throw new JsonException("I/O error while writing string value in JSON array", e);
        }
        return this;
    }


    public JsonGenerator write(int value) {
        if (currentContext.scope != Scope.IN_ARRAY) {
            throw new JsonGenerationException("write(int) can only be called in array context");
        }
        try {
            writeComma();
            writer.write(value);
        } catch (IOException e) {
            throw new JsonException("I/O error while writing int value in JSON array", e);
        }
        return this;
    }

    @Override
    public JsonGenerator write(long value) {
        if (currentContext.scope != Scope.IN_ARRAY) {
            throw new JsonGenerationException("write(long) can only be called in array context");
        }
        try {
            writeValue(String.valueOf(value));
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing long value in JSON array", ioe);
        }
        return this;
    }

    @Override
    public JsonGenerator write(double value) {
        if (currentContext.scope != Scope.IN_ARRAY) {
            throw new JsonGenerationException("write(double) can only be called in array context");
        }
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new NumberFormatException("write(double) value cannot be Infinite or NaN");
        }
        try {
            writeValue(String.valueOf(value));
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing double value in JSON array", ioe);
        }
        return this;
    }

    @Override
    public JsonGenerator write(BigInteger value) {
        if (currentContext.scope != Scope.IN_ARRAY) {
            throw new JsonGenerationException("write(BigInteger) can only be called in array context");
        }
        try {
            writeValue(value.toString());
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing BigInteger value in JSON array", ioe);
        }
        return this;
    }

    @Override
    public JsonGenerator write(BigDecimal value) {
        if (currentContext.scope != Scope.IN_ARRAY) {
            throw new JsonGenerationException("write(BigDecimal) can only be called in array context");
        }
        try {
            writeValue(value.toString());
        } catch(IOException ioe) {
            throw new JsonException("I/O error while writing BigDecimal value in JSON array", ioe);
        }
        return this;
    }

    public JsonGenerator write(boolean value) {
        if (currentContext.scope != Scope.IN_ARRAY) {
            throw new JsonGenerationException("write(boolean) can only be called in array context");
        }
        try {
            writeComma();
            writer.write(value ? "true" : "false");
        } catch (IOException e) {
            throw new JsonException("I/O error while writing boolean value in JSON array", e);
        }
        return this;
    }

    public JsonGenerator writeNull() {
        if (currentContext.scope != Scope.IN_ARRAY) {
            throw new JsonGenerationException("writeNull() can only be called in array context");
        }
        try {
            writeComma();
            writer.write("null");
        } catch (IOException e) {
            throw new JsonException("I/O error while writing null value in JSON array", e);
        }
        return this;
    }

    private void writeValue(String value) throws IOException {
        writeComma();
        writer.write(value);
    }

    private void writeValue(String name, String value) throws IOException {
        writeComma();
        writeEscapedString(writer, name);
        writer.write(':');
        writer.write(value);
    }

    void flushBuffer() {
        try {
            writer.flushBuffer();
        } catch (IOException e) {
            throw new JsonException("I/O error while flushing the buffer", e);
        }
    }

    @Override
    public JsonGenerator writeEnd() {
        if (currentContext.scope == Scope.IN_NONE) {
            throw new JsonGenerationException("writeEnd() cannot be called in no context");
        }
        try {
            writer.write(currentContext.scope == Scope.IN_ARRAY ? ']' : '}');
        } catch (IOException e) {
            throw new JsonException("I/O error while writing end of JSON structure", e);
        }
        currentContext = stack.pop();
        return this;
    }

    protected void writeComma() throws IOException {
        if (!currentContext.first) {
            writer.write(",");
        }
        currentContext.first = false;
    }

    private static class Context {
        boolean first = true;
        final Scope scope;

        Context(Scope scope) {
            this.scope = scope;
        }

    }

    public void close() {
        if (currentContext.scope != Scope.IN_NONE || currentContext.first) {
            throw new JsonGenerationException("Generating incomplete JSON");
        }
        try {
            writer.close();
        } catch (IOException ioe) {
            throw new JsonException("I/O error while closing JsonGenerator", ioe);
        }
    }

    static void writeEscapedString(GeneratorBufferedWriter w, String string) throws IOException {
        w.write('"');
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
                case '"':
                case '\\':
                    w.write('\\');
                    w.write(c);
                    break;
                case '\b':
                    w.write("\\b");
                    break;
                case '\f':
                    w.write("\\f");
                    break;
                case '\n':
                    w.write("\\n");
                    break;
                case '\r':
                    w.write("\\r");
                    break;
                case '\t':
                    w.write("\\t");
                    break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
                        String hex = "000" + Integer.toHexString(c);
                        w.write("\\u" + hex.substring(hex.length() - 4));
                    } else {
                        w.write(c);
                    }
            }
        }
        w.write('"');
    }

    // Using own buffering mechanism as JDK's BufferedWriter uses synchronized
    // methods. Also, flushBuffer() is useful when you don't want to actually
    // flush the underlying output source
    static final class GeneratorBufferedWriter {
        private final Writer writer;
        private final char buf[] = new char[4096];  // capacity >= INT_MIN_VALUE_CHARS.length
        private int len = 0;

        GeneratorBufferedWriter(Writer writer) {
            this.writer = writer;
        }

        void write(String str) throws IOException {
            int begin = 0, end = str.length();
            while (begin < end) {       // source begin and end indexes
                int no = Math.min(buf.length - len, end - begin);
                str.getChars(begin, begin + no, buf, len);
                begin += no;            // Increment source index
                len += no;              // Increment dest index
                if (len >= buf.length) {
                    flushBuffer();
                }
            }
        }

        void write(char c) throws IOException {
            if (len >= buf.length) {
                flushBuffer();
            }
            buf[len++] = c;
        }

        // Not using Integer.toString() since it creates intermediary String
        // Also, we want the chars to be copied to our buffer directly
        void write(int num) throws IOException {
            int size;
            if (num == Integer.MIN_VALUE) {
                size = INT_MIN_VALUE_CHARS.length;
            } else {
                size = (num < 0) ? stringSize(-num) + 1 : stringSize(num);
            }
            if (len+size >= buf.length) {
                flushBuffer();
            }
            if (num == Integer.MIN_VALUE) {
                System.arraycopy(INT_MIN_VALUE_CHARS, 0, buf, len, size);
            } else {
                fillIntChars(num, buf, len+size);
            }
            len += size;
        }

        void flushBuffer() throws IOException {
            if (len > 0) {
                writer.write(buf, 0, len);
                len = 0;
            }
        }

        void flush() throws IOException {
            flushBuffer();
            writer.flush();
        }

        void close() throws IOException {
            flushBuffer();
            writer.close();
        }
    }

    private static final char[] INT_MIN_VALUE_CHARS = "-2147483648".toCharArray();
    private static final int [] INT_CHARS_SIZE_TABLE = { 9, 99, 999, 9999, 99999,
            999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE };

    // Requires positive x
    private static int stringSize(int x) {
        for (int i=0; ; i++)
            if (x <= INT_CHARS_SIZE_TABLE[i])
                return i+1;
    }

    /**
     * Places characters representing the integer i into the
     * character array buf. The characters are placed into
     * the buffer backwards starting with the least significant
     * digit at the specified index (exclusive), and working
     * backwards from there.
     *
     * Will fail if i == Integer.MIN_VALUE
     */
    private static void fillIntChars(int i, char[] buf, int index) {
        int q, r;
        int charPos = index;
        char sign = 0;

        if (i < 0) {
            sign = '-';
            i = -i;
        }

        // Generate two digits per iteration
        while (i >= 65536) {
            q = i / 100;
            // really: r = i - (q * 100);
            r = i - ((q << 6) + (q << 5) + (q << 2));
            i = q;
            buf [--charPos] = DIGIT_ONES[r];
            buf [--charPos] = DIGIT_TENS[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i <= 65536, i);
        for (;;) {
            q = (i * 52429) >>> (16+3);
            r = i - ((q << 3) + (q << 1));  // r = i-(q*10) ...
            buf [--charPos] = DIGITS[r];
            i = q;
            if (i == 0) break;
        }
        if (sign != 0) {
            buf [--charPos] = sign;
        }
    }

    private static final char [] DIGIT_TENS = {
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
            '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
            '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
            '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
            '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
            '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
            '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
            '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
            '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
    } ;

    private static final char [] DIGIT_ONES = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    } ;

    /**
     * All possible chars for representing a number as a String
     */
    private static final char[] DIGITS = {
            '0' , '1' , '2' , '3' , '4' , '5' ,
            '6' , '7' , '8' , '9'
    };

}