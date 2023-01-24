package com.noahbres.meepmeep.roadrunner

import com.acmerobotics.roadrunner.*

class SampleTankDrive {
    companion object {
        @JvmStatic
        fun getVelocityConstraint(
            maxVel: Double,
            maxAngularVel: Double,
            trackWidth: Double
        ): VelConstraint {
            return MinVelConstraint(
                listOf(
                    AngularVelConstraint(maxAngularVel),
                    TankKinematics(trackWidth).WheelVelConstraint(maxVel),
                )
            )
        }

        @JvmStatic
        fun getAccelerationConstraint(maxAccel: Double): AccelConstraint? {
            return ProfileAccelConstraint(-maxAccel, maxAccel)
        }
    }
}