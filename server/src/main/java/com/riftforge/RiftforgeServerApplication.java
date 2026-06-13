package com.riftforge;

import com.riftforge.model.move.*;
import com.riftforge.model.MatchRecord;
import com.riftforge.model.PendingChoice;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@ImportRuntimeHints(RiftforgeServerApplication.Hints.class)
public class RiftforgeServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(RiftforgeServerApplication.class, args);
  }

  static class Hints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      MemberCategory[] all = MemberCategory.values();
      for (Class<?> type : new Class<?>[] {
          MoveRequest.class,
          DealCardMove.class, TapCardMove.class, FlipCardMove.class,
          PlayCardMove.class, MoveCardMove.class, RepositionCardMove.class, TapRuneMove.class,
          DiscardRuneMove.class, MoveToBattlefieldMove.class,
          MulliganMove.class, UndoRunesMove.class, PassPhaseMove.class, ResolveShowdownMove.class, AdjustScoreMove.class, VisionChoiceMove.class,
          DismissRevealedMove.class, HideCardMove.class, EquipGearMove.class, ResolveChoiceMove.class,
          PendingChoice.class, PendingChoice.ChoiceOption.class, PendingChoice.CardChoiceOption.class, PendingChoice.CardChoiceAssignment.class,
          MatchRecord.class, MatchRecord.PlayerSummary.class
      }) {
        hints.reflection().registerType(type, all);
      }
    }
  }
}
