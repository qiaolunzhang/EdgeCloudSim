/*
 * Title:        EdgeCloudSim - EdgeTask
 * 
 * Description: 
 * A custom class used in Load Generator Model to store tasks information
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.utils;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import edu.boun.edgecloudsim.core.SimSettings;

public class KernelProperty {
    private double startTime;
    private long length, inputFileSize, outputFileSize;
    private int taskType;
    private int pesNumber;
    private int mobileDeviceId;
    private boolean submitted;
    private int taskPropertyId;
    
    public KernelProperty(double _startTime, int _mobileDeviceId, int _taskType, int _pesNumber, long _length, long _inputFileSize, long _outputFileSize) {
    	startTime=_startTime;
    	mobileDeviceId=_mobileDeviceId;
    	taskType=_taskType;
    	pesNumber = _pesNumber;
    	length = _length;
    	outputFileSize = _inputFileSize;
       	inputFileSize = _outputFileSize;
       	submitted = false;
	}
    
    public KernelProperty(int _mobileDeviceId, int _taskType, double _startTime, ExponentialDistribution[][] expRngList, int _taskPropertyId) {
    	mobileDeviceId=_mobileDeviceId;
    	startTime=_startTime;
    	taskType=_taskType;
    	
    	inputFileSize = (long)expRngList[_taskType][0].sample();
    	outputFileSize =(long)expRngList[_taskType][1].sample();
    	length = (long)expRngList[_taskType][2].sample();
    	
    	pesNumber = (int)SimSettings.getInstance().getApplicationLookUpTable()[_taskType][8];
    	taskPropertyId = _taskPropertyId;
    	submitted = false;
	}
    
    public KernelProperty(int _mobileDeviceId, int _subTaskType, int _taskType, double _startTime, ExponentialDistribution[][] expRngList, int _taskPropertyId) {
    	mobileDeviceId=_mobileDeviceId;
    	startTime=_startTime;
    	taskType=_taskType;
    	
    	inputFileSize = (long)expRngList[_subTaskType][0].sample();
    	outputFileSize =(long)expRngList[_subTaskType][1].sample();
    	length = (long)expRngList[_subTaskType][2].sample();
    	
    	pesNumber = (int)SimSettings.getInstance().getKernelLookUpTable()[_subTaskType][8];
    	taskPropertyId = _taskPropertyId;
    	submitted = false;
    }
	
    public void setSubmitted() {
    	submitted = true;
    }
    
    public double getStartTime(){
    	return startTime;
    }
    
    public long getLength(){
    	return length;
    }
    
    public long getInputFileSize(){
    	return inputFileSize;
    }
    
    public long getOutputFileSize(){
    	return outputFileSize;
    }

    public int getTaskType(){
    	return taskType;
    }
    
    public int getPesNumber(){
    	return pesNumber;
    }
    
    public int getMobileDeviceId(){
    	return mobileDeviceId;
    }
    
    public int getTaskPropertyId() {
    	return taskPropertyId;
    }
}