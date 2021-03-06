package tonivade.db;

import java.util.HashSet;
import java.util.Set;

import tonivade.redis.protocol.SafeString;

public class TinyDBSessionState {

    private int db;

    private final Set<SafeString> subscriptions = new HashSet<>();

    public int getCurrentDB() {
        return db;
    }

    public void setCurrentDB(int db) {
        this.db = db;
    }

    public Set<SafeString> getSubscriptions() {
        return subscriptions;
    }

    public void addSubscription(SafeString channel) {
        subscriptions.add(channel);
    }

    public void removeSubscription(SafeString channel) {
        subscriptions.remove(channel);
    }

    public boolean isSubscribed() {
        return !subscriptions.isEmpty();
    }

}
