/*
 * Copyright (c) 2015, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */

package tonivade.db.command.hash;

import static tonivade.db.data.DatabaseKey.safeKey;
import static tonivade.db.data.DatabaseValue.entry;
import static tonivade.db.data.DatabaseValue.hash;

import java.util.HashMap;
import java.util.Map;

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

@Command("hset")
@ParamLength(3)
@ParamType(DataType.HASH)
public class HashSetCommand implements ITinyDBCommand {

    @Override
    public void execute(IDatabase db, IRequest request, IResponse response) {
        DatabaseValue value = hash(entry(request.getParam(1), request.getParam(2)));

        DatabaseValue resultValue = db.merge(safeKey(request.getParam(0)), value,
                (oldValue, newValue) -> {
                    Map<SafeString, SafeString> merge = new HashMap<>();
                    merge.putAll(oldValue.getValue());
                    merge.putAll(newValue.getValue());
                    return hash(merge.entrySet());
                });

        Map<SafeString, SafeString> resultMap = resultValue.getValue();

        response.addInt(resultMap.get(request.getParam(1)) == null);
    }

}
