package net.hachimi.client.impl.manager;

import net.hachimi.client.impl.event.FinishLoadingEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.init.Fonts;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;

public class EventManager {
   public EventManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onGameFinishedInit(FinishLoadingEvent event) {
      Fonts.init();
   }

   @EventListener
   public void onReloadShader(RenderWorldEvent.Hand event) {
      Managers.SHADER.reloadShaders();
   }
}
