/*
 * Copyright (c) 2015, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */

package tonivade.db.command.key;

import static org.hamcrest.CoreMatchers.is;
import static tonivade.db.DatabaseKeyMatchers.isNotExpired;
import static tonivade.db.data.DatabaseValue.string;

import org.junit.Rule;
import org.junit.Test;

import tonivade.db.command.CommandRule;
import tonivade.db.command.CommandUnderTest;

@CommandUnderTest(ExpireCommand.class)
public class ExpireCommandTest {

    @Rule
    public final CommandRule rule = new CommandRule(this);

    @Test
    public void testExecute() {
        rule.withData("test", string("value"))
            .withParams("test", "10")
            .execute()
            .assertKey("test", isNotExpired())
            .assertValue("test", is(string("value")))
            .verify().addInt(true);

        rule.withParams("notExists", "10")
            .execute()
            .verify().addInt(false);
    }

}
