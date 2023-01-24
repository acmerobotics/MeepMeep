package com.noahbres.meepmeep.roadrunner

import com.acmerobotics.roadrunner.Trajectory

fun interface AddTrajectoryCallback {
    fun buildTrajectory(drive: DriveShim): List<Trajectory>
}