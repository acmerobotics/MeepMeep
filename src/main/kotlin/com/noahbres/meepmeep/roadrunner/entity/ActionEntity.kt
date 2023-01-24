package com.noahbres.meepmeep.roadrunner.entity

import com.acmerobotics.roadrunner.*
import com.noahbres.meepmeep.MeepMeep
import com.noahbres.meepmeep.core.colorscheme.ColorScheme
import com.noahbres.meepmeep.core.entity.ThemedEntity
import com.noahbres.meepmeep.core.toScreenCoord
import com.noahbres.meepmeep.core.util.FieldUtil
import java.awt.*
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.lang.RuntimeException
import kotlin.math.roundToInt

class TrajectoryAction(val t: TimeTrajectory) : Action {
    override fun run(p: com.acmerobotics.dashboard.telemetry.TelemetryPacket) = TODO()
}
class TurnAction(val t: TimeTurn) : Action {
    override fun run(p: com.acmerobotics.dashboard.telemetry.TelemetryPacket) = TODO()
}

fun actionTimeline(a: Action): Pair<Double, List<Pair<Double, Action>>> {
    val timeline = mutableListOf<Pair<Double, Action>>()

    fun aux(t0: Double, a: Action): Double {
        when (a) {
            is SequentialAction -> {
                var t = t0
                for (a2 in a.initialActions) {
                    t = aux(t, a2)
                }
                return t
            }

            is ParallelAction -> {
                return a.initialActions.maxOf { a2 ->
                    aux(t0, a2)
                }
            }

            is TrajectoryAction -> {
                timeline.add(Pair(t0, a))
                return t0 + a.t.profile.duration
            }

            is TurnAction -> {
                timeline.add(Pair(t0, a))
                return t0 + a.t.profile.duration
            }

            is SleepAction -> {
                return t0 + a.dt
            }

            else -> {
                throw RuntimeException()
            }
        }
    }

    val dt = aux(0.0, a)

    timeline.sortBy { it.first }

    return Pair(dt, timeline)
}

