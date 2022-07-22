import eu.printingin3d.javascad.coords.Angles3d;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.coords.Dims3d;
import eu.printingin3d.javascad.models.Abstract3dModel;
import eu.printingin3d.javascad.models.Cube;
import eu.printingin3d.javascad.tranzitions.Union;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CubeCell {

    private Set<CubeSide> openCubeSides = new HashSet<>();

    private double size;
    private double thickness;
    private double gap;
    private double width;

    private boolean visited = false;
    private boolean start = false;

    public CubeCell(double size, double thickness, double gap, double width) {
        this.size = size;
        this.thickness = thickness;
        this.gap = gap;
        this.width = width;
    }

    public Abstract3dModel makeCubeModel() {
        return new Union(
            new Face(CubeSide.TOP).generateSide(),
            new Face(CubeSide.BOTTOM).generateSide(),
            new Face(CubeSide.LEFT).generateSide(),
            new Face(CubeSide.RIGHT).generateSide(),
            new Face(CubeSide.FRONT).generateSide(),
            new Face(CubeSide.BACK).generateSide()
        );
    }

    public void start() {
        visited = true;
        start = true;
    }

    public void open(CubeSide side) {
        openCubeSides.add(side);
        visited = true;
    }

    public boolean isVisited() {
        return visited;
    }

    public void open(int[] direction) {
        int axis = 0;
        while (direction[axis] == 0) {
            axis++;
        }

        switch (axis) {
            case 0 -> open(direction[axis] > 0 ? CubeSide.RIGHT : CubeSide.LEFT);
            case 1 -> open(direction[axis] > 0 ? CubeSide.BACK : CubeSide.FRONT);
            case 2 -> open(direction[axis] > 0 ? CubeSide.TOP : CubeSide.BOTTOM);
        }
    }

    public enum CubeSide {
        TOP, BOTTOM, LEFT, RIGHT, FRONT, BACK;
    }

    public class Face {

        private boolean selfOpen = false;
        private CubeSide cubeSide;
        private boolean topOpen = false;
        private boolean bottomOpen = false;
        private boolean leftOpen = false;
        private boolean rightOpen = false;


        public Face(CubeSide cubeSide) {
            selfOpen = openCubeSides.contains(cubeSide);
            this.cubeSide = cubeSide;
            if (selfOpen || (start && cubeSide == CubeSide.BOTTOM))
                return;

            switch (cubeSide) {
                case BOTTOM -> {
                    topOpen = openCubeSides.contains(CubeSide.BACK);
                    bottomOpen = openCubeSides.contains(CubeSide.FRONT);
                    rightOpen = openCubeSides.contains(CubeSide.RIGHT);
                    leftOpen = openCubeSides.contains(CubeSide.LEFT);
                }
                case TOP -> {
                    topOpen = openCubeSides.contains(CubeSide.BACK);
                    bottomOpen = openCubeSides.contains(CubeSide.FRONT);
                    rightOpen = openCubeSides.contains(CubeSide.LEFT);
                    leftOpen = openCubeSides.contains(CubeSide.RIGHT);
                }
                case LEFT -> {
                    topOpen = openCubeSides.contains(CubeSide.TOP);
                    bottomOpen = openCubeSides.contains(CubeSide.BOTTOM);
                    leftOpen = openCubeSides.contains(CubeSide.FRONT);
                    rightOpen = openCubeSides.contains(CubeSide.BACK);
                }
                case RIGHT -> {
                    topOpen = openCubeSides.contains(CubeSide.TOP);
                    bottomOpen = openCubeSides.contains(CubeSide.BOTTOM);
                    leftOpen = openCubeSides.contains(CubeSide.BACK);
                    rightOpen = openCubeSides.contains(CubeSide.FRONT);
                }
                case FRONT -> {
                    topOpen = openCubeSides.contains(CubeSide.TOP);
                    bottomOpen = openCubeSides.contains(CubeSide.BOTTOM);
                    leftOpen = openCubeSides.contains(CubeSide.LEFT);
                    rightOpen = openCubeSides.contains(CubeSide.RIGHT);
                }
                case BACK -> {
                    topOpen = openCubeSides.contains(CubeSide.TOP);
                    bottomOpen = openCubeSides.contains(CubeSide.BOTTOM);
                    leftOpen = openCubeSides.contains(CubeSide.RIGHT);
                    rightOpen = openCubeSides.contains(CubeSide.LEFT);
                }
            }
        }

        private Abstract3dModel generateSide() {

            if (selfOpen || (start && cubeSide == CubeSide.BOTTOM)) {
                double hide = (size / 2.0) - (thickness / 2.0);
                return new Cube(thickness).move(new Coords3d(hide, hide, hide));
            }

            double movement = (size / 2.0) - (thickness / 2.0);
            double xRotation = 0.0;
            double yRotation = 0.0;
            double zRotation = 0.0;
            double x = 0.0;
            double y = 0.0;
            double z = 0.0;

            switch (cubeSide) {
                case TOP -> {
                    z += movement;
                    xRotation = 180;
                    yRotation = 0;
                    zRotation = 180;

                    /*
                    left, front,  correct
                    xRotation = 180;
                    yRotation = 0;
                    zRotation = 180;
                     */
                }
                case BOTTOM -> {
                    z -= movement;
                }
                case LEFT -> {
                    xRotation = 90;
                    yRotation = 0;
                    zRotation = 90;
                    x -= movement;
                }
                case RIGHT -> {
                    xRotation = 90;
                    yRotation = 0;
                    zRotation = -90;
                    x += movement;
                }
                case FRONT -> {
                    y -= movement;
                    xRotation = -90;
                    yRotation = 180;
                    zRotation = 0;
                }
                case BACK -> {
                    xRotation = 90;
                    yRotation = 0;
                    zRotation = 180;
                    y += movement;
                }
            }

            return generateFace()
                .rotate(new Angles3d(xRotation, yRotation, zRotation))
                .move(new Coords3d(x, y, z));
        }

        private Abstract3dModel generateFace() {
            List<Abstract3dModel> faceParts = new ArrayList<>();
            double movement = (size / 2.0) - (width / 2.0);

            //bottom
            Abstract3dModel bottom = createEdge(bottomOpen).move(Coords3d.yOnly(-movement));
            faceParts.add(bottom);

            //top
            Abstract3dModel top = createEdge(topOpen).move(Coords3d.yOnly(movement));
            faceParts.add(top);

            //left
            Abstract3dModel left = createEdge(leftOpen)
                .rotate(Angles3d.zOnly(90.0))
                .move(Coords3d.xOnly(-movement));
            faceParts.add(left);

            //right
            Abstract3dModel right = createEdge(rightOpen)
                .rotate(Angles3d.zOnly(90.0))
                .move(Coords3d.xOnly(movement));
            faceParts.add(right);

            return new Union(faceParts);
        }

        private Abstract3dModel createEdge(boolean open) {
            Abstract3dModel side;
            Abstract3dModel sideGap;
            side = new Cube(new Dims3d(size, width, thickness));

            if (open) {
//                sideGap = new Cube(new Dims3d(gap, width/2.0, thickness));
//                side = side.subtractModel(sideGap);
            }
            return side;
        }
    }
}
