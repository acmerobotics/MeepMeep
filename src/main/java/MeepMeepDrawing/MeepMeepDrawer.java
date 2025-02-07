package MeepMeepDrawing;

import com.acmerobotics.roadrunner.Action;
import com.acmerobotics.roadrunner.SequentialAction;
import com.noahbres.meepmeep.MeepMeep;
import com.noahbres.meepmeep.roadrunner.Constraints;
import com.noahbres.meepmeep.roadrunner.DefaultBotBuilder;
import com.noahbres.meepmeep.roadrunner.entity.RoadRunnerBotEntity;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Stack;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import MeepMeepDrawing.nodeEditing.MoveTool;
import MeepMeepDrawing.nodeEditing.RotateHeadingTool;
import MeepMeepDrawing.nodeEditing.RotateTangentTool;


public class MeepMeepDrawer extends MeepMeep implements MouseListener, MouseMotionListener, KeyListener {
    private static final int timerHeight = 28; //height of the timer at the bottom of the screen
    private static final Color codeBackgroundColor = new Color(50, 50, 50);
    private static final Color codeTextColor = new Color(210, 210, 210);
    private final DrawingScreen screen;
    private final RoadRunnerBotEntity bot;
    private final Container meepMeepContentPane;
    private final JTextArea codeTextArea = new JTextArea();
    private int fps = 60;
    private int windowSize;
    private Tool currentTool; //Represents the current tool (e.g., Draw, Move, Rotate) the user is using to interact with the path.
    private RobotPath.PathType currentPathType;
    private boolean drawingView = true;
    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();



    public MeepMeepDrawer(int windowSize, double botWidth, double botHeight, Constraints constraints) {
        super(windowSize);
        this.windowSize = windowSize;
        //the bot to drive across the screen when previewing the path
        bot = createBot(botWidth, botHeight, constraints);
        super.addEntity(bot);

        setupListeners();
        screen = new DrawingScreen(windowSize);
        setupScreenPanels();

        meepMeepContentPane = super.getWindowFrame().getContentPane();
        switchToDrawingView();
        setLineDetail((int)((constraints.getMaxVel())/3));

        setupRepaintLoop();
        super.start();
    }
    public MeepMeepDrawer(int windowSize, double botWidth, double botHeight, Constraints constraints, int fps) {
        super(windowSize, fps);
        this.fps = fps;
        bot = createBot(botWidth, botHeight, constraints);
        super.addEntity(bot);

        setupListeners();

        screen = new DrawingScreen(windowSize);
        setupScreenPanels();

        meepMeepContentPane = super.getWindowFrame().getContentPane();
        switchToDrawingView();
        setLineDetail((int)(constraints.getMaxVel()/3));

        setupRepaintLoop();
        super.start();
    }

    /**
     * Creates the simulated bot that drives along the path in the Meep Meep view
     */
    private RoadRunnerBotEntity createBot(double botWidth, double botHeight, Constraints constraints) {
        return new DefaultBotBuilder(this)
                .setDimensions(botWidth, botHeight)
                .setConstraints(constraints)
                .build();
    }

    /**
     * Adds these key, mouse, and mouseMotion listeners to the frame
     */
    private void setupListeners() {
        super.getWindowFrame().addMouseListener(this);
        super.getWindowFrame().addMouseMotionListener(this);
        super.getWindowFrame().addKeyListener(this);
        super.getWindowFrame().setFocusable(true);
    }

    /**
     * Sets the background of both the meep meep view and the drawing view according to a {@link Background} object
     */
    public void setDrawingBackground(Background background) {
        screen.setBackground(background);
        super.setBackground(background);
    }

    /**
     * Controls how detailed the line previewing the robots path is. Specifically, this controls how frequently a dot
     * is drawn at a point as the path is being interpolated. High values may cause lag, and lower values will often reduce lag
     * @param lineDetail the detail to set to
     */
    public void setLineDetail(int lineDetail) {
        RobotPath.setLineDetail(lineDetail);
    }

    /**
     * Starts a Timer that will periodically repaint the drawing screen and update the code view,
     * ensuring that the user interface stays up-to-date while the simulation is running.
     */
    private void setupRepaintLoop() {
        new Timer(1000 / fps, actionEvent -> {
            if (!drawingView)
                return;
            syncCodeAndPath();
            screen.repaint();
        }).start();
    }

    /**
     * Updates the code text area or the drawing view depending on whether the user is editing
     * the code or drawing a path. If the user is not editing the code, it syncs the text area with
     * the current path. If the user is editing the code, it updates the path accordingly.
     */
    private void syncCodeAndPath() {
        String actionString = currentTool.getActionString();

        //checks if the user is drawing on a path and that the path has changed
        if (!codeTextArea.hasFocus() && !codeTextArea.getText().equals(actionString)) {
            addToHistory(codeTextArea.getText());
            codeTextArea.setText(actionString);
        }
        //otherwise checks if the user is editing the code
        else if (codeTextArea.hasFocus()) {
            currentTool.tryParseCodePath(codeTextArea.getText());
        }
    }