class ActionEntity(
    override val meepMeep: MeepMeep,
    private val action: Action,
    private var colorScheme: ColorScheme
) : ThemedEntity {
    companion object {
        const val PATH_INNER_STROKE_WIDTH = 0.5
        const val PATH_OUTER_STROKE_WIDTH = 2.0

        const val PATH_OUTER_OPACITY = 0.4

        const val PATH_UNFOCUSED_OPACTIY = 0.3

        const val SAMPLE_RESOLUTION = 1.2
    }

    private var canvasWidth = FieldUtil.CANVAS_WIDTH
    private var canvasHeight = FieldUtil.CANVAS_HEIGHT

    override val tag = "TRAJECTORY_SEQUENCE_ENTITY"

    override var zIndex: Int = 0

    private val turnEntityList = mutableListOf<TurnIndicatorEntity>()
    val markerEntityList = mutableListOf<MarkerIndicatorEntity>()

    private lateinit var baseBufferedImage: BufferedImage

    private var currentSegmentImage: BufferedImage? = null

    private var lastSegment: TrajectoryAction? = null
    private var currentSegment: TrajectoryAction? = null

    var trajectoryProgress: Double? = null

    val timeline = actionTimeline(action).second

    init {
        redrawPath()
    }

    private fun redrawPath() {
        // Request to clear previous turn indicator entities
        turnEntityList.forEach {
            meepMeep.requestToRemoveEntity(it)
        }
        turnEntityList.clear()

        // Request to clear previous marker indicator entities
        markerEntityList.forEach {
            meepMeep.requestToRemoveEntity(it)
        }
        markerEntityList.clear()

        val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val device = environment.defaultScreenDevice
        val config = device.defaultConfiguration

        baseBufferedImage = config.createCompatibleImage(
            canvasWidth.toInt(), canvasHeight.toInt(), Transparency.TRANSLUCENT
        )
        val gfx = baseBufferedImage.createGraphics()

        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        gfx.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        val trajectoryDrawnPath = Path2D.Double()

        val innerStroke = BasicStroke(
            FieldUtil.scaleInchesToPixel(PATH_INNER_STROKE_WIDTH).toFloat(),
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND
        )
//        val outerStroke = BasicStroke(
//            FieldUtil.scaleInchesToPixel(PATH_OUTER_STROKE_WIDTH).toFloat(),
//            BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND
//        )

        var first = true

//        val firstVec = action.start().vec().toScreenCoord()
//        trajectoryDrawnPath.moveTo(firstVec.x, firstVec.y)

        for ((t0, action) in timeline) {
            when (action) {
                is TrajectoryAction -> {
                    val displacementSamples = (action.t.path.length() / SAMPLE_RESOLUTION).roundToInt()

                    val displacements = (0..displacementSamples).map {
                        it / displacementSamples.toDouble() * action.t.path.length()
                    }

                    val poses = displacements.map { action.t.path[it, 1].value() }

                    for (pose in poses.drop(1)) {
                        val coord = pose.trans.toScreenCoord()
                        if (first) {
                            trajectoryDrawnPath.moveTo(coord.x, coord.y)
                            first = false
                        } else {
                            trajectoryDrawnPath.lineTo(coord.x, coord.y)
                        }
                    }
                }
                is TurnAction -> {
                    val turnEntity = TurnIndicatorEntity(
                        meepMeep, colorScheme, action.t.beginPose.trans, action.t.beginPose.rot.log(),
                        (action.t.beginPose.rot + action.t.angle).log()
                    )
                    turnEntityList.add(turnEntity)
                    meepMeep.requestToAddEntity(turnEntity)
                }
            }
        }

        var poseSupplier: (Double) -> Pose2d = { Pose2d(0.0, 0.0, 0.0) }
        for ((t0, action) in timeline) {
            when (action) {
                is SleepAction -> {}
                is TurnAction -> {
                    poseSupplier = { action.t[it - t0].value() }
                }
                is TrajectoryAction -> {
                    poseSupplier = { action.t[it - t0].value() }
                }
                else -> {
                    val markerEntity = MarkerIndicatorEntity(meepMeep, colorScheme, poseSupplier(t0), t0)
                    markerEntityList.add(markerEntity)
                    meepMeep.requestToAddEntity(markerEntity)
                }
            }
        }

//        gfx.stroke = outerStroke
//        gfx.color = Color(
//                colorScheme.TRAJCETORY_PATH_COLOR.red, colorScheme.TRAJCETORY_PATH_COLOR.green,
//                colorScheme.TRAJCETORY_PATH_COLOR.blue, (PATH_OUTER_OPACITY * 255).toInt()
//        )
//        gfx.draw(trajectoryDrawnPath)

        gfx.stroke = innerStroke
        gfx.color = colorScheme.TRAJCETORY_PATH_COLOR
        gfx.color = Color(
            colorScheme.TRAJCETORY_PATH_COLOR.red, colorScheme.TRAJCETORY_PATH_COLOR.green,
            colorScheme.TRAJCETORY_PATH_COLOR.blue, (PATH_UNFOCUSED_OPACTIY * 255).toInt()

        )
        gfx.draw(trajectoryDrawnPath)
    }

    private fun redrawCurrentSegment() {
        if (currentSegment == null) {
            currentSegmentImage = null
            return
        }

        val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val device = environment.defaultScreenDevice
        val config = device.defaultConfiguration

        currentSegmentImage = config.createCompatibleImage(
            canvasWidth.toInt(), canvasHeight.toInt(), Transparency.TRANSLUCENT
        )
        val gfx = currentSegmentImage!!.createGraphics()

        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        gfx.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        val trajectoryDrawnPath = Path2D.Double()

        val outerStroke = BasicStroke(
            FieldUtil.scaleInchesToPixel(PATH_OUTER_STROKE_WIDTH).toFloat(),
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND
        )
        val innerStroke = BasicStroke(
            FieldUtil.scaleInchesToPixel(PATH_INNER_STROKE_WIDTH).toFloat(),
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND
        )

        val traj = currentSegment!!.t

        val firstVec = currentSegment!!.t.path.begin(1).trans.value().toScreenCoord()
        trajectoryDrawnPath.moveTo(firstVec.x, firstVec.y)

        val displacementSamples = (traj.path.length() / SAMPLE_RESOLUTION).roundToInt()

        val displacements = (0..displacementSamples).map {
            it / displacementSamples.toDouble() * traj.path.length()
        }

        val poses = displacements.map { traj.path[it, 1].value() }

        for (pose in poses.drop(1)) {
            val coord = pose.trans.toScreenCoord()
            trajectoryDrawnPath.lineTo(coord.x, coord.y)
        }

        gfx.stroke = outerStroke
        gfx.color = Color(
            colorScheme.TRAJCETORY_PATH_COLOR.red, colorScheme.TRAJCETORY_PATH_COLOR.green,
            colorScheme.TRAJCETORY_PATH_COLOR.blue, (PATH_OUTER_OPACITY * 255).toInt()
        )
        gfx.draw(trajectoryDrawnPath)

        gfx.stroke = innerStroke
        gfx.color = colorScheme.TRAJCETORY_PATH_COLOR
        gfx.draw(trajectoryDrawnPath)
    }

    override fun update(deltaTime: Long) {
        currentSegment = if (trajectoryProgress == null) {
            null
        } else {
            (timeline
                .filter { (_, a) -> a is TrajectoryAction } as List<Pair<Double, TrajectoryAction>>)
                .firstOrNull { (t0, a) -> trajectoryProgress!! < (t0 + a.t.duration) }
                ?.second
        }

        if (lastSegment != currentSegment) {
            redrawCurrentSegment()
        }

        lastSegment = currentSegment
    }

    override fun render(gfx: Graphics2D, canvasWidth: Int, canvasHeight: Int) {
        gfx.drawImage(baseBufferedImage, null, 0, 0)

        if (currentSegmentImage != null) gfx.drawImage(currentSegmentImage, null, 0, 0)
    }

    override fun setCanvasDimensions(canvasWidth: Double, canvasHeight: Double) {
        if (this.canvasWidth != canvasWidth || this.canvasHeight != canvasHeight) redrawPath()
        this.canvasWidth = canvasWidth
        this.canvasHeight = canvasHeight
    }

    override fun switchScheme(scheme: ColorScheme) {
        if (this.colorScheme != scheme) {
            this.colorScheme = scheme
            redrawPath()
        }
    }
}