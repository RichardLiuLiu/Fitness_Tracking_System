package demo.task;

import demo.model.*;
import demo.service.PositionService;
import demo.support.NavUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocationSimulator implements Runnable {

    @Getter
    @Setter
    private long id;

    @Setter
    private PositionService positionService;

    private AtomicBoolean cancel = new AtomicBoolean();

    private double speedInMps;
    private boolean shouldMove;
    private boolean exportPositionsToMessaging = true;
    private Integer reportInterval = 500;

    @Getter
    @Setter
    private PositionInfo positionInfo;

    @Setter
    private List<Leg> legs;
    private RunnerStatus runnerStatus = RunnerStatus.NONE;
    private String runningId;

    private Integer secondsToError = 45;

    @Setter
    private Point startPoint;
    private Date executionStartTime;

    private MedicalInfo medicalInfo;

    public LocationSimulator(GpsSimulatorRequest gpsSimulatorRequest) {
        this.shouldMove = gpsSimulatorRequest.isMove();
        this.exportPositionsToMessaging = gpsSimulatorRequest.isExportPositionsToMessaging();
        this.setSpeed(gpsSimulatorRequest.getSpeed());
        this.reportInterval = gpsSimulatorRequest.getReportInterval();

        this.runningId = gpsSimulatorRequest.getRunningId();
        this.runnerStatus = gpsSimulatorRequest.getRunnerStatus();
        this.medicalInfo = gpsSimulatorRequest.getMedicalInfo();
    }

    @Override
    public void run() {
        try {
            executionStartTime = new Date();
            if (cancel.get()) {
                destroy();
                return;
            }
            while (!Thread.interrupted()) {
                long startTime = new Date().getTime();
                if (positionInfo != null) {
                    if (shouldMove) {
                        moveRunningLocation();
                        positionInfo.setSpeed(speedInMps);
                    } else {
                        positionInfo.setSpeed(0.0);
                    }

                    if (this.secondsToError > 0 && startTime - executionStartTime
                            .getTime() >= this.secondsToError * 1000) {
                        this.runnerStatus = RunnerStatus.SUPPLY_NOW;
                    }

                    positionInfo.setRunnerStatus(this.runnerStatus);

                    final MedicalInfo medicalInfoToUse;

                    switch (this.runnerStatus) {
                        case SUPPLY_SOON:
                        case SUPPLY_NOW:
                        case STOP_NOW:
                            medicalInfoToUse = this.medicalInfo;
                            break;
                        default:
                            medicalInfoToUse = null;
                            break;
                    }

                    final CurrentPosition currentPosition = new CurrentPosition(
                            positionInfo.getRunningId(),
                            new Point(positionInfo.getPosition().getLatitude(),
                                      positionInfo.getPosition().getLongitude()),
                                      positionInfo.getRunnerStatus(),
                                      positionInfo.getSpeed(),
                                      positionInfo.getLeg().getDirection(),
                                      medicalInfoToUse
                    );
                    positionService.processPositionInfo(id, currentPosition, this.exportPositionsToMessaging);
                }

                sleep(startTime);
            }
        } catch (InterruptedException ie) {
            destroy();
            return;
        }
        destroy();
    }

    private void destroy() {
        positionInfo = null;
    }

    private void sleep(long startTime) throws InterruptedException {
        long endTime = new Date().getTime();
        long elapsedTime = endTime - startTime;
        long sleepTime = reportInterval - elapsedTime > 0 ? reportInterval - elapsedTime : 0;
        Thread.sleep(sleepTime);
    }

    private void moveRunningLocation() {
        double distance = speedInMps * reportInterval / 1000.0;
        double distanceFromStart = positionInfo.getDistanceFromStart() + distance;
        double excess = 0.0; // amount by which next position will exceed end

        for (int i = positionInfo.getLeg().getId(); i < legs.size(); i++) {
            Leg currentLeg = legs.get(i);
            excess = distanceFromStart > currentLeg.getLength() ? distanceFromStart - currentLeg.getLength() : 0.0;

            if (Double.doubleToRawLongBits(excess) == 0) {
                // this means new position falls within current leg
                positionInfo.setDistanceFromStart(distanceFromStart);
                positionInfo.setLeg(currentLeg);
                Point newPosition = NavUtils.getPosition(currentLeg.getStartPosition(), distanceFromStart, currentLeg.getDirection());
                positionInfo.setPosition(newPosition);
                return;
            }
            distanceFromStart = excess;
        }
        setStartPosition();
    }

    public void setSpeed(double speed) {
        this.speedInMps = speed;
    }

    public double getSpeed(){
        return this.speedInMps;
    }

    public void setStartPosition() {
        positionInfo = new PositionInfo();
        positionInfo.setRunningId(this.runningId);
        Leg leg = legs.get(0);
        positionInfo.setLeg(leg);
        positionInfo.setPosition(leg.getStartPosition());
        positionInfo.setDistanceFromStart(0.0);
    }

    public synchronized void cancel(){
        this.cancel.set(true);
    }
}
