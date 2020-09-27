/*
 * Copyright 2014-2017 Chronicle Software
 *
 * http://www.chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.logger.logback;

import ch.qos.logback.classic.Level;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.logger.ChronicleEntryProcessor;
import net.openhft.chronicle.logger.entry.Entry;
import net.openhft.chronicle.logger.entry.EntryReader;
import net.openhft.chronicle.logger.DefaultChronicleEntryProcessor;
import net.openhft.chronicle.logger.codec.CodecRegistry;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static ch.qos.logback.classic.Level.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class LogbackChronicleZstdBinaryAppenderTest extends LogbackTestBase {

    @Override
    String getResource() {
        return "/logback-chronicle-zstd-binary-appender.xml";
    }

    @Test
    public void testZstdBinaryAppender() throws IOException {
        final String testId = "binary-zstd-chronicle";
        final String threadId = testId + "-th";

        final Logger logger = getLoggerContext().getLogger(testId);
        Path path = Paths.get(basePath(testId));
        Files.createDirectories(path);

        Thread.currentThread().setName(threadId);

        Level[] levels = { ERROR, WARN, INFO, DEBUG, TRACE };
        for (Level level : levels) {
            log(logger, level, "level is {}", level);
        }

        EntryReader eventReader = new EntryReader();
        CodecRegistry registry = CodecRegistry.builder().withDefaults(path).build();
        ChronicleEntryProcessor<String> processor = new DefaultChronicleEntryProcessor(registry);

        try (final ChronicleQueue cq = getChronicleQueue(testId)) {
            net.openhft.chronicle.queue.ExcerptTailer tailer = cq.createTailer();
            for (Level level: levels) {
                Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer();
                tailer.readBytes(bytes);
                Entry entry = eventReader.read(bytes);
                assertTrue(entry.timestamp().epochSecond() <= Instant.now().getEpochSecond());
                assertEquals(level.toInt(), entry.level());
                assertEquals(threadId, entry.threadName());
                assertEquals(testId, entry.loggerName());
                assertEquals("level is " + level.levelStr, processor.apply(entry));
            }
            try (DocumentContext dc = tailer.readingDocument()) {
                Wire wire = dc.wire();
                assertNull(wire);
            }
        }
        IOTools.deleteDirWithFiles(basePath(testId));
    }
}
