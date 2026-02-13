package MeepMeepDrawing;

import com.acmerobotics.roadrunner.Action;
import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.TrajectoryActionBuilder;
import com.noahbres.meepmeep.roadrunner.entity.ActionEvent;
import com.noahbres.meepmeep.roadrunner.entity.ActionTimeline;
import com.noahbres.meepmeep.roadrunner.entity.RoadRunnerBotEntity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class RobotPath {
    public enum PathType {
        strafeToLinearHeading,
        strafeToConstantHeading,
        splineTo,
        splineToConstantHeading,
        splineToLinearHeading,


    }
    private static double LINE_DETAIL = 40;
    private final Node n1;
    private final Node n2;
    private final RoadRunnerBotEntity bot;
    private PathType type;
    private boolean highlight = false;
    private static final Color NODE_HIGHLIGHT_COLOR = new Color(32, 139, 205, 20);
    private static final Color NODE_NORMAL_COLOR = new Color(32, 139, 205, 10);
    private static final int OUTER_CIRCLE_PAINT_RADIUS = 10;
    private static final int INNER_CIRCLE_PAINT_RADIUS = 3;



    public RobotPath (Node n1, Node n2, PathType type, RoadRunnerBotEntity bot) {
        this.n1 = n1;
        this.n2 = n2;
        this.bot = bot;
        this.type = type;
    }

    public Node getN1() {
        return n1;
    }
    public Node getN2() {
        return n2;
    }
    public PathType getPathType() {
        return type;
    }
    public void setEndLocation(Point2D p) {
        this.n2.setLocation(p);
    }

     /**
     * Controls how detailed the line previewing the robots path is. Specifically, this controls how frequently a dot
     * is drawn at a point as the path is being interpolated
     * @param lineDetail the detail to set to
     */
    protected static void setLineDetail(int lineDetail) {
        LINE_DETAIL = lineDetail;
    }

    protected boolean isEmpty() {
        return n1.distance(n2)<1;
    }

    protected void paint(Graphics2D g2d) {
        if (n1==null || n2==null)
            return;
        n1.paint(g2d);
        n2.paint(g2d);
        previewAction(g2d, getAction(n1, n2));
        //bolds the path and draws a string in the center of the path with the type of the path
        if (highlight) {
            Point midPoint = new Point((int)((n1.x+n2.x)/2), (int)((n1.y+n2.y)/2));
            g2d.setColor(Color.WHITE);
            String name = type.name();
            int width = g2d.getFontMetrics().stringWidth(name);
            g2d.drawString(name, midPoint.x-width/2, midPoint.y);
        }

    }

    /**
     * Makes an Action object with the two passed in nodes that can be ran on a RoadRunnerBotEntity
     * @param n1 The starting node of the path
     * @param n2 The ending node of the path
     * @return An action to be ran on a RoadRunnerBotEntity
     */
    private Action getAction(Node n1, Node n2) {
        if (n1.distance(n2)<1) {//roadrunner throws an error if the start and end point are the same
            return null;
        }
        validateAngles();
        TrajectoryActionBuilder builder;
        //calls the corresponding robot action according to the path type of the RobotPath
        switch (type) {
            case strafeToLinearHeading:
                 builder = bot.getDrive().actionBuilder(n1.getHeadingPose());
                return builder.strafeToLinearHeading(n2.getVector(), n2.getHeading()).build();
            case strafeToConstantHeading:
                builder = bot.getDrive().actionBuilder(n1.getHeadingPose());
                return builder.strafeToConstantHeading(n2.getVector()).build();
            case splineTo:
                builder = bot.getDrive().actionBuilder(n1.getHeadingPose());
                return builder.splineTo(n2.getVector(), n2.getHeading()).build();
            case splineToConstantHeading:
                builder = bot.getDrive().actionBuilder(n1.getHeadingPose());
                return builder.splineToConstantHeading(n2.getVector(), n2.getTangent()).build();
            case splineToLinearHeading:
                builder = bot.getDrive().actionBuilder(n1.getHeadingPose());
                return builder.splineToLinearHeading(n2.getHeadingPose(), n2.getTangent()).build();
            default:
                return null;
        }
    }

    /**
     * Makes an Action object represented in inches with the two passed in nodes that can be ran on a RoadRunnerBotEntity
     * @return an action to be ran on a RoadRunnerBotEntity
     */
    protected Action getActionInInches() {
        return getAction(n1.scaleToInches(), n2.scaleToInches());
    }

    /**
     * Draws the path of an action onto a graphics object
     * @param g2d The graphics object to use to paint
     * @param action The action to paint
     */
    private void previewAction(Graphics2D g2d, Action action) {
        if (action==null) {
            return;
        }
        if (bot.getCurrentActionTimeline() != null)
            bot.getCurrentActionTimeline().getEvents().clear();

        bot.runAction(action);
        paintBotPath(g2d);
    }

    /**
     * Draws each point along the bots path as it runs whatever action it is currently doing
     * @param g2d The graphics object to use to paint
     */
    private void paintBotPath(Graphics2D g2d) {
        ArrayList<Point> points = getPointsAlongPath();
        if (points==null)
            return;
        for (Point p : points) {
            if (highlight)
                g2d.setColor(NODE_HIGHLIGHT_COLOR);
            else
                g2d.setColor(NODE_NORMAL_COLOR);
            //draws two circles, one bigger and more transparent one and one smaller and more opaque one
            g2d.fillOval(p.x - OUTER_CIRCLE_PAINT_RADIUS, p.y - OUTER_CIRCLE_PAINT_RADIUS, OUTER_CIRCLE_PAINT_RADIUS*2, OUTER_CIRCLE_PAINT_RADIUS*2);
            g2d.setColor(NODE_HIGHLIGHT_COLOR);
            g2d.fillOval(p.x - INNER_CIRCLE_PAINT_RADIUS, p.y - INNER_CIRCLE_PAINT_RADIUS, INNER_CIRCLE_PAINT_RADIUS*2, INNER_CIRCLE_PAINT_RADIUS*2);
        }
    }
    protected void setPathType(PathType type) {
        this.type = type;
    }

    /**
     * Generates a list of all the points along the path that the simulated robot is currently running
     * @return The list of every point along the robot's path
     */
    protected ArrayList<Point> getPointsAlongPath() {
        ActionTimeline timeline = bot.getCurrentActionTimeline();
        if (timeline == null)
            return null;
        ArrayList<Point> points = new ArrayList<>();
        //goes through every action in the bot's path
        for (ActionEvent e : timeline.getEvents()) {
            //goes through every point in the timeline and adds the point to the list
            for (double t = 0; t < timeline.getDuration(); t += 1.0/LINE_DETAIL) {
                Pose2d pose = e.getA().get(t);
                points.add(new Point((int) pose.position.x, (int) pose.position.y));
            }
        }
        return points;
    }

    /**
     * Runs this path on the simulated bot
     */
    protected void runPath() {
        if (bot.getCurrentActionTimeline() != null)
            bot.getCurrentActionTimeline().getEvents().clear();
        Action a = getAction(n1,n2);
        if (a==null)
            return;
        bot.runAction(a);
    }
    protected void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }

    /**
     * Sets the path angle according to the current path type
     * @param angle The angle to set to in radians
     */
    public void setPathAngle(Node n, double angle) {
        //in some robotpaths, the path angle
        switch (type) {
            case strafeToLinearHeading:
                n.setHeading(angle);
                n.setTangent(angle);
                break;
            case strafeToConstantHeading:
                n.setHeading(angle);
                n.setTangent(angle);
                break;
            case splineTo:
                n.setTangent(angle);
                n.setHeading(angle);
                break;
            case splineToLinearHeading:
                n.setTangent(angle);
                break;
            case splineToConstantHeading:
                n.setTangent(angle);
                break;
        }
    }

    /**
     * Sets the heading angle according to the current path type
     * @param angle the angle to set the heading to in radians
     */
    public void setHeadingAngle(Node n, double angle) {
        switch (type) {
            case strafeToLinearHeading:
                n.setHeading(angle);
                n.setTangent(angle);
                break;
            case strafeToConstantHeading:
                n.setHeading(angle);
                n.setTangent(angle);
                break;
            case splineTo:
                n.setTangent(angle);
                n.setHeading(angle);
                break;
            case splineToLinearHeading:
                n.setHeading(angle);
                break;
            case splineToConstantHeading:
                n.setHeading(angle);
                break;
        }
    }

    /**
     * Fixes any discrepensies between path and heading angles that would cause the robot to instantly rotate to a different pose
     */
    protected void validateAngles() {
        if (n2.getHeading()!=n1.getHeading()) {
            if (type==PathType.splineToConstantHeading) {
                n2.setHeading(n1.getHeading());
            }
            if (type==PathType.strafeToConstantHeading) {
                n2.setHeading(n1.getHeading());
                n2.setTangent(n1.getTangent());
            }
        }
    }


}
