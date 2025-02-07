package MeepMeepDrawing;

import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.Vector2d;
import com.noahbres.meepmeep.roadrunner.entity.RoadRunnerBotEntity;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public abstract class Tool implements MouseListener, MouseMotionListener {

    public enum EditorMode{
        Draw,
        Move,
        Rotate_Heading,
        Rotate_Path,
        Select
    }
    private static final int MOUSE_TO_NODE_SENSITIVITY = 20;
    private static final ArrayList<RobotPath> path = new ArrayList<>();
    private RobotPath.PathType currentPathType;
    private final RoadRunnerBotEntity bot;

    public Tool(RobotPath.PathType pathType, RoadRunnerBotEntity bot) {
        this.currentPathType = pathType;
        this.bot = bot;
        path.forEach(robotPath-> {robotPath.setHighlight(false);});
    }

    @Override
    public void mouseMoved(MouseEvent e) {}
    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseDragged(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseClicked(MouseEvent e) {
        //right click removes the very last node in the path
        if (e.getButton()==MouseEvent.BUTTON3) {
            if (path.get(path.size()-1).getN2().distance(e.getPoint())<MOUSE_TO_NODE_SENSITIVITY) {
                path.remove(path.size()-1);
            }
        }
    }
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}

    protected void setCurrentPathType(RobotPath.PathType type) {
        this.currentPathType = type;
    }
    protected ArrayList<RobotPath> getPath() {
        return path;
    }
    protected void addPath(RobotPath robotPath) {
        path.add(robotPath);
    }
    protected RobotPath.PathType getPathType() {
        return currentPathType;
    }
    protected RoadRunnerBotEntity getBot() {
        return bot;
    }

    /**
     * Starts the path at a certain point when no other paths have been drawn
     * @param p1 The very first point in a path
     */
    protected void initPath(Point p1) {
        path.add(new RobotPath(new Node(p1, 0, 0), new Node(p1,0,0), currentPathType, bot));
    }
    protected static void paintPath(Graphics2D g2d) {
        path.forEach(path -> path.paint(g2d));
    }
    protected int getMouseToNodeSensitivity() {
        return MOUSE_TO_NODE_SENSITIVITY;
    }

    /**
     * Makes a point that has been rounded from the point to round to the nearest 90 degree angle around the startpoint
     * @param startPoint The point to round according to
     * @param pointToRound The point to round to the nearest 90 degrees
     * @return The rounded point
     */
    protected Point2D roundTo90(Point2D startPoint, Point2D pointToRound) {
        double xDiff = Math.abs(pointToRound.getX()-startPoint.getX());
        double yDiff = Math.abs(pointToRound.getY()-startPoint.getY());
        if (xDiff>yDiff) {
            return new Point2D.Double(pointToRound.getX(), startPoint.getY());
        }
        else {
            return new Point2D.Double(startPoint.getX(), pointToRound.getY());
        }
    }

    /**
     * Generates a string that contains the code for the current path on the screen
     * @return The string generated that contains the code for the user to copy or edit
     */
    protected String getActionString() {
        if (path.isEmpty())
            return "";
        //initiates the string at the starting pose of the first path in the list
        StringBuilder actionString = new StringBuilder("bot.getDrive().actionBuilder(" + poseToString(path.get(0).getN1().toPoseInInches()) + ")");
        //adds all actions to the string
        for (int i = 0; i<path.size();i++) {
            RobotPath p = path.get(i);
            actionString.append(pathToString(p));
        }
        //closes and returns the string
        actionString.append(")");
        return actionString.toString();
    }

    /**
     * Converts a pose2d object to a string representation of the code needed to create one
     * @param pose The pose to turn into a string
     * @return The string code representation of the pose
     */
    private String poseToString(Pose2d pose) {
        double heading = pose.heading.toDouble();
        return ("new Pose2d(" + pose.position.x + "," + pose.position.y + "," + heading + ")");
    }

    /**
     * Convers a vector2d object to a string representation of the code needed to create one
     * @param vector The vector to turn into a string
     * @return The string code representation of the vecotr
     */
    private String vectorToString(Vector2d vector) {
        return ("new Vector2d(" + vector.x + "," + vector.y + ")");
    }

    /**
     * Converts a RobotPath to a string represntation of the code needed to create one.
     * This string will be one line that contains all the information needed to tell a robot to follow the desired path
     * @param path The path to convert to a string
     * @return A string representation of a line of code according to the syntax of the current pathType
     */
    private String pathToString(RobotPath path) {
        String actionString = "\n\t." + path.getPathType() + "(";
        //adds the correct syntax for each different type of path
        switch (path.getPathType()) {
            case strafeToLinearHeading:
                actionString+=vectorToString(path.getN2().scaleToInches().getVector()) + "," + path.getN2().getHeading() + ")";
                break;
            case strafeToConstantHeading:
                actionString+=vectorToString(path.getN2().scaleToInches().getVector()) + ")";
                break;
            case splineTo:
                actionString+=vectorToString(path.getN2().scaleToInches().getVector()) + "," + path.getN2().getHeading() + ")";
                break;
            case splineToConstantHeading:
                actionString+=vectorToString(path.getN2().scaleToInches().getVector()) + "," + path.getN2().getTangent() + ")";
                break;
            case splineToLinearHeading:
                actionString+=poseToString(path.getN1().toPoseInInches()) + "," + path.getN2().getHeading() + ")";
        }
        return actionString;
    }

    /**
     * Attempts to parse code that the user has written to turn into actions that the robot can follow
     *
     * @param path The code that the user has written
     */
    protected void tryParseCodePath(String path) {
        if (path.isEmpty()) {
            Tool.path.clear();
            return;
        }
        //removes all spaces to ensure compatibility with different spacing styles
        path = path.replaceAll(" ", "");
        //splits the string into multiple according to a line break
        String[] lines = path.split("\\R");
        ArrayList<RobotPath> paths;
        //trys to interpret the lines of code the user has written. if the code is incomprehensible to the program, nothing in the path is updated
        try {
            paths = interpretPaths(lines);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            return;
        }
        Tool.path.clear();
        Tool.path.addAll(paths);
    }

    /**
     * Attempts to interpret the lines of code the user has written and turn it into a list of robotpaths.
     * If a piece of code is encountered that does not match known syntax, an error is thrown
     * @param lines The lines of code to interpret
     * @return A list of the paths according to the code
     */
    private ArrayList<RobotPath> interpretPaths(String[] lines) throws IndexOutOfBoundsException, NumberFormatException {
        Pose2d startingPose = interpretPose2d(lines[0]);
        ArrayList<RobotPath> paths = new ArrayList<>();
        //starts i at 1 because the first line of code merely sets the starting pose, it doesn't actually define a path
        for (int i = 1; i < lines.length; i++) {
            Node previousNode;
            //sets the previous node to the previous path's ending node, or the starting node if there is no previous node
            if (paths.isEmpty())
                previousNode = new Node(startingPose, startingPose.heading.toDouble());
            else
                previousNode = paths.get(paths.size()-1).getN2();

            //interprets the correct path type depending on the syntax
            if (lines[i].contains(RobotPath.PathType.strafeToLinearHeading.name()))
                paths.add(interpretStrafeLinearHeading(lines[i], previousNode));
            else if (lines[i].contains(RobotPath.PathType.strafeToConstantHeading.name()))
                paths.add(interpretStrafeConstantHeading(lines[i], previousNode));
            else if (lines[i].contains(RobotPath.PathType.splineToConstantHeading.name()))
                paths.add(interpretSplineConstantHeading(lines[i], previousNode));
            else if (lines[i].contains(RobotPath.PathType.splineToLinearHeading.name()))
                paths.add(interpretSplineLinearHeading(lines[i], previousNode));
            else if (lines[i].contains(RobotPath.PathType.splineTo.name()))
                paths.add(interpretSpline(lines[i], previousNode));
        }
        return paths;
    }


    //the following methods interpret each different type of path that is used in the program and returns a corresponding RobotPath of the same type
    private RobotPath interpretStrafeLinearHeading(String line, Node previousNode) {
        return new RobotPath(previousNode, new Node(interpretVector2d(line), interpretTangent(line)), RobotPath.PathType.strafeToLinearHeading, bot);
    }
    private RobotPath interpretStrafeConstantHeading(String line, Node previousNode) {
        return new RobotPath(previousNode, new Node(interpretVector2d(line), previousNode.getHeading()), RobotPath.PathType.strafeToConstantHeading, bot);
    }
    private RobotPath interpretSpline(String line, Node previousNode) {
        return new RobotPath(previousNode, new Node(interpretVector2d(line), interpretTangent(line)), RobotPath.PathType.splineTo, bot);
    }
    private RobotPath interpretSplineConstantHeading(String line, Node previousNode) {

        return new RobotPath(previousNode, new Node(interpretVector2d(line), interpretTangent(line)), RobotPath.PathType.splineToConstantHeading, bot);
    }
    private RobotPath interpretSplineLinearHeading(String line, Node previousNode) {
        return new RobotPath(previousNode, new Node(interpretPose2d(line), interpretTangent(line)), RobotPath.PathType.splineToLinearHeading, bot);
    }

    /**
     * Turns a string containing a Vector2d definition into a Vector2d
     * @param line The line of code to look for a vector definition
     * @return A vector object interpreted from the line of code
     */
    private Vector2d interpretVector2d(String line) {
        String vector = "newVector2d(";
        int index = line.indexOf(vector)+vector.length();
        StringBuilder x = new StringBuilder();
        //adds each digit within the string until it reaches a ',' signifying the end of the number
        while (line.charAt(index)!=',') {
            x.append(line.charAt(index));
            index++;
        }
        StringBuilder y = new StringBuilder();
        //index will be at the ',' character, so 1 is added to it to go to the next number
        index++;
        //adds each digit within the string until it reaches a ')' signifying the end of the vector
        while (line.charAt(index)!=')') {
            y.append(line.charAt(index));
            index++;
        }
        return new Vector2d(Double.parseDouble(x.toString()), Double.parseDouble(y.toString()));
    }

    /**
     * Turns a string containing a Pose2d definition into a Pose2d
     * @param line The line of code to look for a pose definition
     * @return A pose object interpreted from the line of code
     */
    private Pose2d interpretPose2d(String line) {
        String pose = "newPose2d(";
        int index = line.indexOf(pose)+pose.length();
        StringBuilder x = new StringBuilder();
        //adds each digit of the x pos until a ',' is reached
        while (line.charAt(index)!=',') {
            x.append(line.charAt(index));
            index++;
        }
        //1 is added to get put the index on a number instead of a ','
        index++;
        StringBuilder y = new StringBuilder();
        //adds each digit of y
        while (line.charAt(index)!=',') {
            y.append(line.charAt(index));
            index++;
        }
        StringBuilder heading = new StringBuilder();
        index++;
        //adds each digit of the heading angle
        while (line.charAt(index)!=')') {
            heading.append(line.charAt(index));
            index++;
        }
        //heading is multiplied by negative 1 because the conversion between screen coords to inches flips the angle, so adding a negative sign flips it back
        return new Pose2d(Double.parseDouble(x.toString()), Double.parseDouble(y.toString()), -Double.parseDouble(heading.toString()));
    }


    /**
     * Interprets the tangent angle from a line of code that performs a robot action represented as a string.
     * @param line The line of code to look for a tangent in.
     * @return The angle in radians of the tangent of the line
     */
    private double interpretTangent(String line) {
        //In all actions, the first ')' marks
        //the end of a position definition and the start of an angle definition. For example, in
        //strafeToLinearHeading(new Vector2d(39.65625,26.261250000000004),0.0), the first instance of a ')' character marks the start of an angle.
        //this is true for the rest of the path types as well
        int index = line.indexOf(')') +2;//add two to the index, one for the ending ')', and the other for the ending ',' both after vectors or poses
        StringBuilder tangent = new StringBuilder();
        //adds all the digits of the tangent angle
        while (line.charAt(index)!=')') {
            tangent.append(line.charAt(index));
            index++;
        }
        return Double.parseDouble(tangent.toString());
    }



}
