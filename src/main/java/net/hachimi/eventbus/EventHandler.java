package net.hachimi.eventbus;

import net.hachimi.eventbus.event.Event;

public interface EventHandler {
   void subscribe(Object var1);

   void unsubscribe(Object var1);

   boolean dispatch(Event var1);
}
