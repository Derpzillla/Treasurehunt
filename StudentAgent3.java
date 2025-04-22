import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StudentAgent3 {

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

  // Print the trial result.
  private static void printTrialResult(TreasureHunt.TrialResult result) {
    System.out.println("Trial: Found=" + result.foundGoal + ", Steps=" + result.steps);
  }

  // Helper function to get the size of the candidate cells
  private static int getCandidateCellsSize(boolean[][][] candidateRegion, boolean[][][] visited) {
    int edge = candidateRegion.length;
    int count = 0;
    for (int x = 0; x < edge; x++) {
      for (int y = 0; y < edge; y++) {
        for (int z = 0; z < edge; z++) {
          if (candidateRegion[x][y][z] && !visited[x][y][z]) {
            count++;
          }
        }
      }
    }
    return count;
  }

  // Helper function to get the list of candidate cells
  private static List<MyCoord> getCandidateCells(boolean[][][] candidateRegion, boolean[][][] visited) {
    int edge = candidateRegion.length;
    List<MyCoord> candidateCells = new ArrayList<>();
    for (int x = 0; x < edge; x++) {
      for (int y = 0; y < edge; y++) {
        for (int z = 0; z < edge; z++) {
          if (candidateRegion[x][y][z] && !visited[x][y][z]) {
            candidateCells.add(new MyCoord(x, y, z));
          }
        }
      }
    }
    return candidateCells;
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
          System.out.println("Candidate Cells Length: " + getCandidateCellsSize(candidateRegion, visited));
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

    // Combined Phase 2 & 3: Seek hints until candidate space <= 100, then search for goal
    boolean seekingHints = true;
    while (seekingHints) {
      int candidateSize = getCandidateCellsSize(candidateRegion, visited);
      if (candidateSize <= 100) {
        seekingHints = false; // Candidate space is small enough, stop seeking hints
        break;
      }

      boolean foundHint = false;
      for (int z = 0; z < edge; z++) {
        for (int y = 0; y < edge; y++) {
          for (int x = 0; x < edge; x++) {
            if (visited[x][y][z]) continue;
            game.jumpTo(x, y, z);
            visited[x][y][z] = true;
            TreasureHunt.Collectible item = game.search();
            if (item != null) {
              if (!item.isHint()) {
                TreasureHunt.TrialResult result = game.submit();
                outputFinalLocations(game, hintFound);
                printTrialResult(result);
                System.out.println("Candidate Cells Length: " + getCandidateCellsSize(candidateRegion, visited));
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
                foundHint = true;
                break; // Found a hint, break to re-evaluate candidate space size
              }
            }
          }
          if (foundHint) break;
        }
        if (foundHint) break;
      }
      if (!foundHint) seekingHints = false; // No more hints to be found
    }

    // Search for the goal within the candidate region
    List<MyCoord> candidateCells = getCandidateCells(candidateRegion, visited);

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
        System.out.println("Candidate Cells Length: " + getCandidateCellsSize(candidateRegion, visited));
        return result;
      }
    }

    // Finally, submit if all else fails.
    TreasureHunt.TrialResult result = game.submit();
    outputFinalLocations(game, hintFound);
    printTrialResult(result);
    System.out.println("Candidate Cells Length: " + getCandidateCellsSize(candidateRegion, visited));
    return result;
  }
}