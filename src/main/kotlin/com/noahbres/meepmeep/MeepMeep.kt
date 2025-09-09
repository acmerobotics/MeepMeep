package com.noahbres.meepmeep

import com.acmerobotics.roadrunner.Vector2d
import com.noahbres.meepmeep.core.colorscheme.ColorManager
import com.noahbres.meepmeep.core.colorscheme.ColorScheme
import com.noahbres.meepmeep.core.entity.*
import com.noahbres.meepmeep.core.ui.WindowFrame
import com.noahbres.meepmeep.core.util.FieldUtil
import com.noahbres.meepmeep.core.util.LoopManager
import com.noahbres.meepmeep.roadrunner.entity.RoadRunnerBotEntity
import com.noahbres.meepmeep.roadrunner.ui.TrajectoryProgressSliderMaster
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EtchedBorder


open class MeepMeep @JvmOverloads constructor(private val windowSize: Int, fps: Int = 60) {
    companion object {
        @JvmStatic
        lateinit var DEFAULT_AXES_ENTITY: AxesEntity

        @JvmStatic
        lateinit var DEFAULT_COMPASS_ENTITY: CompassEntity

        // Custom Fonts
        @JvmStatic
        lateinit var FONT_CMU_BOLD_LIGHT: Font

        @JvmStatic
        lateinit var FONT_CMU: Font

        @JvmStatic
        lateinit var FONT_CMU_BOLD: Font
    }

    val windowFrame = WindowFrame("Meep Meep", windowSize)
    val canvas = windowFrame.canvas

    val colorManager = ColorManager()

    private var bg: Image? = null

    private val entityList = mutableListOf<Entity>()
    private val requestedAddEntityList = mutableListOf<Entity>()
    private val requestedRemoveEntityList = mutableListOf<Entity>()

    private val zIndexManager = ZIndexManager();

    // TODO: Make custom dirty list that auto sorts
    // Returns true if entity list needs to be sorted
    private var entityListDirty = false

    private var bgAlpha = 1.0f

    private val render: () -> Unit = {
        val g = canvas.bufferStrat.drawGraphics as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.clearRect(0, 0, canvas.width, canvas.height)

        // render
        if (bg != null) {
            if (bgAlpha < 1.0f) {
                val resetComposite = g.composite
                val alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bgAlpha)
                g.composite = alphaComposite
                g.drawImage(bg, 0, 0, null)
                g.composite = resetComposite
            } else {
                g.drawImage(bg, 0, 0, null)
            }
        }

        entityList.forEach { it.render(g, canvas.width, canvas.height) }

        // Draw fps
        g.font = Font("Sans", Font.BOLD, 20)
        g.color = ColorManager.COLOR_PALETTE.GREEN_600
        g.drawString("%.1f FPS".format(loopManager.fps), 10, 20)

        // Draw mouse coords
        val mouseToFieldCoords = FieldUtil.screenCoordsToFieldCoords(
            Vector2d(
                canvasMouseX.toDouble(),
                canvasMouseY.toDouble()
            )
        )

        g.font = Font("Sans", Font.BOLD, 14)
        g.color =
            if (colorManager.isDarkMode) ColorManager.COLOR_PALETTE.GRAY_100 else ColorManager.COLOR_PALETTE.GRAY_800

        g.drawString(
            "(%.1f, %.1f)".format(
                mouseToFieldCoords.x,
                mouseToFieldCoords.y,
            ), 10, canvas.height - 8
        )

