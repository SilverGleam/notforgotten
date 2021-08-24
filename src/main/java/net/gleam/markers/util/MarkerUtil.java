package net.gleam.markers.util;

public class MarkerUtil {
  public static int calculateTotalExperience(int level, float progress) {
    int levelSquared = level * level; // needed for equations
    int levelExperience = 0;
    int progressExperience = 0;

    // the following equations should all equate to ints
    // https://minecraft.fandom.com/wiki/Experience#Leveling_up
    if (16 >= level) {
      levelExperience = (int) (levelSquared + 6 * level);
      progressExperience = (int) ((2 * level + 7) * progress);
    }

    if (31 >= level) {
      levelExperience = (int) (2.5 * levelSquared - 40.5 * level + 360);
      progressExperience = (int) ((5 * level - 38) * progress);
    }

    if (level >= 32) {
      levelExperience = (int) (4.5 * levelSquared - 162.5 * level + 2220);
      progressExperience = (int) ((9 * level - 158) * progress);
    }

    return levelExperience + progressExperience;
  }
}
