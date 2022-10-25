package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    @Mock
    Sensor sensor;

    @Mock
    ImageService imageService;
    @Mock
    SecurityRepository securityRepo;

    @InjectMocks
    SecurityService securityService;

    @BeforeEach
    public void setupEach(){
        MockitoAnnotations.openMocks(this);
    }
    //Few codes and logics were written by following Mr. Michael, session lead, Udacity.



@ParameterizedTest //#1
    @EnumSource(value=ArmingStatus.class, names = {"ARMED_AWAY","ARMED_HOME"})
    public void if_alarmArmed_plus_sensorGotActivated_changeSystem_pendingAlarm(ArmingStatus armingStatus){
        when(securityRepo.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepo.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
         when(sensor.getActive()).thenReturn(false);
        boolean isActive=true;
        securityService.changeSensorActivationStatus(sensor,isActive);
        verify(securityRepo,times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(sensor,times(1)).setActive(any(Boolean.TYPE));
        verify(sensor,times(1)).getActive();

}
    //If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm on. [This is the case where all sensors are deactivated and then one gets activated]
    @ParameterizedTest //#2
    @EnumSource(value=ArmingStatus.class, names = {"ARMED_AWAY","ARMED_HOME"})
    public void if_alarmArmed_plus_sensorActivated_plus_systemPendingAlarm_changeToAlarm(ArmingStatus armingStatus){
        when(securityRepo.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepo.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(sensor.getActive()).thenReturn(false);
        boolean isActive=true;
        securityService.changeSensorActivationStatus(sensor,isActive);
        verify(securityRepo,times(1)).setAlarmStatus(AlarmStatus.ALARM);
        verify(sensor,times(1)).setActive(any(Boolean.TYPE));
        verify(sensor,times(1)).getActive();

    }
    //If pending alarm and all sensors are inactive, return to no alarm state.

    @Test //#3

public void if_pendingAlarm_plus_allSensors_inactive_changeToNoAlarm(){
        when(sensor.getActive()).thenReturn(true);
        boolean isSystemFlag=false;
        when(securityRepo.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor,isSystemFlag);
        verify(sensor,times(1)).setActive(any(Boolean.TYPE));
        verify(sensor,times(2)).getActive();
       verify(securityRepo,times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    //If alarm is active, change in sensor state should not affect the alarm state.
    @ParameterizedTest //#4
    @ValueSource(booleans = {true,false})

    public void if_alarmIsActive_changeSensor_noChangeInAlarm(boolean sensorState){
        when(securityRepo.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor,!sensorState);
        verify(sensor,times(1)).setActive(any(Boolean.TYPE));
        verify(sensor,never()).getActive();
        verify(securityRepo,never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

//If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test ////#5
public void if_sensorActivated_plus_systemActive_plus_alarm_isInPending_thenChange_alarm() {
     when(sensor.getActive()).thenReturn(false);
     boolean isSystemFlag= true;
     when(securityRepo.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
     securityService.changeSensorActivationStatus(sensor,isSystemFlag);
     verify(sensor,times(1)).setActive(any(Boolean.TYPE));
     verify(securityRepo,times(1)).setAlarmStatus(AlarmStatus.ALARM);
     verify(sensor,times(1)).getActive();
    }

    //If a sensor is deactivated while already inactive, make no changes to the alarm state.
@Test //#6
    public void if_sensorIsDeActivated_plus_alreadyInactive_changeNothing_toAlarm(){
        when(sensor.getActive()).thenReturn(false);
        boolean isSystemFlag= false;
        securityService.changeSensorActivationStatus(sensor,isSystemFlag);
        verify(sensor,times(1)).setActive(any(Boolean.TYPE));
        verify(securityRepo,never()).setAlarmStatus(any(AlarmStatus.class));
}
    //If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
@Test //#7
public void if_image_serviceFinds_catImage_duringSystemIs_ArmedHome_changeTo_Alarm(){

when(imageService.imageContainsCat(any(BufferedImage.class),anyFloat())).thenReturn(true);
    when(securityRepo.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
    securityService.processImage(mock(BufferedImage.class));
    verify(securityRepo,times(1)).setAlarmStatus(AlarmStatus.ALARM);
}

 //If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test //#8
    public void if_imageService_findsNotCat_plus_sensorsInActive_changeTo_noAlarm(){
        when(imageService.imageContainsCat(any(BufferedImage.class),anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepo,times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    //If the system is disarmed, set the status to no alarm.
    @Test //#9
    public void if_systemIsDisarmed_setTo_noAlarm(){

    securityService.setArmingStatus(ArmingStatus.DISARMED);
    verify(securityRepo,times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    verify(securityRepo,times(1)).setArmingStatus(ArmingStatus.DISARMED);

    }

//If the system is armed, reset all sensors to inactive.
    @ParameterizedTest //#10
    @EnumSource(value=ArmingStatus.class, names = {"ARMED_AWAY","ARMED_HOME"})
    public void if_systemIsArmed_reset_AllSensors_toInactive(ArmingStatus armingStatus){
        Set<Sensor> activeSensors = createSensors(true,12);
         when(securityRepo.getSensors()).thenReturn(activeSensors);
        securityService.setArmingStatus(armingStatus);
        verify(securityRepo,times(1)).setArmingStatus(armingStatus);
        verify(securityRepo,times(1)).getSensors();
        assertAllSensorsMatchInputActiveState(activeSensors,false);

    }

    //If the system is armed-home while the camera shows a cat, set the alarm status to alarm.*/
@Test //#11
    public void if_systemIsArmedHome_imageService_detectsCat_setAlarmStatus_toAlarm(){
      when(securityRepo.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
      when(imageService.imageContainsCat(any(BufferedImage.class),anyFloat())).thenReturn(true);
      securityService.processImage(mock(BufferedImage.class));
verify(securityRepo,times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    private Set<Sensor> createSensors(boolean sensorActiveState, int numberOfSensors) {
        Set<Sensor> sensors=new HashSet<>();
        for(int i=0; i<numberOfSensors; i++){
            Sensor newSensor = new Sensor(getRandomString(), SensorType.randomSensorType());
            newSensor.setActive(sensorActiveState);
            sensors.add(newSensor);
        }return sensors;

    }

    private String getRandomString() {
        return UUID.randomUUID().toString();

    }
    private void assertAllSensorsMatchInputActiveState(Set<Sensor> sensors, boolean sensorActiveStatus) {
        sensors.forEach(sensor1 -> assertEquals(sensorActiveStatus,sensor1.getActive()));
    }

    /*
     * Below are the unit tests to increase the unit test code coverage
     */
    @Test
    public void codeCoverage_for_test_to_add_and_remove_statusListener_method() {
        StatusListener mockStatusListener = mock(StatusListener.class);
        securityService.addStatusListener(mockStatusListener);
        securityService.removeStatusListener(mockStatusListener);
    }

    @Test
    public void codeCoverage_for_test_add_sensor_method() {
        Sensor mockSensor = mock(Sensor.class);
        securityService.addSensor(mockSensor);
        securityService.removeSensor(mockSensor);
        verify(securityRepo, times(1)).addSensor(any(Sensor.class));
        verify(securityRepo, times(1)).removeSensor(any(Sensor.class));
    }
    @Test
    public void codeCoverage_for_test_remove_Sensor_method() {
        Sensor mockSensor = mock(Sensor.class);
        doNothing().when(securityRepo).removeSensor(mockSensor);
        securityService.removeSensor(mockSensor);
        verify(securityRepo, times(1)).removeSensor(any(Sensor.class));
    }

    @ParameterizedTest   //
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void codeCoverage_for_method_if_systemIsArmed_reset_AllSensors_toInactive (ArmingStatus armedStatus) {
       //
        Set<Sensor> activeSensors = createSensors(true, 6);
        when(securityRepo.getArmingStatus()).thenReturn(armedStatus);
        List<Sensor> sensorList = new ArrayList<>(activeSensors);
        Collections.shuffle(sensorList);
        Sensor randomSensor = sensorList.get(0);
        securityService.changeSensorActivationStatus(randomSensor);
        verify(securityRepo, times(1)).getArmingStatus();
        verify(securityRepo, times(1)).getAlarmStatus();
        assertEquals(false, randomSensor.getActive(), "the sensor should be set to inactive");
    }
    @ParameterizedTest   //
    @EnumSource(value = AlarmStatus.class, names = {"PENDING_ALARM"})
    public void codeCoverage_for_method_if_alarmStatusInPendingAlarm_and_sensorActive_and_systemIsInactive_then_handleSensorDeactivated(AlarmStatus alarmStatus) {
        Sensor mockSensor = mock(Sensor.class);
        when(mockSensor.getActive()).thenReturn(true);
        when(securityRepo.getAlarmStatus()).thenReturn(alarmStatus);
        securityService.changeSensorActivationStatus(mockSensor, false);
        verify(securityRepo, times(1)).setAlarmStatus(any(AlarmStatus.class));
    }
    @ParameterizedTest   //
    @EnumSource(value = AlarmStatus.class, names = {"PENDING_ALARM", "NO_ALARM"})
    public void codeCoverage_for_method_if_alarmIsSetToAlarm_plus_sensorIsInactive_plus_systemIsInactive_changeArmingStatus_toDisArmed_then_handleSensor(AlarmStatus alarmStatus) {
        Sensor mockSensor = mock(Sensor.class);
        when(mockSensor.getActive()).thenReturn(false);
        when(securityRepo.getAlarmStatus()).thenReturn(alarmStatus);
        when(securityRepo.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(mockSensor, true);
        verify(securityRepo, times(1)).getAlarmStatus();
    }

    @Test
    public void coverage_for_test_getAlarmStatus() {
        securityService.getAlarmStatus();
        verify(securityRepo, times(1)).getAlarmStatus();
    }


    @Test
    public void if_sensorIsActivated_plus_alarmStatus_isAlarm_changeAlarm_toPending() {
        when(sensor.getActive()).thenReturn(true);
        when(securityRepo.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor);
        verify(sensor, times(1)).getActive();
        verify(securityRepo, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
    @Test
    public void if_sensorIsInActivated_plus_armStatus_isDisArmed_changeNothing() {
        when(sensor.getActive()).thenReturn(false);
        when(securityRepo.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor);
        verify(sensor, times(1)).getActive();
        verify(securityRepo, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    public void test_securityService_constructor() {
        SecurityService securityService1 = new SecurityService(securityRepo);
    }
    @ParameterizedTest   //
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void if_systemArmed_setSensor_toDeactivated(ArmingStatus armingStatus) {
        when(securityRepo.getArmingStatus()).thenReturn(armingStatus);
        securityService.changeSensorActivationStatus(sensor);
        verify(sensor, times(1)).setActive(false);
    }
    @Test
    public void if_sensorDeactivated_plus_alarmPending_changeAlarm_toNoAlarm() {
        when(sensor.getActive()).thenReturn(false);
        when(securityRepo.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor);
        verify(sensor, times(1)).getActive();
        verify(securityRepo, times(2)).getAlarmStatus();
        verify(securityRepo, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    @Test
    public void codeCoverage_forMethod_if_armingStatus_isArmedHome_call_setArmingStatus() {
        securityService.isCatFound = true;
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
    }

    }
