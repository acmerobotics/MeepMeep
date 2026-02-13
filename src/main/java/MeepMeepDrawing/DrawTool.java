package MeepMeepDrawing;

import com.noahbres.meepmeep.roadrunner.entity.RoadRunnerBotEntity;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

public class DrawTool extends Tool {


    public DrawTool(RobotPath.PathType pathType, RoadRunnerBotEntity bot) {
        super(pathType, bot);
    }




    @Override
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        //ensures that only left click applies the action
        if (e.getButton()!=MouseEvent.BUTTON1)
            return;
        //initializes the starting path if there is no other path drawn yet
        if (getPath().isEmpty()) {
            initPath(e.getPoint());
            return;
        }

        //makes a new path with the starting node being the ending node of the previous path
        Node previousNode = getPath().get(getPath().size()-1).getN2();
        RobotPath newPath = new RobotPath(previousNode, new Node(e.getPoint(), 0,0), super.getPathType(), getBot());
        super.addPath(newPath);

    }
    @Override
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
        //ensures that only left click applies the action
        if (e.getButton()!=MouseEvent.BUTTON1)
            return;
        Point2D mousePoint = e.getPoint();
        //moves the path's endpoint while the mouse is being dragged. Also rounds the point if shift is held
        if (e.isShiftDown())
            mousePoint = roundTo90(getPath().get(getPath().size()-1).getN1(), mousePoint);
        super.getPath().get(getPath().size()-1).setEndLocation(mousePoint);
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        //ensures that only left click applies the action
        if (e.getButton()!=MouseEvent.BUTTON1)
            return;
        Point2D mousePoint = e.getPoint();
        if (e.isShiftDown())
            mousePoint = roundTo90(getPath().get(getPath().size()-1).getN1(), mousePoint);
        //finalizes the path's location and ensures that the path is not empty
        super.getPath().get(getPath().size()-1).setEndLocation(mousePoint);
        if (super.getPath().get(getPath().size()-1).isEmpty()) {
            super.getPath().remove(getPath().size()-1);
        }
    }
}
