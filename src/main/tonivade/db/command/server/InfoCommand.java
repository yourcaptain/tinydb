/*
 * Copyright (c) 2015, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */

package tonivade.db.command.server;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static tonivade.redis.protocol.SafeString.safeString;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import tonivade.db.command.ITinyDBCommand;
import tonivade.db.command.annotation.ReadOnly;
import tonivade.db.data.DatabaseValue;
import tonivade.db.data.IDatabase;
import tonivade.redis.annotation.Command;
import tonivade.redis.command.IRequest;
import tonivade.redis.command.IResponse;
import tonivade.redis.command.IServerContext;
import tonivade.redis.protocol.SafeString;

@ReadOnly
@Command("info")
public class InfoCommand implements ITinyDBCommand {

    private static final String SHARP = "#";
    private static final String SEPARATOR = ":";
    private static final String DELIMITER = "\r\n";

    private static final String SECTION_KEYSPACE = "keyspace";
    private static final String SECTION_COMMANDSTATS = "commandstats";
    private static final String SECTION_CPU = "cpu";
    private static final String SECTION_STATS = "stats";
    private static final String SECTION_PERSISTENCE = "persistence";
    private static final String SECTION_MEMORY = "memory";
    private static final String SECTION_CLIENTS = "clients";
    private static final String SECTION_REPLICATION = "replication";
    private static final String SECTION_SERVER = "server";

    @Override
    public void execute(IDatabase db, IRequest request, IResponse response) {
        Map<String, Map<String, String>> sections = new HashMap<>();
        Optional<SafeString> param = request.getOptionalParam(0);
        if (param.isPresent()) {
            String sectionName = param.get().toString();
            sections.put(sectionName, section(sectionName, request.getServerContext()));
        } else {
            for (String section : allSections()) {
                sections.put(section, section(section, request.getServerContext()));
            }
        }
        response.addBulkStr(safeString(makeString(sections)));
    }

    private String makeString(Map<String, Map<String, String>> sections) {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, Map<String, String>> section : sections.entrySet()) {
            sb.append(SHARP).append(section.getKey()).append(DELIMITER);
            for (Entry<String, String> entry : section.getValue().entrySet()) {
                sb.append(entry.getKey()).append(SEPARATOR).append(entry.getValue()).append(DELIMITER);
            }
            sb.append(DELIMITER);
        }
        sb.append(DELIMITER);
        return sb.toString();
    }

    private List<String> allSections() {
        return asList(SECTION_SERVER, SECTION_REPLICATION, SECTION_CLIENTS,
                SECTION_MEMORY, SECTION_PERSISTENCE, SECTION_STATS, SECTION_CPU,
                SECTION_COMMANDSTATS, SECTION_KEYSPACE);
    }

    private Map<String, String> section(String section, IServerContext ctx) {
        switch (section.toLowerCase()) {
        case SECTION_SERVER:
            return server(ctx);
        case SECTION_REPLICATION:
            return replication(ctx);
        case SECTION_CLIENTS:
            return clients(ctx);
        case SECTION_MEMORY:
            return memory(ctx);
        case SECTION_PERSISTENCE:
            return persistence(ctx);
        case SECTION_STATS:
            return stats(ctx);
        case SECTION_CPU:
            return cpu(ctx);
        case SECTION_COMMANDSTATS:
            return commandstats(ctx);
        case SECTION_KEYSPACE:
            return keyspace(ctx);
        default:
            break;
        }
        return null;
    }

    private Map<String, String> server(IServerContext ctx) {
        return map(entry("redis_version", "2.8.21"),
                entry("tcp_port", valueOf(ctx.getPort())));
    }

    private Map<String, String> replication(IServerContext ctx) {
        return map(entry("role", getServerState(ctx).isMaster() ? "master" : "slave"),
                entry("connected_slaves", slaves(ctx)));
    }

    private String slaves(IServerContext ctx) {
        DatabaseValue slaves = getAdminDatabase(ctx).getOrDefault("slaves", DatabaseValue.EMPTY_SET);
        return String.valueOf(slaves.<Set<String>>getValue().size());
    }

    private Map<String, String> clients(IServerContext ctx) {
        return map(entry("connected_clients", valueOf(ctx.getClients())));
    }

    private Map<String, String> memory(IServerContext ctx) {
        return map(entry("used_memory", valueOf(Runtime.getRuntime().totalMemory())));
    }

    private Map<String, String> persistence(IServerContext ctx) {
        // TODO:
        return map();
    }

    private Map<String, String> stats(IServerContext ctx) {
        // TODO:
        return map();
    }

    private Map<String, String> cpu(IServerContext ctx) {
        // TODO:
        return map();
    }

    private Map<String, String> commandstats(IServerContext ctx) {
        // TODO:
        return map();
    }

    private Map<String, String> keyspace(IServerContext ctx) {
        // TODO:
        return map();
    }

    @SafeVarargs
    private static Map<String, String> map(Entry<String, String> ... values) {
        return Stream.of(values).collect(toMap(Entry::getKey, Entry::getValue));
    }

    public static Entry<String, String> entry(String key, String value) {
        return new SimpleEntry<>(key, value);
    }
}