        g.dispose()
        canvas.bufferStrat.show()
    }

    private val update: (deltaTime: Long) -> Unit = { deltaTime ->
        if (entityListDirty) {
            requestedRemoveEntityList.forEach {
                removeEntity(it)
            }
            requestedRemoveEntityList.clear();

            requestedAddEntityList.forEach {
                addEntity(it)
            }
            requestedAddEntityList.clear();

            entityList.sortBy { it.zIndex }
            entityListDirty = false
        }

        val originalSize = entityList.size
        for (i in 0 until originalSize) {
            entityList[i].update(deltaTime)
        }
    }

    private val loopManager = LoopManager(fps, update, render)

    // Road Runner UI Elements

    // Handles progress slider elements
    private val progressSliderMasterPanel: TrajectoryProgressSliderMaster by lazy {
        TrajectoryProgressSliderMaster(
            this,
            FieldUtil.CANVAS_WIDTH.toInt(),
            20
        )
    }

    private var canvasMouseX = 0
    private var canvasMouseY = 0

    init {
        // Core init
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        windowFrame.contentPane.background = colorManager.theme.UI_MAIN_BG
        windowFrame.canvasPanel.background = colorManager.theme.UI_MAIN_BG

        val classLoader = Thread.currentThread().contextClassLoader

        FONT_CMU_BOLD_LIGHT = Font.createFont(
            Font.TRUETYPE_FONT, classLoader.getResourceAsStream("font/cmunbi.ttf")
        ).deriveFont(20f)
        FONT_CMU = Font.createFont(Font.TRUETYPE_FONT, classLoader.getResourceAsStream("font/cmunrm.ttf"))
        FONT_CMU_BOLD = Font.createFont(Font.TRUETYPE_FONT, classLoader.getResourceAsStream("font/cmunbx.ttf"))

        FieldUtil.CANVAS_WIDTH = windowSize.toDouble()
        FieldUtil.CANVAS_HEIGHT = windowSize.toDouble()

        DEFAULT_AXES_ENTITY = AxesEntity(this, 0.8, colorManager.theme, FONT_CMU_BOLD_LIGHT, 20f)
        DEFAULT_COMPASS_ENTITY = CompassEntity(
            this, colorManager.theme, 30.0, 30.0, Vector2d(-54.0, 54.0)
        )

        // Handle UI
        windowFrame.canvasPanel.add(progressSliderMasterPanel)

        windowFrame.pack()

        canvas.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseDragged(p0: MouseEvent?) {}

            override fun mouseMoved(e: MouseEvent) {
                canvasMouseX = e.x
                canvasMouseY = e.y
            }
        })

        canvas.addKeyListener(object : KeyListener {
            override fun keyTyped(p0: KeyEvent?) {}

            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_C) {
                    // Draw mouse coords
                    val mouseToFieldCoords = FieldUtil.screenCoordsToFieldCoords(
                        Vector2d(
                            canvasMouseX.toDouble(),
                            canvasMouseY.toDouble()
                        )
                    )

                    val stringSelection = StringSelection(
                        "%.1f, %.1f".format(
                            mouseToFieldCoords.x,
                            mouseToFieldCoords.y,
                        )
                    )
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(stringSelection, null)
                }
            }

            override fun keyReleased(p0: KeyEvent?) {}
        })

        // Entities
        zIndexManager.setTagHierarchy(
            "RR_BOT_ENTITY",
            "TURN_INDICATOR_ENTITY",
            "MARKER_INDICATOR_ENTITY",
            "ACTION_ENTITY",
            "COMPASS_ENTITY",
            "AXES_ENTITY",
        )

        addEntity(DEFAULT_AXES_ENTITY)
        addEntity(DEFAULT_COMPASS_ENTITY)
    }

    fun start(): MeepMeep {
        if (bg == null) setBackground(Background.GRID_BLUE)
        windowFrame.isVisible = true

        // Default added entities are initialized before color schemes are set
        // Thus make sure to reset them
        entityList.forEach {
            if (it is ThemedEntity) it.switchScheme(colorManager.theme)
            if (it is RoadRunnerBotEntity) it.start()
        }

        onCanvasResize()

        loopManager.start()

        return this
    }

    //-------------Theme Settings-------------//
    fun setBackground(background: Background = Background.GRID_BLUE): MeepMeep {
        val classLoader = Thread.currentThread().contextClassLoader

        fun rotated(im: BufferedImage, angle: Double): BufferedImage {
            val rotatedImage = BufferedImage(im.width, im.height, im.type)
            val graphics2D = rotatedImage.createGraphics()
            graphics2D.rotate(angle, im.width / 2.0, im.height / 2.0)
            graphics2D.drawImage(im, null, 0, 0)
            return rotatedImage
        }

        bg = when (background) {
            Background.GRID_BLUE -> {
                colorManager.isDarkMode = false
                ImageIO.read(classLoader.getResourceAsStream("background/misc/field-grid-blue.jpg"))
            }
            Background.GRID_GREEN -> {
                colorManager.isDarkMode = false
                ImageIO.read(classLoader.getResourceAsStream("background/misc/field-grid-green.jpg"))
            }
            Background.GRID_GRAY -> {
                colorManager.isDarkMode = false
                ImageIO.read(classLoader.getResourceAsStream("background/misc/field-grid-gray.jpg"))
            }
            Background.FIELD_SKYSTONE_OFFICIAL -> {
                colorManager.isDarkMode = false
                ImageIO.read(classLoader.getResourceAsStream("background/season-2019-skystone/field-2019-skystone-official.png"))
            }
            Background.FIELD_SKYSTONE_GF_DARK -> {
                colorManager.isDarkMode = true
                ImageIO.read(classLoader.getResourceAsStream("background/season-2019-skystone/field-2019-skystone-gf-dark.png"))
            }
            Background.FIELD_SKYSTONE_INNOV8RZ_LIGHT -> {
                colorManager.isDarkMode = false
                ImageIO.read(classLoader.getResourceAsStream("background/season-2019-skystone/field-2019-skystone-innov8rz-light.jpg"))
            }
            Background.FIELD_SKYSTONE_INNOV8RZ_DARK -> {
                colorManager.isDarkMode = true
                ImageIO.read(classLoader.getResourceAsStream("background/season-2019-skystone/field-2019-skystone-innov8rz-dark.jpg"))
            }
            Background.FIELD_SKYSTONE_STARWARS_DARK -> {
                colorManager.isDarkMode = true
                ImageIO.read(classLoader.getResourceAsStream("background/season-2019-skystone/field-2019-skystone-starwars.png"))
            }
            Background.FIELD_ULTIMATEGOAL_INNOV8RZ_DARK -> {
                colorManager.isDarkMode = true
                ImageIO.read(classLoader.getResourceAsStream("background/season-2020-ultimategoal/field-2020-innov8rz-dark.jpg"))
            }
            Background.FIELD_FREIGHTFRENZY_OFFICIAL -> {
                colorManager.isDarkMode = false
                ImageIO.read(classLoader.getResourceAsStream("background/season-2021-freightfrenzy/field-2021-official.png"))
            }
            Background.FIELD_FREIGHTFRENZY_ADI_DARK -> {
                colorManager.isDarkMode = true
                ImageIO.read(classLoader.getResourceAsStream("background/season-2021-freightfrenzy/field-2021-adi-dark.png"))
            }
            Background.FIELD_POWERPLAY_OFFICIAL -> {
                colorManager.isDarkMode = false
                ImageIO.read(classLoader.getResourceAsStream("background/season-2022-powerplay/field-2022-official.png"))
            }
            Background.FIELD_POWERPLAY_KAI_DARK -> {
                colorManager.isDarkMode = true
                ImageIO.read(classLoader.getResourceAsStream("background/season-2022-powerplay/field-2022-kai-dark.png"))
            }
            Background.FIELD_POWERPLAY_KAI_LIGHT -> {
                colorManager.isDarkMode = false
                ImageIO.read(classLoader.getResourceAsStream("background/season-2022-powerplay/field-2022-kai-light.png"))
            }
            Background.FIELD_CENTERSTAGE_OFFICIAL -> {
                colorManager.isDarkMode = false
                rotated(ImageIO.read(classLoader.getResourceAsStream("background/season-2023-centerstage/field-2023-official.png")), Math.toRadians(90.0))
            }
            Background.FIELD_CENTERSTAGE_JUICE_DARK -> {
                colorManager.isDarkMode = true
                rotated(ImageIO.read(classLoader.getResourceAsStream("background/season-2023-centerstage/field-2023-juice-dark.png")), Math.toRadians(90.0))
            }
            Background.FIELD_CENTERSTAGE_JUICE_LIGHT -> {
                colorManager.isDarkMode = false
                rotated(ImageIO.read(classLoader.getResourceAsStream("background/season-2023-centerstage/field-2023-juice-light.png")), Math.toRadians(90.0))
            }
            Background.FIELD_INTO_THE_DEEP_OFFICIAL -> {
                colorManager.isDarkMode = false
                ImageIO.read(classLoader.getResourceAsStream("background/season-2024-intothedeep/field-2024-official.png"))
            }
            Background.FIELD_INTO_THE_DEEP_JUICE_DARK -> {
                colorManager.isDarkMode = true
                ImageIO.read(classLoader.getResourceAsStream("background/season-2024-intothedeep/field-2024-juice-dark.png"))
            }
            Background.FIELD_INTO_THE_DEEP_JUICE_LIGHT -> {
                colorManager.isDarkMode = false
                ImageIO.read(classLoader.getResourceAsStream("background/season-2024-intothedeep/field-2024-juice-light.png"))
            }
            Background.FIELD_DECODE_OFFICIAL -> {
                colorManager.isDarkMode = false
                ImageIO.read(classLoader.getResourceAsStream("background/season-2025-decode/field-2025-official.png"))
            }
            Background.FIELD_DECODE_JUICE_DARK -> {
                colorManager.isDarkMode = true
                ImageIO.read(classLoader.getResourceAsStream("background/season-2025-decode/field-2025-juice-dark.png"))
            }
            Background.FIELD_DECODE_JUICE_BLACK -> {
                colorManager.isDarkMode = true
                ImageIO.read(classLoader.getResourceAsStream("background/season-2025-decode/field-2025-juice-black.png"))
            }
            Background.FIELD_DECODE_JUICE_PAPER -> {
                colorManager.isDarkMode = false
                ImageIO.read(classLoader.getResourceAsStream("background/season-2025-decode/field-2025-juice-paper.png"))
            }
            Background.FIELD_DECODE_JUICE_LIGHT -> {
                colorManager.isDarkMode = false
                ImageIO.read(classLoader.getResourceAsStream("background/season-2025-decode/field-2025-juice-light.png"))
            }
        }.getScaledInstance(windowSize, windowSize, Image.SCALE_SMOOTH)

        refreshTheme()

        return this
    }

    fun setBackground(image: Image): MeepMeep {
        bg = image.getScaledInstance(windowSize, windowSize, Image.SCALE_SMOOTH)

        return this
    }

    @JvmOverloads
    fun setTheme(schemeLight: ColorScheme, schemeDark: ColorScheme = schemeLight): MeepMeep {
        colorManager.setTheme(schemeLight, schemeDark)

        refreshTheme()

        return this
    }

    open fun refreshTheme() {
        // Core Refresh
        entityList.forEach {
            if (it is ThemedEntity) it.switchScheme(colorManager.theme)
        }

        windowFrame.contentPane.background = colorManager.theme.UI_MAIN_BG
        windowFrame.canvasPanel.background = colorManager.theme.UI_MAIN_BG
    }

    fun setDarkMode(isDarkMode: Boolean): MeepMeep {
        colorManager.isDarkMode = isDarkMode

        return this
    }

    private fun onCanvasResize() {
        FieldUtil.CANVAS_WIDTH = windowSize.toDouble()
        FieldUtil.CANVAS_HEIGHT = windowSize.toDouble()

        entityList.forEach {
            it.setCanvasDimensions(FieldUtil.CANVAS_WIDTH, FieldUtil.CANVAS_HEIGHT)
        }
    }

    //-------------Axes Settings-------------//
    fun setAxesInterval(interval: Int): MeepMeep {
        if (DEFAULT_AXES_ENTITY in entityList) DEFAULT_AXES_ENTITY.setInterval(interval)

        return this
    }

    //-------------Entity-------------//
    fun addEntity(entity: Entity): MeepMeep {
        zIndexManager.addEntity(entity)

        entityList.add(entity)
        entityListDirty = true

        if (entity is MouseListener) canvas.addMouseListener(entity)

        if (entity is MouseMotionListener) canvas.addMouseMotionListener(entity)

        if (entity is RoadRunnerBotEntity)
            progressSliderMasterPanel.addRoadRunnerBot(entity)

        if (entity is EntityEventListener)
            entity.onAddToEntityList()

        return this
    }

    fun removeEntity(entity: Entity): MeepMeep {
        entityList.remove(entity)
        requestedAddEntityList.remove(entity)
        entityListDirty = true

        if (entity is MouseListener) canvas.removeMouseListener(entity)

        if (entity is MouseMotionListener) canvas.removeMouseMotionListener(entity)

        if (entity is RoadRunnerBotEntity)
            progressSliderMasterPanel.removeRoadRunnerBot(entity)

        if (entity is EntityEventListener)
            entity.onRemoveFromEntityList()

        return this
    }

    fun requestToAddEntity(entity: Entity): MeepMeep {
        requestedAddEntityList.add(entity)
        entityListDirty = true

        return this
    }


    fun requestToRemoveEntity(entity: Entity): MeepMeep {
        requestedRemoveEntityList.add(entity)
        entityListDirty = true

        return this
    }

    //-------------Misc-------------//
    fun setBackgroundAlpha(alpha: Float): MeepMeep {
        bgAlpha = alpha

        return this
    }

    enum class Background {
        GRID_BLUE,
        GRID_GREEN,
        GRID_GRAY,
        FIELD_SKYSTONE_OFFICIAL,
        FIELD_SKYSTONE_GF_DARK,
        FIELD_SKYSTONE_INNOV8RZ_LIGHT,
        FIELD_SKYSTONE_INNOV8RZ_DARK,
        FIELD_SKYSTONE_STARWARS_DARK,
        FIELD_ULTIMATEGOAL_INNOV8RZ_DARK,
        FIELD_FREIGHTFRENZY_OFFICIAL,
        FIELD_FREIGHTFRENZY_ADI_DARK,
        FIELD_POWERPLAY_OFFICIAL,
        FIELD_POWERPLAY_KAI_DARK,
        FIELD_POWERPLAY_KAI_LIGHT,
        FIELD_CENTERSTAGE_OFFICIAL,
        FIELD_CENTERSTAGE_JUICE_DARK,
        FIELD_CENTERSTAGE_JUICE_LIGHT,
        FIELD_INTO_THE_DEEP_OFFICIAL,
        FIELD_INTO_THE_DEEP_JUICE_DARK,
        FIELD_INTO_THE_DEEP_JUICE_LIGHT,
        FIELD_DECODE_OFFICIAL,
        FIELD_DECODE_JUICE_DARK,
        FIELD_DECODE_JUICE_BLACK,
        FIELD_DECODE_JUICE_PAPER,
        FIELD_DECODE_JUICE_LIGHT,
    }
}
