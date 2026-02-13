package MeepMeepDrawing;


import com.noahbres.meepmeep.MeepMeep;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import kotlin.NoWhenBranchMatchedException;

public class DrawingScreen extends JPanel{
    private static final int ACTION_PANEL_WIDTH = 200;
    private static final int CODE_PANEL_HEIGHT = 200;
    private BufferedImage background;

    public DrawingScreen(int windowSize) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(windowSize+ACTION_PANEL_WIDTH, windowSize));
        try {
            background = backgroundToImage(MeepMeep.Background.GRID_BLUE);//Grid blue is the default background in meep meep
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected int getActionPanelWidth() {
        return ACTION_PANEL_WIDTH;
    }
    protected int getCodePanelHeight() {
        return CODE_PANEL_HEIGHT;
    }

    /**
     * Sets the background bufferedimage from a {@link MeepMeep.Background} object
     */
    public void setBackground(MeepMeep.Background background) {
        try {
            this.background = backgroundToImage(background);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        g2d.drawImage(background,0,0,getWidth()-ACTION_PANEL_WIDTH,getHeight(),null);
        Tool.paintPath(g2d);
    }

    //TODO: improve this method's access to the Background enum for easier implementation of future field backgrounds
    /** 
     * This method was copied from {@link MeepMeep#setBackground(MeepMeep.Background)}.
     * @param background The background enum constant to aquire an image from
     * @return A {@link BufferedImage} that contains the image from the background enum contstant
     */
    protected BufferedImage backgroundToImage(MeepMeep.Background background) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        BufferedImage backgroundImage;
        switch (background.ordinal()) {
            case 0:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/misc/field-grid-blue.jpg"));
                break;
            case 1:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/misc/field-grid-green.jpg"));
                break;
            case 2:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/misc/field-grid-gray.jpg"));
                break;
            case 3:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2019-skystone/field-2019-skystone-official.png"));
                break;
            case 4:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2019-skystone/field-2019-skystone-gf-dark.png"));
                break;
            case 5:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2019-skystone/field-2019-skystone-innov8rz-light.jpg"));
                break;
            case 6:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2019-skystone/field-2019-skystone-innov8rz-dark.jpg"));
                break;
            case 7:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2019-skystone/field-2019-skystone-starwars.png"));
                break;
            case 8:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2020-ultimategoal/field-2020-innov8rz-dark.jpg"));
                break;
            case 9:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2021-freightfrenzy/field-2021-official.png"));
                break;
            case 10:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2021-freightfrenzy/field-2021-adi-dark.png"));
                break;
            case 11:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2022-powerplay/field-2022-official.png"));
                break;
            case 12:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2022-powerplay/field-2022-kai-dark.png"));
                break;
            case 13:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2022-powerplay/field-2022-kai-light.png"));
                break;
            case 14:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2023-centerstage/field-2023-official.png"));
                break;
            case 15:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2023-centerstage/field-2023-juice-dark.png"));
                break;
            case 16:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2023-centerstage/field-2023-juice-light.png"));
                break;
            case 17:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2024-intothedeep/field-2024-official.png"));
                break;
            case 18:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2024-intothedeep/field-2024-juice-dark.png"));
                break;
            case 19:
                backgroundImage = ImageIO.read(classLoader.getResourceAsStream("background/season-2024-intothedeep/field-2024-juice-light.png"));
                break;
            default:
                throw new NoWhenBranchMatchedException();
        }
        return backgroundImage;
    }
}

