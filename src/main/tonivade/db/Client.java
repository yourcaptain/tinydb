/*
 * Copyright (c) 2015, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */

package tonivade.db;

import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import tonivade.redis.IRedisCallback;
import tonivade.redis.RedisClient;
import tonivade.redis.protocol.RedisToken;

public class Client implements IRedisCallback {

    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

    private static final String CHARSET_NAME = "UTF-8";
    private static final String QUIT = "quit";
    private static final String END_OF_LINE = "\r\n";
    private static final String PROMPT = "> ";

    private final BlockingQueue<RedisToken> responses = new ArrayBlockingQueue<>(1);

    @Override
    public void onConnect() {
        System.out.println("connected!");
    }

    @Override
    public void onDisconnect() {
        System.out.println("disconnected!");
    }

    @Override
    public void onMessage(RedisToken token) {
        try {
            responses.put(token);
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "message not processed", e);
        }
    }

    public RedisToken response() throws InterruptedException {
        return responses.take();
    }

    public static void main(String[] args) throws Exception {
        OptionParser parser = new OptionParser();
        OptionSpec<Void> help = parser.accepts("help", "print help");
        OptionSpec<String> host = parser.accepts("h", "host").withRequiredArg().ofType(String.class)
                .defaultsTo(TinyDB.DEFAULT_HOST);
        OptionSpec<Integer> port = parser.accepts("p", "port").withRequiredArg().ofType(Integer.class)
                .defaultsTo(TinyDB.DEFAULT_PORT);

        OptionSet options = parser.parse(args);

        if (options.has(help)) {
            parser.printHelpOn(System.out);
        } else {
            Client callback = new Client();

            String optionHost = options.valueOf(host);
            int optionPort = parsePort(options.valueOf(port));
            RedisClient client = new RedisClient(optionHost, optionPort, callback);
            client.start();

            prompt();
            try (Scanner scanner = new Scanner(System.in, CHARSET_NAME)) {
                for (boolean quit = false; !quit && scanner.hasNextLine(); prompt()) {
                    String line = scanner.nextLine();
                    if (!line.isEmpty()) {
                        client.send(line + END_OF_LINE);
                        System.out.println(callback.response());
                        quit = line.equalsIgnoreCase(QUIT);
                    }
                }
            } finally {
                client.stop();
            }
        }
    }

    private static void prompt() {
        System.out.print(PROMPT);
    }

    private static int parsePort(Integer optionPort) {
        return optionPort != null ? optionPort : ITinyDB.DEFAULT_PORT;
    }

}
