package com.noahbres.meepmeep.roadrunner

import com.acmerobotics.roadrunner.*
import com.noahbres.meepmeep.roadrunner.entity.TrajectoryActionStub
import com.noahbres.meepmeep.roadrunner.entity.TurnActionStub

class DriveShim(driveTrainType: DriveTrainType, private val constraints: Constraints, var poseEstimate: Pose2d) {
    private val velConstraint = when (driveTrainType) {
        DriveTrainType.MECANUM -> MinVelConstraint(
            listOf(
                AngularVelConstraint(constraints.maxAngVel),
                MecanumKinematics(constraints.trackWidth).WheelVelConstraint(constraints.maxVel),
            )
        )
        DriveTrainType.TANK -> MinVelConstraint(
            listOf(
                AngularVelConstraint(constraints.maxAngVel),
                TankKinematics(constraints.trackWidth).WheelVelConstraint(constraints.maxVel),
            )
        )
    }

    private val accelConstraint = ProfileAccelConstraint(-constraints.maxAccel, constraints.maxAccel)

    fun actionBuilder(startPose: Pose2d): TrajectoryActionBuilder {
        return TrajectoryActionBuilder(
            ::TurnActionStub,
            ::TrajectoryActionStub,
            TrajectoryBuilderParams(
                1e-6,
                ProfileParams(
                    0.25, 0.1, 1e-2
                )
            ),
            startPose, 0.0,
            TurnConstraints(
                constraints.maxAngVel,
                -constraints.maxAngAccel,
                constraints.maxAngAccel,
            ),
            velConstraint,
            accelConstraint,
        )
    }
}