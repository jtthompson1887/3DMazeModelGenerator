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
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public Main() {
    }

    public static void main(String[] args) throws IOException {

//        List<Abstract3dModel> parts = new ArrayList<>();
//        Cube cube = new Cube(new Dims3d(25.0, 10.0, 2.0));
//        parts.add(cube);
        int cellAmount = 50;

        int xSize = cellAmount;
        int ySize = cellAmount;
        int zSize = cellAmount;

        double scale = 1.66;
        double size = 10 * scale;
        double thickness = 1 * scale;
        double gap = 5 * scale;
        double width = 1.75 * scale;
        int startX = cellAmount / 2;
        int startY = cellAmount / 2;

        CubeCell[][][] cellMatrix = new CubeCell[xSize][ySize][zSize];

        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < xSize; z++) {
                    CubeCell cubeCell = new CubeCell(size, thickness, gap, width);
                    cellMatrix[x][y][z] = cubeCell;
                }
            }
        }

//        int[][] xyzPair = obtainXYZPair(xSize, ySize, zSize);
//        CubeCell firstCell = cellMatrix[xyzPair[0][0]][xyzPair[0][1]][xyzPair[0][2]];
//        int[] direction = xyzPair[1];
//        CubeCell secondCell = cellMatrix[xyzPair[2][0]][xyzPair[2][1]][xyzPair[2][2]];
//
//        firstCell.open(direction);
//        int[] directionFlipped = flippedDirection(direction);
//        secondCell.open(directionFlipped);
//
//        System.out.print(direction[0]+":"+direction[1]+":"+direction[2]);

        HashSet<CubeCell> visitedCells = new HashSet<>();
        int[] currentPos = new int[]{startX, startY, 0};
        CubeCell currentCube = cellMatrix[currentPos[0]][currentPos[1]][currentPos[2]];
        currentCube.start();
        visitedCells.add(currentCube);
        int[][][] nextPairings = moveAsXYZPair(currentPos, xSize, ySize, zSize);
        for (int[][] nextPairing : nextPairings) {
            generateMaze(nextPairing, cellMatrix, visitedCells, xSize, ySize, zSize);
        }


        List<Abstract3dModel> cubes = new ArrayList<>();
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < xSize; z++) {
                    CubeCell cubeCell = cellMatrix[x][y][z];
                    cubes.add(cubeCell.makeCubeModel()
                        .move(new Coords3d(x * size + x * 0.0001, y * size + y * 0.0001, z * size + z * 0.0001)));
                }
            }
        }


        double diameter = size - (thickness * 3);
        double dent = 0.15 * diameter;
        Abstract3dModel sphere = new Difference(new Sphere(Radius.fromDiameter(diameter)), new Cube(diameter).move(Coords3d.zOnly(-diameter + dent))).move(new Coords3d(size * startX, size * startY, -(dent) - thickness));
        Abstract3dModel sphereSupport = new Cube(diameter * 0.5).align(Side.BOTTOM_IN_CENTER, cubes.get(0).move(new Coords3d(size * startX, size * startY, 0d)));
        sphere = new Union(sphere, sphereSupport);
        cubes.add(sphere);

        ITagColors tagColors = (new TagColorsBuilder()).addTag(1, new Color(139, 90, 43)).addTag(2, Color.GRAY).buildTagColors();
        FacetGenerationContext generationContext = new FacetGenerationContext(tagColors, null, 0);
        List<Facet> facets = cubes.stream().flatMap(cube -> cube.toCSG(generationContext).toFacets().stream()).collect(Collectors.toList());
        //Abstract3dModel model = new Union(cubes);


        //exportSCAD(asList(model));
        exportSTL(facets);

//        ITagColors tagColors = (new TagColorsBuilder()).addTag(1, new Color(139, 90, 43)).addTag(2, Color.GRAY).buildTagColors();
//        Abstract3dModel cyl = (new Difference((new Cylinder(20.0, 5.0)).withTag(1), (new Cylinder(21.0, 2.0)).withTag(2))).moves(Arrays.asList(Coords3d.xOnly(-20.0), Coords3d.xOnly(20.0)));
//        Abstract3dModel base = (new Cube(new Dims3d(10.0, 10.0, 1.0))).addModel((new Cylinder(10.0, Radius.fromDiameter(1.0))).annotate("bit").moves(Coords3d.yOnly(4.0).createVariances()));
//        Abstract3dModel cubes = base.addModel((new Cube(new Dims3d(5.0, 5.0, 1.0))).subtractModel((new Cylinder(2.0, Radius.fromDiameter(1.5))).move(Coords3d.xOnly(2.0)).annotate("hole")).align("hole", Side.CENTER, base, "bit").move(Coords3d.zOnly(1.0)));
//        (new SaveScadFiles(new File("C:/temp"))).addModel("test.scad", cubes).saveScadFiles();
//        FileExporterFactory.createExporter(new File("C:/temp/export-factory-test.stl")).writeToFile(cyl.toCSG(new FacetGenerationContext(tagColors, (FacetGenerationContext)null, 0)).toFacets());
    }


    public static void generateMaze(int[][] currentPairing, CubeCell[][][] cellMatrix, Set<CubeCell> visitedCells, int xSize, int ySize, int zSize) {
        if (visitedCells.size() == xSize * ySize * zSize) {
            return;
        }
        int[] currentPos = currentPairing[0];
        CubeCell currentCube = cellMatrix[currentPos[0]][currentPos[1]][currentPos[2]];
        int[] nextPos = currentPairing[2];
        CubeCell nextCube = cellMatrix[nextPos[0]][nextPos[1]][nextPos[2]];

        if (nextCube.isVisited())
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
        ITagColors tagColors = (new TagColorsBuilder()).addTag(1, new Color(139, 90, 43)).addTag(2, Color.GRAY).buildTagColors();
        FileExporterFactory.createExporter(new File("C:/temp/export-factory-test.stl")).writeToFile(facets);
    }

    public static void exportSTL(IModel model) throws IOException {
        ITagColors tagColors = (new TagColorsBuilder()).addTag(1, new Color(139, 90, 43)).addTag(2, Color.GRAY).buildTagColors();
        FileExporterFactory.createExporter(new File("C:/temp/export-factory-test.stl")).writeToFile(model.toCSG(new FacetGenerationContext(tagColors, null, 0)).toFacets());
    }
}
