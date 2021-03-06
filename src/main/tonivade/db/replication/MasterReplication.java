/*
 * Copyright (c) 2015, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */

package tonivade.db.replication;

import static java.util.stream.Collectors.toList;
import static tonivade.db.data.DatabaseKey.safeKey;
import static tonivade.db.data.DatabaseValue.set;
import static tonivade.redis.protocol.RedisToken.array;
import static tonivade.redis.protocol.RedisToken.string;
import static tonivade.redis.protocol.SafeString.safeString;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import tonivade.db.ITinyDB;
import tonivade.db.TinyDBServerState;
import tonivade.db.data.DatabaseKey;
import tonivade.db.data.DatabaseValue;
import tonivade.db.data.IDatabase;
import tonivade.redis.protocol.RedisToken;
import tonivade.redis.protocol.SafeString;

public class MasterReplication implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(MasterReplication.class.getName());

    private static final String SELECT_COMMAND = "SELECT";
    private static final String PING_COMMAND = "PING";

    private static final DatabaseKey SLAVES_KEY = safeKey(safeString("slaves"));

    private static final int TASK_DELAY = 2;

    private final ITinyDB server;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public MasterReplication(ITinyDB server) {
        this.server = server;
    }

    public void start() {
        executor.scheduleWithFixedDelay(this, TASK_DELAY, TASK_DELAY, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.shutdown();
    }

    public void addSlave(String id) {
        getAdminDatabase().merge(SLAVES_KEY, set(safeString(id)), (oldValue, newValue) -> {
            List<SafeString> merge = new LinkedList<>();
            merge.addAll(oldValue.getValue());
            merge.addAll(newValue.getValue());
            return set(merge);
        });
        LOGGER.info(() -> "new slave: " + id);
    }

    public void removeSlave(String id) {
        getAdminDatabase().merge(SLAVES_KEY, set(safeString(id)), (oldValue, newValue) -> {
            List<SafeString> merge = new LinkedList<>();
            merge.addAll(oldValue.getValue());
            merge.removeAll(newValue.getValue());
            return set(merge);
        });
        LOGGER.info(() -> "slave revomed: " + id);
    }

    @Override
    public void run() {
        List<RedisToken> commands = createCommands();

        for (SafeString slave : getSlaves()) {
            for (RedisToken command : commands) {
                server.publish(slave.toString(), command);
            }
        }
    }

    private IDatabase getAdminDatabase() {
        return getServerState().getAdminDatabase();
    }

    private Set<SafeString> getSlaves() {
        return getAdminDatabase().getOrDefault(SLAVES_KEY, DatabaseValue.EMPTY_SET).getValue();
    }

    private List<RedisToken> createCommands() {
        List<RedisToken> commands = new LinkedList<>();
        commands.add(pingCommand());
        commands.addAll(commandsToReplicate());
        return commands;
    }

    private List<RedisToken> commandsToReplicate() {
        List<RedisToken> commands = new LinkedList<>();

        for (List<RedisToken> command : server.getCommandsToReplicate()) {
            commands.add(selectCommand(command.get(0)));
            commands.add(array(command.stream().skip(1).collect(toList())));
        }
        return commands;
    }

    private RedisToken selectCommand(RedisToken database) {
        return array(string(SELECT_COMMAND), database);
    }

    private RedisToken pingCommand() {
        return array(string(PING_COMMAND));
    }

    private TinyDBServerState getServerState() {
        return server.getValue("state");
    }

}
