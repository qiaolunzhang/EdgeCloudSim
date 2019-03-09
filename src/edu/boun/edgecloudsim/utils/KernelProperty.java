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
    private int applicationType;
    private int pesNumber;
    private int mobileDeviceId;
    private int kernelId;
    
    public KernelProperty(double _startTime, int _mobileDeviceId, int _taskType, int _pesNumber, long _length, long _inputFileSize, long _outputFileSize) {
    	startTime=_startTime;
    	mobileDeviceId=_mobileDeviceId;
    	applicationType=_taskType;
    	pesNumber = _pesNumber;
    	length = _length;
    	outputFileSize = _inputFileSize;
       	inputFileSize = _outputFileSize;
	}
    
    public KernelProperty(int _mobileDeviceId, int _applicationType, double _startTime, ExponentialDistribution[][] expRngList, int _kernelId) {
    	mobileDeviceId=_mobileDeviceId;
    	startTime=_startTime;
    	applicationType=_applicationType;
    	
    	inputFileSize = (long)expRngList[_applicationType][0].sample();
    	outputFileSize =(long)expRngList[_applicationType][1].sample();
    	length = (long)expRngList[_applicationType][2].sample();
    	
    	pesNumber = (int)SimSettings.getInstance().getApplicationLookUpTable()[_applicationType][8];
    	kernelId = _kernelId;
	}
    
    public KernelProperty(int _mobileDeviceId, int _kernelType, int _applicationType, double _startTime, ExponentialDistribution[][] expRngList, int _taskPropertyId) {
    	mobileDeviceId=_mobileDeviceId;
    	startTime=_startTime;
    	applicationType=_applicationType;
    	
    	inputFileSize = (long)expRngList[_kernelType][0].sample();
    	outputFileSize =(long)expRngList[_kernelType][1].sample();
    	length = (long)expRngList[_kernelType][2].sample();
    	
    	pesNumber = (int)SimSettings.getInstance().getKernelLookUpTable()[_kernelType][8];
    	kernelId = _taskPropertyId;
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

    public int getApplicationType(){
    	return applicationType;
    }
    
    public int getPesNumber(){
    	return pesNumber;
    }
    
    public int getMobileDeviceId(){
    	return mobileDeviceId;
    }
    
    public int getKernelId() {
    	return kernelId;
    }
}