    /**
     * Adds the current action string to the undo stack, ensuring no duplicate actions are pushed.
     * It also clears the redo stack when a new action is added.
     * @param actionString the string representation of the robot's path to store
     */
    private void addToHistory(String actionString) {
        //if the undo stack is empty, there is no need to worry about duplicates
        if (undoStack.isEmpty()) {
            undoStack.push(actionString);
            redoStack.clear();
            return;
        }
        //otherwise the undo stack is not empty, and duplicates should not be pushed
        if (!undoStack.peek().equals(actionString)) {
            undoStack.push(actionString);
            redoStack.clear();
        }
    }




    /**
     * Sets up both the action panel and the code panel on the right and bottom of the screen respectively
     */
    private void setupScreenPanels() {
        JPanel actionPanel = setupActionPanel();

        //sets up button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(actionPanel.getBackground());
        setupAllRadioButtons(buttonPanel);
        setupCodePanel(buttonPanel);
        actionPanel.add(buttonPanel);

        setupInstructionPanel(actionPanel, buttonPanel);
    }

    /**
     * Adds text at the bottom of the action panel that tells the user how to operate the program
     */
    private void setupInstructionPanel(JPanel actionPanel, JPanel buttonPanel) {
        //setting up instruction pnale
        JPanel instructionPanel = new JPanel();
        instructionPanel.setBackground(actionPanel.getBackground());
        //setting up text area
        JTextPane instructions = setupTextPane(actionPanel);
        //formatting the text
        SimpleAttributeSet style = new SimpleAttributeSet();
        //sets the first line indent to -15 from the left indent of 20. This makes a hanging indent
        StyleConstants.setFirstLineIndent(style, -15);
        StyleConstants.setLeftIndent(style, 20);
        StyleConstants.setLineSpacing(style, 0.1f);
        //applys the formatting to the pane
        instructions.getStyledDocument().setParagraphAttributes(0,0,style,false);
        try {
            instructions.getStyledDocument().insertString(0,getInstructionString(), style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        JScrollPane scrollPane = setupInstructionScrollPane(buttonPanel, instructions);
        instructionPanel.add(scrollPane);
        actionPanel.add(instructionPanel);
    }

    private JScrollPane setupInstructionScrollPane(JPanel buttonPanel, JTextPane instructions) {
        JScrollPane scrollPane = new JScrollPane(instructions);
        scrollPane.setBackground(instructions.getBackground());
        scrollPane.setBorder(null);
        //wraps the instructions in a scroll pane so the text is readable at any window size
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        //sets the bounds to whatever space is left on the side panel after the button panel has been added
        scrollPane.setPreferredSize(new Dimension(screen.getActionPanelWidth(), screen.getPreferredSize().height- buttonPanel.getPreferredSize().height));
        return scrollPane;
    }

    /**
     * Sets up the pane used to hold instructions for the user to understand how to use the program
     * @param actionPanel the side panel on the right of the screen
     * @return the setup pane ready for text and formatting
     */
    private JTextPane setupTextPane(JPanel actionPanel) {
        JTextPane instructions = new JTextPane();
        instructions.setBackground(actionPanel.getBackground());
        //setting default paramaters
        instructions.setForeground(Color.BLACK);
        instructions.setEditable(false);
        instructions.setFont(new Font(Font.SERIF, Font.PLAIN, 15));
        instructions.setFocusable(false);
        return instructions;
    }

    /**
     * Generates & returns the string that describes how to use the program
     * @return The string the user can read to understand how to use the program
     */
    private String getInstructionString() {
        return ("Draw: Click and drag to draw points\n" +
                "Move: Click and drag a point to move it\n" +
                "Rotate: Click and drag a node to change its heading or tangent\n" +
                "Select: Click on a path to view its type. Select a path type to change its type\n" +
                "Draw & Rotate: Hold shift to snap to 90 degrees\n" +
                "All: Right click the last point in a path to change its type\n" +
                "All: Press 'Toggle Code' to view, edit, copy, and paste the associated java code. Press escape to stop editing\n" +
                "All: Press spacebar to switch between running the code and editing it\n" +
                "All: Press cmd/ctrl z to undo, cmd/ctrl shift z to redo");
    }

    /**
     * Sets up the action panel on the side of the screen used to control path type and editing type
     * @return The action panel its created
     */
    private JPanel setupActionPanel() {
        JPanel actionPanel = new JPanel();
        actionPanel.setBackground(Color.LIGHT_GRAY);
        actionPanel.setPreferredSize(new Dimension(screen.getActionPanelWidth(), screen.getHeight()-screen.getCodePanelHeight()));
        actionPanel.setLayout(new FlowLayout());
        screen.add(actionPanel, BorderLayout.EAST);
        return actionPanel;
    }


    /**
     * Sets up the code panel at the bottom of the screen for the user to edit and view
     * @param actionPanel The side panel of the screen to add a button to
     */
    private void setupCodePanel(JPanel actionPanel) {
        //setting up the panel that holds the code
        JPanel codePanel = new JPanel();
        codePanel.setPreferredSize(new Dimension(screen.getWidth(), screen.getCodePanelHeight()));
        codePanel.setBackground(codeBackgroundColor);
        codePanel.setLayout(new BorderLayout());

        //setting up the text area that the user can write in
        codeTextArea.setBackground(codePanel.getBackground());
        codeTextArea.setForeground(codeTextColor);
        codeTextArea.setCaretColor(codeTextColor);
        //adds a keylistener that listens for the escape key that removes focus from the text area to signify that the user is done editing
        codeTextArea.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {}
            @Override
            public void keyPressed(KeyEvent keyEvent) {}
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode()==KeyEvent.VK_ESCAPE) {
                    getWindowFrame().requestFocus();
                }
            }
        });

        //using a scroll pane so that the user can scroll to view more lines of code
        JScrollPane scrollPane = new JScrollPane(codeTextArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        //adds the panels to the screen
        screen.add(codePanel, BorderLayout.SOUTH);
        codePanel.add(scrollPane, BorderLayout.CENTER);
        codePanel.setVisible(false);

        //adds a button to toggle visibility of the code
        JToggleButton toggleCode = new JToggleButton("Toggle Code");
        toggleCode.addActionListener(actionEvent -> codePanel.setVisible(!codePanel.isVisible()));
        toggleCode.setFocusable(false);
        actionPanel.add(toggleCode);
    }


    /**
     * Adds all the operation-selecting buttons to a panel
     * @param panel The panel to add the buttons to
     */
    private void setupAllRadioButtons(JPanel panel) {
        setupEditorModeRadioButtons(panel);
        setupPathRadioButtons(panel);
    }

    /**
     * Adds the path selecting buttons (spline, strafe, etc) to a panel
     */
    private void setupPathRadioButtons(JPanel panel) {
        ButtonGroup pathTypes = new ButtonGroup();
        panel.add(Box.createVerticalStrut(15));
        panel.add(new JLabel("\t" + "Path Type:"));
        //adds a new button for each value in the PathType enum
        for (int i = 0; i < RobotPath.PathType.values().length; i++) {
            JRadioButton button = new JRadioButton(RobotPath.PathType.values()[i].name());
            //sets focusable to false so that focus is not taken away from frame
            button.setFocusable(false);
            pathTypes.add(button);
            panel.add(button);
            addPathTypeActionTo(button);
            //sets the first button as selected by default
            if (i==0) {
                pathTypes.setSelected(button.getModel(), true);
                button.doClick();
            }
        }
    }

    /**
     * Adds the edit tool selection buttons (draw, move, rotate, etc.) to a panel
     */
    private void setupEditorModeRadioButtons(JPanel panel) {
        ButtonGroup editorModes = new ButtonGroup();
        panel.add(Box.createVerticalStrut(15));
        panel.add(new JLabel("\t" + "Editor Mode:"));
        for (int i = 0; i < Tool.EditorMode.values().length; i++) {
            JRadioButton button = new JRadioButton(Tool.EditorMode.values()[i].name());
            //sets focusable to false so that focus is not taken away from frame
            button.setFocusable(false);
            editorModes.add(button);
            panel.add(button);
            addEditorActionTo(button);
            //sets the first button as selected by default
            if (i==0) {
                editorModes.setSelected(button.getModel(), true);
                button.doClick();
            }
        }
    }

    /**
     * Assigns a button to its corresponding editing action enum param (draw, move, rotate, etc.) and action
     */
    private void addEditorActionTo(JRadioButton button) {
        switch (Tool.EditorMode.valueOf(button.getText())) {
            case Draw:
                button.addActionListener(actionEvent -> currentTool = new DrawTool(currentPathType, bot));
                break;
            case Move:
                button.addActionListener(actionEvent -> currentTool = new MoveTool(currentPathType, bot));
                break;
            case Select:
                button.addActionListener(actionEvent -> currentTool = new SelectTool(currentPathType, bot));
                break;
            case Rotate_Path:
                button.addActionListener(actionEvent -> currentTool = new RotateTangentTool(currentPathType, bot));
                break;
            case Rotate_Heading:
                button.addActionListener(actionEvent -> currentTool = new RotateHeadingTool(currentPathType, bot));
                break;
        }
    }

    /**
     * Assigns a button to its corresponding path type enum param (spline, strafe, etc.) and action
     */
    private void addPathTypeActionTo(AbstractButton button) {
        button.addActionListener(actionEvent -> {
            currentPathType = RobotPath.PathType.valueOf(button.getText());
            currentTool.setCurrentPathType(currentPathType);
        });
    }


    /**
     * Clears any existing actions before starting new ones
     */
    private void clearExistingActions() {
        if (bot.getCurrentActionTimeline() != null) {
            bot.getCurrentActionTimeline().getEvents().clear();
        }
    }

    /**
     * Starts simulating the bot's movement along the designed path
     */
    private void startBotPath() {
        bot.pause();
        super.removeEntity(bot);
        super.start();
        ArrayList<Action> actions = new ArrayList<>();
        currentTool.getPath().forEach(robotPath -> actions.add(robotPath.getActionInInches()));
        bot.runAction(new SequentialAction(actions));
        super.addEntity(bot);
        bot.unpause();
    }


    /**
     * Switches the window to the view that the user can draw and edit paths in
     */
    private void switchToDrawingView() {
        super.getWindowFrame().setContentPane(screen);
        super.getWindowFrame().setSize(screen.getPreferredSize().width, meepMeepContentPane.getPreferredSize().height+timerHeight);
    }
    /**
     * Switches to the Roadrunner view and runs the path created by the user
     */
    private void switchToRoadrunnerView() {
        clearExistingActions();
        startBotPath();
        super.getWindowFrame().setContentPane(meepMeepContentPane);
        super.getWindowFrame().setSize(meepMeepContentPane.getWidth(), meepMeepContentPane.getPreferredSize().height+timerHeight);
        super.start();
    }

    /**
     * Toggles between the drawing view (where the user edits paths) and the Roadrunner simulation view
     * (where the bot runs the created path).
     */
    private void toggleView() {
         drawingView = !drawingView;
         if (drawingView)
             switchToDrawingView();
         else
             switchToRoadrunnerView();
    }

    /**
     * Undoes the most recent action by popping the undo stack and updating the current tool's path and the code area.
     * The undone action is pushed to the redo stack, allowing it to be redone later.
     */
    private void undo() {
        if (undoStack.isEmpty())
            return;
        // Saves the current state for potential redo
        redoStack.push(currentTool.getActionString());
        // Restores and syncs the previous action from the undo stack
        currentTool.tryParseCodePath(undoStack.pop());
        codeTextArea.setText(currentTool.getActionString());
    }


    /**
     * Redoes the most recent undone action by popping the redo stack and updating the current tool's path and the code area.
     * The redone action is moved to the undo stack.
     */
    private void redo() {
        if (redoStack.isEmpty())
            return;
        // Saves the current state for potential undo
        undoStack.push(currentTool.getActionString());
        // Restores and syncs the previous action from the redo stack
        currentTool.tryParseCodePath(redoStack.pop());
        codeTextArea.setText(currentTool.getActionString());
    }

    @Override
    public void keyTyped(KeyEvent e) {}
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode()==KeyEvent.VK_SPACE) {
            toggleView();
        }
        if (e.getKeyCode()==KeyEvent.VK_Z && (e.isMetaDown() || e.isControlDown())) {
            if (e.isShiftDown())
                redo();
            else
                undo();
        }
    }
    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {
        if (outsideBounds(e.getPoint()))
            return;
        e.translatePoint(0,-timerHeight);
        currentTool.mouseClicked(e);
    }
    @Override
    public void mousePressed(MouseEvent e) {
        super.getWindowFrame().requestFocus();
        if (outsideBounds(e.getPoint()))
            return;
        e.translatePoint(0,-timerHeight);
        currentTool.mousePressed(e);
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (outsideBounds(e.getPoint()))
            return;
        e.translatePoint(0,-timerHeight);
        currentTool.mouseReleased(e);
    }
    @Override
    public void mouseEntered(MouseEvent e) {
        if (outsideBounds(e.getPoint()))
            return;
        e.translatePoint(0,-timerHeight);
        currentTool.mouseEntered(e);
    }
    @Override
    public void mouseExited(MouseEvent e) {
        if (outsideBounds(e.getPoint()))
            return;
        e.translatePoint(0,-timerHeight);
        currentTool.mouseExited(e);
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        if (outsideBounds(e.getPoint()))
            return;
        e.translatePoint(0,-timerHeight);
        currentTool.mouseDragged(e);
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        if (outsideBounds(e.getPoint()))
            return;
        e.translatePoint(0,-timerHeight);
        currentTool.mouseMoved(e);
    }
    private boolean outsideBounds(Point p) {
        Rectangle bounds = new Rectangle(windowSize, windowSize);
        return !bounds.contains(p);
    }
}
