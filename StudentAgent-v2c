public class StudentAgent {
  public static boolean[][][] goalCandidate;
  public static boolean[][][] hintCandidate;
  public static int myStepCount;

  public static TreasureHunt.TrialResult run(TreasureHunt game) {
    // Your agent's logic here, using game.jumpTo, move..., search, etc.
    // Must call game.submit() at the end.
    // DO NOT create a TrialResult manually — they'll get a compiler error.

    myStepCount = 0;
    int[] hint = lookForHint(game);
    if(hint[0] == -1) return game.submit();
    TreasureHunt.Collectible item = game.search();
    if(!item.isHint()) {
      return game.submit();
    }

    Object[] hintMessage = item.getMessage();
    int hintDistance = (int) hintMessage[0];
    int[] goalRange = (int[]) hintMessage[1];
    int goalMin = goalRange[0];
    int goalMax = goalRange[1];

    //Check if each space within the cube is in the goalCandidate region given by the hint
    for(int x = 0; x < 20; x++) {
      for(int y = 0; y < 20; y++) {
        for(int z = 0; z < 20; z++) {
          int manDist = Math.abs(hint[0] - x) + Math.abs(hint[1] - y) + Math.abs(hint[2] - z);
          if((manDist < goalMin) || (manDist > goalMax)) {
            goalCandidate[x][y][z] = false;
          }
        }
      }
    }


    //This section was added from version 1: it check for a second hint before checking the goal region
    //if the hintDistance given was 3 or less. 
    int[] hint2 = {-1, -1, -1};
    if(hintDistance <= 3) {
      for(int x = 0; x < 20; x++) {
        for(int y = 0; y < 20; y++) {
          for(int z = 0; z < 20; z++) {
            int manDist = Math.abs(hint[0] - x) + Math.abs(hint[1] - y) + Math.abs(hint[2] - z);
            if((manDist == hintDistance) && (hintCandidate[x][y][z])) {
              if(jump(x, y, z, game)) return game.submit();
              if(game.search() != null) {
                if(!game.search().isHint()) {
                  return game.submit();
                } else {
                  //Found a hint
                  TreasureHunt.Collectible newHint = game.search();
                  hint2[0] = x;
                  hint2[1] = y;
                  hint2[2] = z;
                  updateGoalRegion(newHint, hint2);
                  break;
                }
              }
              goalCandidate[x][y][z] = false;
              hintCandidate[x][y][z] = false;
            }
          }
          if(hint2[0] != -1) break;
        }
        if(hint2[0] != -1) break;
      }
      if(hint2[0] == -1) System.out.println("\n\n\n\n\n\n\n\nSomething broke...");
    }

    //If hint distance falls within goal region, check that first
    for(int x = 0; x < 20; x++) {
      for(int y = 0; y < 20; y++) {
        for(int z = 0; z < 20; z++) {
          int manDist = Math.abs(hint[0] - x) + Math.abs(hint[1] - y) + Math.abs(hint[2] - z);
          if((manDist == hintDistance) && (goalCandidate[x][y][z])) {
            if(jump(x, y, z, game)) return game.submit();
            if(game.search() != null) {
              if(!game.search().isHint()) {
                return game.submit();
              } else {
                //Found a hint
                TreasureHunt.Collectible newHint = game.search();
                int[] hintCoord = {x, y, z};
                updateGoalRegion(newHint, hintCoord);
              }
            }
            goalCandidate[x][y][z] = false;
            hintCandidate[x][y][z] = false;
          }
        }
      }
    }

    //Check if each space in the goalCandidate region contains the goal
    for(int x = 0; x < 20; x++) {
      for(int y = 0; y < 20; y++) {
        for(int z = 0; z < 20; z++) {
          if(goalCandidate[x][y][z]) {
            if(jump(x, y, z, game)) return game.submit();
            if(game.search() != null) {
              if(!game.search().isHint()) {
                return game.submit();
              } else {
                //Found a hint
                TreasureHunt.Collectible newHint = game.search();
                int[] hintCoord = {x, y, z};
                updateGoalRegion(newHint, hintCoord);
              }
            }
            goalCandidate[x][y][z] = false;
            hintCandidate[x][y][z] = false;
          }
        }
      }
    }

    return game.submit(); // fallback (goal always exists)
  }

  public static int[] lookForHint(TreasureHunt game) {
    goalCandidate = new boolean[20][20][20];
    hintCandidate = new boolean[20][20][20];
    for(int i = 0; i < 20; i++) {
      for(int j = 0; j < 20; j++) {
        for(int k = 0; k < 20; k++) {
          goalCandidate[i][j][k] = true;
          hintCandidate[i][j][k] = true;
        }
      }
    }

    // Traverse edges first, spiral inwards to elimate nodes
    int[] hint = {-1, -1, -1};
    
    for(int offset = 0; offset <= 10; offset++) {
      for(int i = (0 + offset); i < (20 - offset); i++) {
        for(int j = (0 + offset); j < (20 - offset); j++) {
          if(jump(j, i, offset, game)) return hint;
          goalCandidate[j][i][offset] = false;
          hintCandidate[j][i][offset] = false;
          if(game.search() != null) {
            hint[0] = j;
            hint[1] = i;
            hint[2] = offset;
            return hint;
          }
        }
      }
  
      for(int i = (1 + offset); i < (20 - offset); i++) {
        for(int j = (0 + offset); j < (20 - offset); j++) {
          if(jump(j, offset, i, game)) return hint;
          goalCandidate[j][offset][i] = false;
          hintCandidate[j][offset][i] = false;
          if(game.search() != null) {
            hint[0] = j;
            hint[1] = offset;
            hint[2] = i;
            return hint;
          }
        }
      }
  
      for(int i = (1 + offset); i < (20 - offset); i++) {
        for(int j = (1 + offset); j < ( 20 - offset); j++) {
          if(jump(offset, i, j, game)) return hint;
          goalCandidate[offset][i][j] = false;
          hintCandidate[offset][i][j] = false;
          if(game.search() != null) {
            hint[0] = offset;
            hint[1] = i;
            hint[2] = j;
            return hint;
          }
        }
      }
  
      for(int i = (19 - offset); i > (0 + offset); i--) {
        for(int j = (19 - offset); j > (0 + offset); j--) {
          if(jump((19 - offset), i, j, game)) return hint;
          goalCandidate[(19 - offset)][i][j] = false;
          hintCandidate[(19 - offset)][i][j] = false;
          if(game.search() != null) {
            hint[0] = (19 - offset);
            hint[1] = i;
            hint[2] = j;
            return hint;
          }
        }
      }
  
      for(int i = (19 - offset); i > (0 + offset); i--) {
        for(int j = (18 - offset); j > (0 + offset); j--) {
          if(jump(i, (19 - offset), j, game)) return hint;
          goalCandidate[i][(19 - offset)][j] = false;
          hintCandidate[i][(19 - offset)][j] = false;
          if(game.search() != null) {
            hint[0] = i;
            hint[1] = (19 - offset);
            hint[2] = j;
            return hint;
          }
        }
      }
  
      for(int i = (18 - offset); i > (0 + offset); i--) {
        for(int j = (18 - offset); j > (0 + offset); j--) {
          if(jump((19 - offset), i, j, game)) return hint;
          goalCandidate[i][j][(19 - offset)] = false;
          hintCandidate[i][j][(19 - offset)] = false;
          if(game.search() != null) {
            hint[0] = i;
            hint[1] = j;
            hint[2] = (19 - offset);
            return hint;
          }
        }
      }
    }

    return hint;
  }

  public static void updateGoalRegion(TreasureHunt.Collectible hint, int[] hintCoord) {
    Object[] hintMessage = hint.getMessage();
    int[] goalRange = (int[]) hintMessage[1];
    int goalMin = goalRange[0];
    int goalMax = goalRange[1];

    //Check if each space within the cube is in the goalCandidate region given by the hint
    for(int x = 0; x < 20; x++) {
      for(int y = 0; y < 20; y++) {
        for(int z = 0; z < 20; z++) {
          int manDist = Math.abs(hintCoord[0] - x) + Math.abs(hintCoord[1] - y) + Math.abs(hintCoord[2] - z);
          if((manDist < goalMin) || (manDist > goalMax)) {
            goalCandidate[x][y][z] = false;
          }
        }
      }
    }
  }

  //Use this method to call game.jumpTo and increase myStepCount
  //If myStepCount exceeds 2000, return true and call game.submit to end trial run
  public static boolean jump(int x, int y, int z, TreasureHunt game) {
    game.jumpTo(x, y, z);
    myStepCount++;
    if(myStepCount > 2000) return true;
    return false;
  }

}
