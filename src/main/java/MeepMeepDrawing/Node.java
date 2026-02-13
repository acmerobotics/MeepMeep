package MeepMeepDrawing;
import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.Vector2d;
import com.noahbres.meepmeep.core.util.FieldUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

public class Node extends Point2D.Double {
    private double heading = 0;
    private double tangent = 0;
    private static final int NODE_RADIUS = 10;
    private static final Color NODE_COLOR = new Color(36, 86, 224);
    private static final Color HEADING_ARROW_COLOR = new Color(55, 255, 0, 150);
    private static final Color TANGENT_ARROW_COLOR = new Color(255, 0, 0, 150);

    /**
     * Creates a node according to the raw x and y values passed in
     */
    public Node(double x, double y, double heading, double tangent) {
        super(x,y);
        this.heading = heading;
        this.tangent = tangent;
    }

    /**
     * Creates a node according to the raw values of the passed in point
     */
    public Node(Point p, double heading, double tangent) {
        super(p.x, p.y);
        this.heading = heading;
        this.tangent = tangent;
    }
    /**
     * Creates a Node according to a vector object scaled from inches to pixels
     */
    public Node(Vector2d v, double heading) {
        super(v.x,v.y);
        Node n = scaleToPixels();
        this.x = n.x;
        this.y = n.y;
        this.heading = heading;
        this.tangent = heading;
    }
    /**
     * Creates a Node according to a pose object scaled from inches to pixels
     */
    public Node(Pose2d p, double tangent) {
        super(p.position.x,p.position.y);
        Node n = scaleToPixels();
        this.x = n.x;
        this.y = n.y;
        this.heading = p.heading.toDouble();
        this.tangent = tangent;
    }

    /**
     * Draws the node and the arrow(s) representing the heading and/or tangent
     */
    public void paint(Graphics2D g2d) {
        g2d.setColor(NODE_COLOR);
        int radius = NODE_RADIUS;
        g2d.fillOval((int)x-radius/2, (int)y-radius/2, radius, radius);
        drawArrow(g2d, heading, HEADING_ARROW_COLOR);
        drawArrow(g2d, tangent, TANGENT_ARROW_COLOR);
    }

    /**
     * Draws an arrow in a certain direction with a certainn color with a certain graphics object
     * @param g2d The graphics object to paint with
     * @param direction The direction of the angle in radians
     * @param color The color of the arrow
     */
    private void drawArrow(Graphics2D g2d, double direction, Color color) {
        g2d.setStroke(new BasicStroke(5));
        g2d.setColor(color);
        Line2D arrow = new Line2D.Double(this, new Double(x+20, y));
        int x = (int)this.x;
        int y = (int)this.y;
        Polygon triangle = new Polygon(new int[]{x+20,x+25,x+20}, new int[]{y-6, y, y+6}, 3);
        AffineTransform graphicsTransform = g2d.getTransform();
        AffineTransform rotateInstance = new AffineTransform();
        rotateInstance.translate(x, y);
        rotateInstance.rotate(direction);
        rotateInstance.translate(-x, -y);
        g2d.transform(rotateInstance);
        g2d.draw(arrow);
        g2d.fill(triangle);
        g2d.setTransform(graphicsTransform);
    }

    protected Pose2d getHeadingPose() {
        return new Pose2d(x,y,heading);
    }
    protected Vector2d getVector() {
        return new Vector2d(x,y);
    }
    protected double getHeading() {
        return heading;
    }
    protected double getTangent() {
        return tangent;
    }
    /**
     * @return A node where all of its locational data has been scaled from pixels to inches
     */
    protected Node scaleToInches() {
        Vector2d inchPoint = FieldUtil.screenCoordsToFieldCoords(new Vector2d(x,y));
        return new Node(inchPoint.x, inchPoint.y, -heading, -tangent);
    }

    /**
     * @return A node where all of its locational data has been scaled from inches to pixels
     */
    protected Node scaleToPixels() {
        Vector2d pixelPoint = FieldUtil.fieldCoordsToScreenCoords(new Vector2d(x,y));
        return new Node(pixelPoint.x, pixelPoint.y, -heading, -tangent);
    }
    protected void setHeading(double heading) {
        this.heading = heading;
    }
    protected void setTangent(double tangent) {
        this.tangent = tangent;
    }

    /**
     * @return A pose of this position scaled to inches
     */
    protected Pose2d toPoseInInches() {
        return scaleToInches().getHeadingPose();
    }



}
