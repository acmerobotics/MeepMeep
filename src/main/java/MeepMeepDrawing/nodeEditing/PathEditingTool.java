package MeepMeepDrawing.nodeEditing;

import com.noahbres.meepmeep.roadrunner.entity.RoadRunnerBotEntity;

import java.awt.Point;
import java.awt.event.MouseEvent;

import MeepMeepDrawing.Node;
import MeepMeepDrawing.RobotPath;
import MeepMeepDrawing.Tool;

public abstract class PathEditingTool extends Tool {
    private RobotPath selectedPath;
    private Node selectedNode;
    public PathEditingTool(RobotPath.PathType type, RoadRunnerBotEntity bot) {
        super(type, bot);
    }
    @Override
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        selectedPath = getPathEndingAt(e.getPoint());
        if (selectedPath==null)
            return;
        //if n1 is closer to the mouse than n2
        if (selectedPath.getN1().distance(e.getPoint())<selectedPath.getN2().distance(e.getPoint()))
            selectedNode = selectedPath.getN1();
        else
            selectedNode = selectedPath.getN2();

    }

    /**
     * Aquires the path that ends at a certain point, or the first path in the list if the very first node is selected
     * @param p The point to look for a path at
     * @return The path at the point, or null if there is no path present
     */
    protected RobotPath getPathEndingAt(Point p) {
        for (int i = 0; i < getPath().size(); i++) {
            RobotPath path = getPath().get(i);
            //checks if the endpoint of the path is overlaps the point to look for a path
            if (path.getN2().distance(p)<getMouseToNodeSensitivity())
                return path;
            //accounts for the very first node in the list because it will never be the second node in a path
            if (i==0 && path.getN1().distance(p)<getMouseToNodeSensitivity())
                return path;
        }
        return null;
    }
    protected RobotPath getSelectedPath() {
        return selectedPath;
    }
    protected Node getSelectedNode() {
        return selectedNode;
    }
}
