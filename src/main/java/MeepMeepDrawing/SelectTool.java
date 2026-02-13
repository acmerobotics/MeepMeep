package MeepMeepDrawing;

import com.noahbres.meepmeep.roadrunner.entity.RoadRunnerBotEntity;

import java.awt.Point;
import java.awt.event.MouseEvent;

public class SelectTool extends Tool{

    private RobotPath selectedPath = null;
    public SelectTool(RobotPath.PathType pathType, RoadRunnerBotEntity bot) {
        super(pathType, bot);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        //resets the previous path's highlight
        if (selectedPath!=null)
            selectedPath.setHighlight(false);

        //highlights the clicked path
        selectedPath = getPathAt(e.getPoint());
        if (selectedPath!=null)
            selectedPath.setHighlight(true);

    }

    /**
     * Allows the user to change the path type of the selected path
     * @param type the path type to change to
     */
    @Override
    public void setCurrentPathType(RobotPath.PathType type) {
        super.setCurrentPathType(type);
        if (selectedPath==null)
            return;
        selectedPath.setPathType(type);
        selectedPath.validateAngles();
    }


    /**
     * Returns the path overlapping with a point
     * @param p The point to check for a path at
     * @return A path overlapping the point, or null if there is no path
     */
    public RobotPath getPathAt(Point p) {
        for (RobotPath path : getPath()) {
            path.runPath();
            for (Point pathPoint : path.getPointsAlongPath()) {
                if (pathPoint.distance(p) < getMouseToNodeSensitivity())
                    return path;
            }
        }
        return null;
    }



}
