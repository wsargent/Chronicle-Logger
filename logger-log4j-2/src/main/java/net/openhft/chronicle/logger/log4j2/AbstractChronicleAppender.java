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
package net.openhft.chronicle.logger.log4j2;

import net.openhft.chronicle.logger.ChronicleLogWriter;
import net.openhft.chronicle.logger.LogAppenderConfig;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;

public abstract class AbstractChronicleAppender extends AbstractAppender {

    private final ChronicleCfg config;
    private String path;

    private ChronicleLogWriter writer;

    AbstractChronicleAppender(String name,
                              Filter filter,
                              Layout<? extends Serializable> layout,
                              boolean ignoreExceptions,
                              Property[] properties,
                              String path,
                              ChronicleCfg config) {
        super(name, filter, layout, ignoreExceptions, properties);

        this.path = path;
        this.config = config;
        this.writer = null;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    protected LogAppenderConfig getChronicleConfig() {
        return this.config;
    }

    protected abstract ChronicleLogWriter createWriter() throws IOException;

    // *************************************************************************
    //
    // *************************************************************************

    protected abstract void doAppend(@NotNull final LogEvent event, @NotNull final ChronicleLogWriter writer);

    @Override
    public void start() {
        if (getPath() == null) {
            LOGGER.error("Appender " + getName() + " has configuration errors and is not started!");

        } else {
            try {
                this.writer = createWriter();
                LogAppenderConfig.write(config, Paths.get(this.getPath()));
            } catch (IOException e) {
                this.writer = null;
                LOGGER.error("Appender " + getName() + " " + e.getMessage());
            }

            super.start();
        }
    }

    @Override
    public void stop() {
        if (this.writer != null) {
            try {
                this.writer.close();
            } catch (IOException e) {
                LOGGER.error("Appender " + getName() + " " + e.getMessage());
            }
        }

        super.stop();
    }

    // *************************************************************************
    //
    // *************************************************************************

    @Override
    public void append(final LogEvent event) {
        if (! isStarted()) {
            throw new IllegalStateException("Not started!");
        }
        if (this.writer != null) {
            doAppend(event, writer);
        }
    }

    // *************************************************************************
    //
    // *************************************************************************

    @Plugin(
            name = "chronicleCfg",
            category = "Core")
    public static final class ChronicleCfg extends LogAppenderConfig {
        ChronicleCfg() {
        }

        @PluginFactory
        public static ChronicleCfg create(
                @PluginAttribute("blockSize") final String blockSize,
                @PluginAttribute("bufferCapacity") final String bufferCapacity,
                @PluginAttribute("rollCycle") final String rollCycle,
                @PluginAttribute("contentType") final String contentType,
                @PluginAttribute("contentEncoding") final String contentEncoding) {

            final ChronicleCfg cfg = new ChronicleCfg();
            if (blockSize != null)
                cfg.blockSize = Integer.parseInt(blockSize);
            if (bufferCapacity != null)
                cfg.bufferCapacity = Long.parseLong(bufferCapacity);
            if (rollCycle != null)
                cfg.rollCycle = rollCycle;
            if (contentType != null)
                cfg.contentType = contentType;
            if (contentEncoding != null)
                cfg.contentEncoding = contentEncoding;

            return cfg;
        }
    }

}
