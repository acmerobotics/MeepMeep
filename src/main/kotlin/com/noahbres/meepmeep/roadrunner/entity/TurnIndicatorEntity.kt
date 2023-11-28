package com.noahbres.meepmeep.roadrunner.entity

import com.acmerobotics.roadrunner.Rotation2d
import com.acmerobotics.roadrunner.Vector2d
import com.noahbres.meepmeep.MeepMeep
import com.noahbres.meepmeep.core.*
import com.noahbres.meepmeep.core.colorscheme.ColorScheme
import com.noahbres.meepmeep.core.entity.ThemedEntity
import com.noahbres.meepmeep.core.util.FieldUtil
import java.awt.BasicStroke
import java.awt.Graphics2D
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class TurnIndicatorEntity(
        override val meepMeep: MeepMeep,
        private var colorScheme: ColorScheme,
        private val pos: Vector2d,
        private val startAngle: Rotation2d,
        private val angle: Double,
) : ThemedEntity {
    private var canvasWidth = FieldUtil.CANVAS_WIDTH
    private var canvasHeight = FieldUtil.CANVAS_HEIGHT

    override val tag = "TURN_INDICATOR_ENTITY"

    override var zIndex: Int = 0

    private val TURN_CIRCLE_RADIUS = 1.0
    private val TURN_ARC_RADIUS = 7.5
    private val TURN_STROKE_WIDTH = 0.5
    private val TURN_ARROW_LENGTH = 1.5
    private val TURN_ARROW_ANGLE = (30.0).toRadians()
    private val TURN_ARROW_ANGLE_ADJUSTMENT = (10.0).toRadians()

    override fun update(deltaTime: Long) {
    }

    override fun render(gfx: Graphics2D, canvasWidth: Int, canvasHeight: Int) {
        gfx.color = colorScheme.TRAJECTORY_TURN_COLOR
        gfx.stroke = BasicStroke(TURN_STROKE_WIDTH.scaleInToPixel().toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        gfx.fill(Ellipse2D.Double(
            (pos.toScreenCoord().x - TURN_CIRCLE_RADIUS.scaleInToPixel() / 2),
            (pos.toScreenCoord().y - TURN_CIRCLE_RADIUS.scaleInToPixel() / 2),
            TURN_CIRCLE_RADIUS.scaleInToPixel(), TURN_CIRCLE_RADIUS.scaleInToPixel()
        ))

        if (angle >= 2 * PI) {
            gfx.draw(Ellipse2D.Double(
                (pos.toScreenCoord().x - TURN_ARC_RADIUS.scaleInToPixel() / 2),
                (pos.toScreenCoord().y - TURN_ARC_RADIUS.scaleInToPixel() / 2),
                TURN_ARC_RADIUS.scaleInToPixel(), TURN_ARC_RADIUS.scaleInToPixel()
            ))
        } else {
            gfx.draw(Arc2D.Double(
                (pos.toScreenCoord().x - TURN_ARC_RADIUS.scaleInToPixel() / 2),
                (pos.toScreenCoord().y - TURN_ARC_RADIUS.scaleInToPixel() / 2),
                TURN_ARC_RADIUS.scaleInToPixel(), TURN_ARC_RADIUS.scaleInToPixel(),
                startAngle.log().toDegrees(), angle.toDegrees(),
                Arc2D.OPEN
            ))
        }

        val arrowBasePoint = pos + ((startAngle + angle) * Vector2d(TURN_ARC_RADIUS / 2, 0.0))

        val arrowVec1 = (startAngle + angle + (-TURN_ARROW_ANGLE - PI / 2 - TURN_ARROW_ANGLE_ADJUSTMENT)) * Vector2d(TURN_ARROW_LENGTH, 0.0)
        val arrowVec2 = (startAngle + angle + (+TURN_ARROW_ANGLE - PI / 2 - TURN_ARROW_ANGLE_ADJUSTMENT)) * Vector2d(TURN_ARROW_LENGTH, 0.0)

        val arrowStartScreen = arrowBasePoint.toScreenCoord()
        val arrowEndScreen1 = (arrowBasePoint + arrowVec1).toScreenCoord()
        val arrowEndScreen2 = (arrowBasePoint + arrowVec2).toScreenCoord()

        gfx.draw(Line2D.Double(
            arrowStartScreen.x, arrowStartScreen.y,
            arrowEndScreen1.x, arrowEndScreen1.y
        ))
        gfx.draw(Line2D.Double(
            arrowStartScreen.x, arrowStartScreen.y,
            arrowEndScreen2.x, arrowEndScreen2.y
        ))
    }

    override fun setCanvasDimensions(canvasWidth: Double, canvasHeight: Double) {
        this.canvasWidth = canvasWidth
        this.canvasHeight = canvasHeight
    }

    override fun switchScheme(scheme: ColorScheme) {
        this.colorScheme = scheme
    }
}