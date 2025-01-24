/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.bootstrap;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.settings.SecureSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.PathUtils;
import org.elasticsearch.core.SuppressForbidden;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;

/**
 * 用于运行Elasticsearch的参数
 *
 * @param daemonize Elasticsearch是否以守护线程运行，如果不以守护线程运行，则为{@code true}
 * @param quiet Elasticsearch是否应该输出日志到控制台，如果不输出，则为{@code false}
 * @param pidFile Elasticsearch应该将其进程ID写入的文件的绝对路径，如果不应该写入pid文件，则为{@code null}
 * @param secrets 提供的安全设置实现
 * @param nodeSettings 从{@code elasticsearch.yml}、cli和进程环境中读取的节点设置
 * @param configDir {@code elasticsearch.yml}和其他配置所在的目录
 * @param logsDir 日志文件应写入的目录
 */
public record ServerArgs(
    boolean daemonize,
    boolean quiet,
    Path pidFile,
    SecureSettings secrets,
    Settings nodeSettings,
    Path configDir,
    Path logsDir
) implements Writeable {

    /**
     * Arguments for running Elasticsearch.
     *
     * @param daemonize {@code true} if Elasticsearch should run as a daemon process, or {@code false} otherwise
     * @param quiet {@code false} if Elasticsearch should print log output to the console, {@code true} otherwise
     * @param pidFile absolute path to a file Elasticsearch should write its process id to, or {@code null} if no pid file should be written
     * @param secrets the provided secure settings implementation
     * @param nodeSettings the node settings read from {@code elasticsearch.yml}, the cli and the process environment
     * @param configDir the directory where {@code elasticsearch.yml} and other config exists
     */
    public ServerArgs {
        assert pidFile == null || pidFile.isAbsolute();
        assert secrets != null;
    }

    /**
     * Alternate constructor to read the args from a binary stream.
     */
    public ServerArgs(StreamInput in) throws IOException {
        this(
            in.readBoolean(),
            in.readBoolean(),
            readPidFile(in),
            readSecureSettingsFromStream(in),
            Settings.readSettingsFromStream(in),
            resolvePath(in.readString()),
            resolvePath(in.readString())
        );
    }

    public ServerArgs() throws IOException {
        this(
            false,
            true,
            resolvePath("/Users/mawen/Documents/develop/elasticsearch/elasticsearch.pid"),
            null,
            Settings.readSettingsDefault(),
            resolvePath("/Users/mawen/Documents/develop/elasticsearch/config"),
            resolvePath("/Users/mawen/Documents/develop/elasticsearch/logs")
        );
    }

    private static Path readPidFile(StreamInput in) throws IOException {
        String pidFile = in.readOptionalString();
        return pidFile == null ? null : resolvePath(pidFile);
    }

    @SuppressForbidden(reason = "reading local path from stream")
    private static Path resolvePath(String path) {
        return PathUtils.get(path);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeBoolean(daemonize);
        out.writeBoolean(quiet);
        out.writeOptionalString(pidFile == null ? null : pidFile.toString());
        out.writeString(secrets.getClass().getName());
        secrets.writeTo(out);
        nodeSettings.writeTo(out);
        out.writeString(configDir.toString());
        out.writeString(logsDir.toString());
    }

    private static SecureSettings readSecureSettingsFromStream(StreamInput in) throws IOException {
        String className = in.readString();
        try {
            return (SecureSettings) Class.forName(className).getConstructor(StreamInput.class).newInstance(in);
        } catch (NoSuchMethodException | ClassNotFoundException | InstantiationException | IllegalAccessException
            | InvocationTargetException cfe) {
            throw new IllegalArgumentException("Invalid secrets implementation [" + className + "]", cfe);
        }
    }

    public static Settings emptySettings() throws IOException {
        return Settings.EMPTY;
    }
}
