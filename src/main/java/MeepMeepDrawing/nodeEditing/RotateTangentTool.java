package MeepMeepDrawing.nodeEditing;

import com.noahbres.meepmeep.roadrunner.entity.RoadRunnerBotEntity;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import MeepMeepDrawing.Node;
import MeepMeepDrawing.RobotPath;

public class RotateTangentTool extends PathEditingTool {
    public RotateTangentTool(RobotPath.PathType type, RoadRunnerBotEntity bot) {
        super(type, bot);
    }


    @Override
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
        setNodeAngle(e);
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        setNodeAngle(e);
    }
    /**
     * Sets the angle of the selected path's endpoint based on the mouse's position
     * @param e The mouseEvent with the currentPosition and whether shift is held
     */
    private void setNodeAngle(MouseEvent e) {
        Point2D mousePoint = e.getPoint();
        RobotPath path = getSelectedPath();
        Node node = getSelectedNode();
        if (path==null)
            return;
        //snaps to 90 degrees if shift is held
        if (e.isShiftDown())
            mousePoint = roundTo90(node, mousePoint);
        //sets the heading angle based on the angle calculated with the arctan of y/x
        path.setPathAngle(node, Math.atan2(mousePoint.getY()-node.y, mousePoint.getX()-node.x));
    }

}
