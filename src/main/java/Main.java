import eu.printingin3d.javascad.basic.Radius;
import eu.printingin3d.javascad.context.ITagColors;
import eu.printingin3d.javascad.context.TagColorsBuilder;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.enums.Side;
import eu.printingin3d.javascad.models.Abstract3dModel;
import eu.printingin3d.javascad.models.Cube;
import eu.printingin3d.javascad.models.IModel;
import eu.printingin3d.javascad.models.Sphere;
import eu.printingin3d.javascad.tranzitions.Difference;
import eu.printingin3d.javascad.tranzitions.Union;
import eu.printingin3d.javascad.utils.SaveScadFiles;
import eu.printingin3d.javascad.vrl.Facet;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;
import eu.printingin3d.javascad.vrl.export.FileExporterFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;

public class Main {
    public Main() {
    }

    public static void main(String[] args) throws IOException {

        int cellAmount = 9;

        int xSize = cellAmount;
        int ySize = cellAmount;
        int zSize = 2;

        boolean doCubes = true;
        boolean doBall = true;
        double scale = 3.5;
        double size = 10 * scale;
        double thickness = 0.75 * scale;
        double gap = 6 * scale;
        double width = 2 * scale; //1.75
        int startX = 1;
        int startY = 1;

        CubeCell[][][] cellMatrix = new CubeCell[xSize][ySize][zSize];

        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    CubeCell cubeCell = new CubeCell(size, thickness, gap, width);
                    cellMatrix[x][y][z] = cubeCell;
                }
            }
        }

        cellMatrix[xSize - 1 - startX][ySize - 1 - startY][1].finish();

        HashSet<CubeCell> visitedCells = new HashSet<>();
        int[] currentPos = new int[]{startX, startY, 0};
        CubeCell currentCube = cellMatrix[currentPos[0]][currentPos[1]][currentPos[2]];
        currentCube.start();
        visitedCells.add(currentCube);
        int[][][] nextPairings = moveAsXYZPair(currentPos, xSize, ySize, zSize);
        Stack<int[][]> pairings = new Stack<>();

        pairings.addAll(Arrays.asList(nextPairings));

        while (!pairings.empty()) {
            int[][] currentPairing = pairings.pop();
            if (visitedCells.size() == xSize * ySize * zSize) {
                pairings.removeAllElements();
                break;
            }
            currentPos = currentPairing[0];
            currentCube = cellMatrix[currentPos[0]][currentPos[1]][currentPos[2]];
            int[] nextPos = currentPairing[2];
            CubeCell nextCube = cellMatrix[nextPos[0]][nextPos[1]][nextPos[2]];

            int visits = nextCube.visited();

            if (nextCube.visited() * 4 < currentCube.visited()) {
                int[] direction = currentPairing[1];
                int[] directionFlipped = flippedDirection(direction);
                currentCube.open(direction);
                nextCube.open(directionFlipped);
                visitedCells.add(nextCube);
            }

            if (visits > 0) {
                continue;
            }

            nextPairings = moveAsXYZPair(nextPos, xSize, ySize, zSize);
            pairings.addAll(Arrays.asList(nextPairings));
            Collections.shuffle(pairings);
        }

        clrscr();
        double progress;
        List<Abstract3dModel> cubes = new ArrayList<>();
        for (int x = 0; x < xSize && doCubes; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    CubeCell cubeCell = cellMatrix[x][y][z];
                    cubes.add(cubeCell.makeCubeModel()
                        .move(new Coords3d(x * size + x * 0.0001, y * size + y * 0.0001, z * size + z * 0.0001)));
                }
                progress = ((x * ySize * zSize + y * zSize) / (xSize * ySize * zSize * 1.0)) * 100d;
                clrscr();
                System.out.println(("cubeModel@" + NumberFormat.getPercentInstance().format(progress / 100)));

            }
        }


        double diameter = size - (thickness * 3);
        double dent = 0.15 * diameter;
        Coords3d sphereMovement = new Coords3d(size * (xSize - startX - 1), size * (ySize - startY - 1), 0d);
        Abstract3dModel sphere = new Difference(new Sphere(Radius.fromDiameter(diameter)), new Cube(diameter)
            .move(Coords3d.zOnly(-diameter + dent)))
            .move(new Coords3d(0, 0, -(dent) - thickness))
            .move(sphereMovement);
        Abstract3dModel sphereSupport = new Cube(diameter * 0.5).align(Side.BOTTOM_IN_CENTER, cubes.get(0).move(sphereMovement));
        sphere = new Union(sphere, sphereSupport);
        if (doBall)
            cubes.add(sphere);

        ITagColors tagColors = (new TagColorsBuilder()).addTag(1, new Color(139, 90, 43)).addTag(2, Color.GRAY).buildTagColors();
        FacetGenerationContext generationContext = new FacetGenerationContext(tagColors, null, 0);
        List<Facet> facets = new ArrayList<>();
        for (int i = 0; i < cubes.size(); i++) {
            facets.addAll(cubes.get(i).toCSG(generationContext).toFacets());
            progress = ((i) / (cubes.size() * 1.0)) * 100d;
            clrscr();
            System.out.println(("cubeFacet@" + NumberFormat.getPercentInstance().format(progress / 100)));
        }

        exportSTL(facets);
    }

    public static void clrscr() {
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                Runtime.getRuntime().exec("clear");
        } catch (IOException | InterruptedException ex) {
        }
    }

    public static void generateMaze(int[][] currentPairing, CubeCell[][][] cellMatrix, Set<CubeCell> visitedCells, int xSize, int ySize, int zSize) {
        if (visitedCells.size() == xSize * ySize * zSize) {
            return;
        }
        int[] currentPos = currentPairing[0];
        CubeCell currentCube = cellMatrix[currentPos[0]][currentPos[1]][currentPos[2]];
        int[] nextPos = currentPairing[2];
        CubeCell nextCube = cellMatrix[nextPos[0]][nextPos[1]][nextPos[2]];

        if (nextCube.visited() > 0)
            return;

        int[] direction = currentPairing[1];
        int[] directionFlipped = flippedDirection(direction);
        currentCube.open(direction);
        nextCube.open(directionFlipped);
        visitedCells.add(nextCube);

        int[][][] nextPairings = moveAsXYZPair(nextPos, xSize, ySize, zSize);
        for (int[][] nextPairing : nextPairings) {
            generateMaze(nextPairing, cellMatrix, visitedCells, xSize, ySize, zSize);
        }

    }

    public static int[][] obtainXYZPair(int xSize, int ySize, int zSize) {
        int[][] XYZpair = new int[3][2];
        boolean done = false;
        while (!done) {
            int[] randomXYZValues = randomXYZ(xSize, ySize, zSize);
            int direction[] = randomDirection();
            int[] otherXYZ = applyDirection(randomXYZValues.clone(), direction);
            boolean goodBounds = checkXYZBounds(otherXYZ, xSize, ySize, zSize);
            if (goodBounds) {
                XYZpair[0] = randomXYZValues;
                XYZpair[1] = direction;
                XYZpair[2] = otherXYZ;
                done = true;
            }
        }
        return XYZpair;
    }

    public static int[][][] moveAsXYZPair(int[] position, int xSize, int ySize, int zSize) {
        List<int[][]> goodList = new ArrayList<>();
        int[][] directions = randomDirections();
        for (int i = 0; i < directions.length; i++) {
            int[] direction = directions[i];
            int[] otherXYZ = applyDirection(position.clone(), direction);
            boolean goodBounds = checkXYZBounds(otherXYZ, xSize, ySize, zSize);
            if (goodBounds) {
                int[][] goodPair = new int[][]{position, direction, otherXYZ};
                goodList.add(goodPair);
            }
        }

        return goodList.toArray(new int[goodList.size()][][]);
    }

    public static int[] flippedDirection(int[] direction) {
        return new int[]{direction[0] * -1, direction[1] * -1, direction[2] * -1};
    }

    public static int[] randomXYZ(int xSize, int ySize, int zSize) {
        Random random = new Random();
        return new int[]{
            random.ints(0, xSize)
                .findFirst()
                .getAsInt(),
            random.ints(0, ySize)
                .findFirst()
                .getAsInt(),
            random.ints(0, zSize)
                .findFirst()
                .getAsInt()};
    }

    public static boolean checkXYZBounds(int[] position, int xSize, int ySize, int zSize) {
        return position[0] >= 0 && position[0] < xSize
            && position[1] >= 0 && position[1] < ySize
            && position[2] >= 0 && position[2] < zSize;
    }

    public static int[] randomDirection() {
        Random random = new Random();
        int value = random.ints(-1, 2)
            .filter(integer -> integer != 0)
            .findFirst()
            .getAsInt();

        int position = random.ints(0, 3)
            .findFirst()
            .getAsInt();
        int[] direction = new int[]{0, 0, 0};
        direction[position] = value;
        return direction;
    }

    public static int[][] randomDirections() {
        int[][] directions = new int[][]{
            new int[]{0, 0, 1},
            new int[]{0, 0, -1},
            new int[]{0, 1, 0},
            new int[]{0, -1, 0},
            new int[]{1, 0, 0},
            new int[]{-1, 0, 0},
        };
        shuffleArray(directions);
        return directions;
    }

    private static void shuffleArray(int[][] array) {
        int index;
        int[] temp;
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

    public static int[] applyDirection(int[] position, int[] direction) {
        position[0] += direction[0];
        position[1] += direction[1];
        position[2] += direction[2];
        return position;
    }

    public static void exportSCAD(List<IModel> models) throws IOException {
        (new SaveScadFiles(new File("C:/temp"))).addModels("test.scad", models).saveScadFiles();
    }

    public static void exportSTL(List<Facet> facets) throws IOException {
        FileExporterFactory.createExporter(new File("C:/temp/export-factory-test.stl")).writeToFile(facets);
    }

    public static void exportSTL(IModel model) throws IOException {
        ITagColors tagColors = (new TagColorsBuilder()).addTag(1, new Color(139, 90, 43)).addTag(2, Color.GRAY).buildTagColors();
        FileExporterFactory.createExporter(new File("C:/temp/export-factory-test.stl")).writeToFile(model.toCSG(new FacetGenerationContext(tagColors, null, 0)).toFacets());
    }
}
