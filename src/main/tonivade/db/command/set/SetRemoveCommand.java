/*
 * Copyright (c) 2015, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */

package tonivade.db.command.set;

import static java.util.stream.Collectors.toList;
import static tonivade.db.data.DatabaseKey.safeKey;
import static tonivade.db.data.DatabaseValue.set;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import tonivade.db.command.ITinyDBCommand;
import tonivade.db.command.annotation.ParamType;
import tonivade.db.data.DataType;
import tonivade.db.data.DatabaseValue;
import tonivade.db.data.IDatabase;
import tonivade.redis.annotation.Command;
import tonivade.redis.annotation.ParamLength;
import tonivade.redis.command.IRequest;
import tonivade.redis.command.IResponse;
import tonivade.redis.protocol.SafeString;

@Command("srem")
@ParamLength(2)
@ParamType(DataType.SET)
public class SetRemoveCommand implements ITinyDBCommand {

    @Override
    public void execute(IDatabase db, IRequest request, IResponse response) {
        List<SafeString> items =  request.getParams().stream().skip(1).collect(toList());
        List<SafeString> removed = new LinkedList<>();
        db.merge(safeKey(request.getParam(0)), DatabaseValue.EMPTY_SET,
                (oldValue, newValue) -> {
                    Set<SafeString> merge = new HashSet<>();
                    merge.addAll(oldValue.getValue());
                    for (SafeString item : items) {
                        if (merge.remove(item)) {
                            removed.add(item);
                        }
                    }
                    return set(merge);
                });

        response.addInt(removed.size());
    }

}
