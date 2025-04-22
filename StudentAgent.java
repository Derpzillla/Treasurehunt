import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class StudentAgent {

  // A simple coordinate holder for internal use.
  static class MyCoord {
    int x, y, z;

    MyCoord(int x, int y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }
  }

  // Returns a list of all coordinates on any outer edge.
  private static List<MyCoord> getOuterEdgeCoords(int edge) {
    List<MyCoord> list = new ArrayList<>();
    for (int x = 0; x < edge; x++) {
      for (int y = 0; y < edge; y++) {
        for (int z = 0; z < edge; z++) {
          if (x == 0 || x == edge - 1 || y == 0 || y == edge - 1 || z == 0 || z == edge - 1) {
            list.add(new MyCoord(x, y, z));
          }
        }
      }
    }
    return list;
  }

  // Returns a boolean 3D candidate region for a given hint.
  // A cell is eligible if its Manhattan distance from (centerX, centerY, centerZ)
  // is between low and high (inclusive).
  private static boolean[][][] getHintCandidateRegion(
      int edge, int centerX, int centerY, int centerZ, int low, int high) {
    boolean[][][] region = new boolean[edge][edge][edge];
    for (int x = 0; x < edge; x++) {
      for (int y = 0; y < edge; y++) {
        for (int z = 0; z < edge; z++) {
          int d = Math.abs(x - centerX) + Math.abs(y - centerY) + Math.abs(z - centerZ);
          region[x][y][z] = (d >= low && d <= high);
        }
      }
    }
    return region;
  }

  // Intersect candidateRegion with newRegion so that only cells true in both remain true.
  private static void intersectCandidateRegion(
      boolean[][][] candidateRegion, boolean[][][] newRegion) {
    int edge = candidateRegion.length;
    for (int x = 0; x < edge; x++) {
      for (int y = 0; y < edge; y++) {
        for (int z = 0; z < edge; z++) {
          candidateRegion[x][y][z] = candidateRegion[x][y][z] && newRegion[x][y][z];
        }
      }
    }
  }

  // Helper: Compute the Manhattan distance between two 3D points.
  private static int manhattanDistance(int x1, int y1, int z1, int x2, int y2, int z2) {
    return Math.abs(x1 - x2) + Math.abs(y1 - y2) + Math.abs(z1 - z2);
  }

  // Print out the final hint and goal coordinates using reflection.
  private static void outputFinalLocations(TreasureHunt game, boolean[] hintFound) {
    try {
      Field hintField = TreasureHunt.class.getDeclaredField("hintCoords");
      hintField.setAccessible(true);
      Object[] hints = (Object[]) hintField.get(game);
      System.out.println("----- Hint Coordinates -----");
      for (int i = 0; i < hints.length; i++) {
        System.out.println(hints[i] + " Found: " + hintFound[i]);
      }

      Field goalField = TreasureHunt.class.getDeclaredField("goalCoord");
      goalField.setAccessible(true);
      Object goalCoord = goalField.get(game);
      System.out.println("----- Goal Coordinate -----");
      System.out.println(goalCoord);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Print the trial result including a computed "score" for this trial.
  // For a single trial, if the goal was found, we compute score = 100000/(steps+1), else 0.



  private static void printTrialResult(TreasureHunt.TrialResult result) {
    System.out.println("Trial: Found=" + result.foundGoal + ", Steps=" + result.steps);
    //double trialScore = result.foundGoal ? 100000.0 / (result.steps + 1) : 0;
    //System.out.printf("Score: %.3f!\n", trialScore);
  }
  
  public static TreasureHunt.TrialResult run(TreasureHunt game) {
    int edge = game.edgeLength;
    boolean[][][] visited = new boolean[edge][edge][edge];
    boolean[][][] candidateRegion = new boolean[edge][edge][edge];
    boolean[] hintFound = new boolean[9];
    // Initially, every cell is a candidate.
    for (int x = 0; x < edge; x++)
      for (int y = 0; y < edge; y++)
        for (int z = 0; z < edge; z++) candidateRegion[x][y][z] = true;

    // Ask user how many hints to use (0 to 9).
    // Scanner scanner = new Scanner(System.in);
    int maxHints = 1;

    int hintCount = 0;

    // Phase 1: Traverse outer edge cells until the first hint is found.
    List<MyCoord> outerEdges = getOuterEdgeCoords(edge);
    for (MyCoord coord : outerEdges) {
      if (visited[coord.x][coord.y][coord.z]) continue;
      game.jumpTo(coord.x, coord.y, coord.z);
      visited[coord.x][coord.y][coord.z] = true;
      TreasureHunt.Collectible item = game.search();
      if (item != null) {
        if (!item.isHint()) { // Goal found directly.
          TreasureHunt.TrialResult result = game.submit();
          outputFinalLocations(game, hintFound);
          printTrialResult(result);
          return result;
        } else {
          hintCount++;
          Object[] hintMsg = item.getMessage();
          int[] goalRange = (int[]) hintMsg[1];
          int hintLow = goalRange[0];
          int hintHigh = goalRange[1];
          boolean[][][] hintRegion =
              getHintCandidateRegion(edge, coord.x, coord.y, coord.z, hintLow, hintHigh);
          intersectCandidateRegion(candidateRegion, hintRegion);
          hintFound[hintCount - 1] = true;
          break; // Stop outer edge scan after first hint.
        }
      }
    }

    // Phase 2: Continue scanning for additional hints until the desired count is reached.
    if (hintCount < maxHints) {
      for (int z = 0; z < edge && hintCount < maxHints; z++) {
        for (int y = 0; y < edge && hintCount < maxHints; y++) {
          for (int x = 0; x < edge && hintCount < maxHints; x++) {
            if (visited[x][y][z]) continue;
            game.jumpTo(x, y, z);
            visited[x][y][z] = true;
            TreasureHunt.Collectible item = game.search();
            if (item != null) {
              if (!item.isHint()) {
                TreasureHunt.TrialResult result = game.submit();
                outputFinalLocations(game, hintFound);
                printTrialResult(result);
                
                return result;
              } else {
                hintCount++;
                Object[] hintMsg = item.getMessage();
                int[] goalRange = (int[]) hintMsg[1];
                int hintLow = goalRange[0];
                int hintHigh = goalRange[1];
                boolean[][][] hintRegion =
                    getHintCandidateRegion(edge, x, y, z, hintLow, hintHigh);
                intersectCandidateRegion(candidateRegion, hintRegion);
                hintFound[hintCount - 1] = true;
                if (hintCount >= maxHints) {
                  break; // Stop scanning for hints if maxHints is reached
                }
              }
            }
          }
          if (hintCount >= maxHints) {
            break; // Stop scanning for hints if maxHints is reached
          }
        }
        if (hintCount >= maxHints) {
          break; // Stop scanning for hints if maxHints is reached
        }
      }
    }

   // Phase 3: Traverse the candidate region (cells not yet visited) to search for the goal.
List<MyCoord> candidateCells = new ArrayList<>();
for (int x = 0; x < edge; x++) {
  for (int y = 0; y < edge; y++) {
    for (int z = 0; z < edge; z++) {
      if (candidateRegion[x][y][z] && !visited[x][y][z])
        candidateCells.add(new MyCoord(x, y, z));
    }
  }
}

// If candidateCells size is less than 50, force search for the goal
if (candidateCells.size() < 100) {
  for (MyCoord coord : candidateCells) {
    game.jumpTo(coord.x, coord.y, coord.z);
    visited[coord.x][coord.y][coord.z] = true;
    TreasureHunt.Collectible item = game.search();
    if (item != null && !item.isHint()) {
      TreasureHunt.TrialResult result = game.submit();
      outputFinalLocations(game, hintFound);
      printTrialResult(result);
      System.out.println("Candidate Cells Length: " + candidateCells.size()); // Print the size
      return result;
    }
  }
}

    // Optimization: Sort candidate cells by their Manhattan distance from the current position.
    TreasureHunt.Coordinate currentPos = game.position();
    final int currX = currentPos.x();
    final int currY = currentPos.y();
    final int currZ = currentPos.z();
    Collections.sort(
        candidateCells,
        new Comparator<MyCoord>() {
          public int compare(MyCoord a, MyCoord b) {
            int da = manhattanDistance(currX, currY, currZ, a.x, a.y, a.z);
            int db = manhattanDistance(currX, currY, currZ, b.x, b.y, b.z);
            return Integer.compare(da, db);
          }
        });

    // Traverse the sorted candidate cells.
    for (MyCoord coord : candidateCells) {
      game.jumpTo(coord.x, coord.y, coord.z);
      visited[coord.x][coord.y][coord.z] = true;
      TreasureHunt.Collectible item = game.search();
      if (item != null && !item.isHint()) {
        TreasureHunt.TrialResult result = game.submit();
        outputFinalLocations(game, hintFound);
        printTrialResult(result);
        System.out.println("Candidate Cells Length: " + candidateCells.size()); // Print the size
        return result;
      }
    }

    // Finally, submit if all else fails.
    TreasureHunt.TrialResult result = game.submit();
    outputFinalLocations(game, hintFound);
    printTrialResult(result);
    System.out.println("Candidate Cells Length: " + candidateCells.size()); // Print the size of candidate cells
    return result;
  }
}