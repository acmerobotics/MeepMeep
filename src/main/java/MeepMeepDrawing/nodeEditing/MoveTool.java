package MeepMeepDrawing.nodeEditing;

import com.noahbres.meepmeep.roadrunner.entity.RoadRunnerBotEntity;

import java.awt.Point;
import java.awt.event.MouseEvent;

import MeepMeepDrawing.RobotPath;

public class MoveTool extends PathEditingTool {

    public MoveTool(RobotPath.PathType pathType, RoadRunnerBotEntity bot) {
        super(pathType, bot);
    }


    @Override
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
        setNodeLocation(e.getPoint());
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        setNodeLocation(e.getPoint());
    }

    /**
     * Sets the selected path's ending node to the mouselocation to move it
     * @param p The point to move the node to
     */
    private void setNodeLocation(Point p) {
        RobotPath path = getSelectedPath();
        if (path==null)
            return;
        path.setEndLocation(p);
    }

}
