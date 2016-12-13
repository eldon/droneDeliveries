package com.dji.sdk.sample.missionmanager;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.dji.sdk.sample.common.Utils;

import java.util.LinkedList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.missionmanager.DJICustomMission;
import dji.sdk.missionmanager.DJIFollowMeMission;
import dji.sdk.missionmanager.DJIHotPointMission;
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIPanoramaMission;
import dji.sdk.missionmanager.DJIWaypoint;
import dji.sdk.missionmanager.DJIWaypointMission;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.missionmanager.DJIMission;

/**
 * Class for waypoint mission.
 */
public class WaypointMissionView extends MissionManagerBaseView {

    public WaypointMissionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.pow(Math.E, -x));
    }

    private double getTrajectory(double x, double r_d, float a_f, float a_d, float b) {
        return a_f - (a_f - a_d) * sigmoid((r_d * x) + b);
    }

    @Override
    protected DJIMission initMission() {
        short NUM_WAYPOINTS = 5;
        short STARTING_DISTANCE = 20;  // meters
        float ALT_FLYING = Float.valueOf(mAF.getText().toString());  // meters
        float ALT_DELIVERY = Float.valueOf(mAD.getText().toString());  // meters
        double RATE_DESCENT = Double.valueOf(mRD.getText().toString());
        float B_OFFSET = Float.valueOf(mBB.getText().toString());

//        Log.d("SIGMOID SANITY CHECK", "START");
//        Log.d("SIG(1)", String.valueOf(sigmoid(1.0)));
//        Log.d("SIG(-1)", String.valueOf(sigmoid(-1.0)));
//        Log.d("SIG(0.27)", String.valueOf(sigmoid(0.27)));
//
//        Log.d("TRAJECTORY SANITY CHECK", "START");
//        Log.d("TRAJ(")



        if (!Utils.checkGpsCoordinate(mHomeLatitude, mHomeLongitude)) {
            Utils.setResultToToast(mContext, "No home point!!!");
            return null;
        }

        StringBuffer trajSB = new StringBuffer();

        // Step 1: create mission
        DJIWaypointMission waypointMission = new DJIWaypointMission();
        waypointMission.maxFlightSpeed = 15;
        waypointMission.autoFlightSpeed = 2;
        List<DJIWaypoint> waypointsList = new LinkedList<>();

        for (int i=0; i<NUM_WAYPOINTS + 1; i++) {
            double y_offset = STARTING_DISTANCE - i*(STARTING_DISTANCE/NUM_WAYPOINTS);
            double y_pos = mHomeLongitude + (y_offset * Utils.ONE_METER_OFFSET);
            float alt = (float) getTrajectory(-y_offset, RATE_DESCENT, ALT_FLYING, ALT_DELIVERY, B_OFFSET);
            waypointsList.add(new DJIWaypoint(
                    mHomeLatitude,
                    y_pos,
                    alt
            ));
            Utils.addLineToSB(trajSB, "Waypoint", i);
            Utils.addLineToSB(trajSB, "y_offset", y_offset);
            Utils.addLineToSB(trajSB, "alt", alt);
        }

        waypointMission.addWaypoints(waypointsList);

        Utils.setResultToText(mContext, mTrajectoryInfoTV, trajSB.toString());

        return waypointMission;
    }

    private int lastTargetWPIndex = -1;
    @Override
    public void missionProgressStatus(DJIMission.DJIMissionProgressStatus progressStatus) {
        if (progressStatus == null) {
            return;
        }
        pushSB = new StringBuffer();
        if (progressStatus instanceof DJIWaypointMission.DJIWaypointMissionStatus) {

            Utils.addLineToSB(pushSB, "Target waypoint index", ((DJIWaypointMission.DJIWaypointMissionStatus) progressStatus).getTargetWaypointIndex());
            Utils.addLineToSB(pushSB, "Is waypoint reached", ((DJIWaypointMission.DJIWaypointMissionStatus) progressStatus).isWaypointReached());
            Utils.addLineToSB(pushSB, "Execute state", ((DJIWaypointMission.DJIWaypointMissionStatus) progressStatus).getExecutionState().name());
            DJIError err = progressStatus.getError();
            Utils.addLineToSB(pushSB, "Mission status", err == null ? "Normal" : err.getDescription());

            if (((DJIWaypointMission.DJIWaypointMissionStatus) progressStatus).getTargetWaypointIndex() == 2 && lastTargetWPIndex == 1) {
                DJIWaypointMission.setAutoFlightSpeed(10, new DJICommonCallbacks.DJICompletionCallback() {

                    @Override
                    public void onResult(DJIError error) {
                        Utils.setResultToToast(mContext, "Set auto flight speed: " + (error == null ? "Success" : error.getDescription()));
                    }
                });
            }
            lastTargetWPIndex = ((DJIWaypointMission.DJIWaypointMissionStatus) progressStatus).getTargetWaypointIndex();
        }
        else if (
                progressStatus instanceof DJIFollowMeMission.DJIFollowMeMissionStatus ||
                        progressStatus instanceof DJIHotPointMission.DJIHotPointMissionStatus ||
                        progressStatus instanceof DJIPanoramaMission.DJIPanoramaMissionStatus ||
                        progressStatus instanceof DJICustomMission.DJICustomMissionProgressStatus
                ) {
            Utils.addLineToSB(pushSB, null, "Received " + progressStatus.getClass().getSimpleName());
        }
        else {
            DJIError err = progressStatus.getError();
            Utils.addLineToSB(pushSB, "Previous mission result", err == null ? "Success" : err.getDescription());
        }
        Utils.setResultToText(mContext, mMissionPushInfoTV, pushSB.toString());
    }
}
