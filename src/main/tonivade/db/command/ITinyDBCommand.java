package tonivade.db.command;

import tonivade.db.ITinyDB;
import tonivade.db.TinyDBServerState;
import tonivade.db.TinyDBSessionState;
import tonivade.db.data.IDatabase;
import tonivade.redis.command.IRequest;
import tonivade.redis.command.IResponse;
import tonivade.redis.command.IServerContext;
import tonivade.redis.command.ISession;

public interface ITinyDBCommand {

    void execute(IDatabase db, IRequest request, IResponse response);

    default ITinyDB getTinyDB(IServerContext server) {
        return (ITinyDB) server;
    }

    default IDatabase getAdminDatabase(IServerContext server) {
        return getServerState(server).getAdminDatabase();
    }

    default TinyDBServerState getServerState(IServerContext server) {
        return server.getValue("state");
    }

    default TinyDBSessionState getSessionState(ISession session) {
        return session.getValue("state");
    }

}
