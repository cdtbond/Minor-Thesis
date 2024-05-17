package Algorithms;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import Calculations.TimeCalculations;
import Calculations.TravelCalculations.TravelCalc;
import Objects.AreaInspection;
import Objects.Inspection;
import Objects.Property;
import Objects.ScheduledInspection;

public class ConstructAlgorithms {

	protected ArrayList<Inspection> unscheduledInspections = new ArrayList<Inspection>();
	protected ArrayList<ScheduledInspection> possibleInspections = new ArrayList<ScheduledInspection>();
	protected HashMap<String,AreaInspection> ai = new HashMap<String,AreaInspection>();	
	protected LocalTime startingTime;
	protected Property startingLoc;
	protected TravelCalc tc;
	protected Integer inspDuration;
	
	public ConstructAlgorithms(ArrayList<Inspection> unscheduledInspections,LocalTime startingTime, Property startingLoc, TravelCalc tc,HashMap<String,AreaInspection> ai,Integer inspDuration) {
		setUnscheduledInspections(unscheduledInspections);
		this.startingTime = startingTime;
		this.startingLoc = startingLoc;
		this.tc = tc;
		this.ai = ai;
		this.inspDuration = inspDuration;
	}
	//Only used for unit testing
	public ConstructAlgorithms() {
		// TODO Auto-generated constructor stub
	}

	public LocalTime getStartTime(LocalTime time1,LocalTime time2) {
		return (time1.isAfter(time2)? time1 : time2);
	}

	public void setUnscheduledInspections(ArrayList<Inspection> unscheduledInspections) {
		this.unscheduledInspections = unscheduledInspections;
	}
	
	public ArrayList<Inspection> getUnscheduledInspections() {
		return unscheduledInspections;
	}
	
	protected ArrayList<Inspection> limitByEuclidTravelTime(int startIndex, int finishIndex) throws InspectionNotValidException {
		ArrayList<Inspection> viableInsp = new ArrayList<Inspection>();

		
		for(int i = startIndex; i <= finishIndex; i++) {
			
			Property finishProp = unscheduledInspections.get(i).getProperty();
			double travelDur = tc.niaveTravelTime(startingLoc, finishProp);
			if(validTravelTime((long) travelDur,unscheduledInspections.get(i))) {
				//System.out.println("With a starting time of " + startingTime + "a travel time of " + (long) travelDur + " mins viable inspection via euclid method" + unscheduledInspections.get(i).getId() + " will be pass through to be assessed for API inspection. It has an early start of " + unscheduledInspections.get(i).getEarlyStart() + " a late Start of " + unscheduledInspections.get(i).getLateStart());			
				viableInsp.add(unscheduledInspections.get(i));
			}
		}
		if(viableInsp.size() == 0) {
			throw new InspectionNotValidException();
		}
		
		return viableInsp;
	}
	
	protected ArrayList<ScheduledInspection> limitByAPITravelTime(ArrayList<Inspection> viableInsp) {
		String startAddress = startingLoc.getStreetAddress();
		ArrayList<ScheduledInspection> testScheduledInspections = new ArrayList<ScheduledInspection>();
		
		for (Inspection insp: viableInsp) {
			testScheduledInspections = addValidAPI(testScheduledInspections,insp,startAddress);
		}
		return testScheduledInspections;
	}
	
	
	protected ArrayList<ScheduledInspection> addValidAPI(ArrayList<ScheduledInspection> testScheduledInspections, Inspection viableInsp,String startAddress) {
		Integer travelTime = tc.apiTravelTime(startAddress, viableInsp.getProperty().getStreetAddress(), startingTime);
		
		//API api = new API(tt,startAddress,viableInsp.getProperty().getStreetAddress(),startingTime);
		//Integer travelTime = api.syncGetDuration();
		int ai = getAreaDuration(viableInsp);
		try {
		if(validTravelTime(travelTime,viableInsp)) {
			//System.out.println("With a starting time of " + startingTime + "an API travel time of " + travelTime + " viable inspection " + viableInsp.getId() + " will be added to the viable inspection list. It has an early start of " + viableInsp.getEarlyStart() + " a late Start of " + viableInsp.getLateStart());			
			TimeCalculations timeCalcs = new TimeCalculations(startingTime,travelTime,viableInsp.getLateStart(),viableInsp.getEarlyStart(),ai,inspDuration);
			testScheduledInspections.add(new ScheduledInspection(viableInsp,timeCalcs.getScheduledStart(),timeCalcs));
		} else {
			System.out.println("Viable inspection " + viableInsp.getId() + " will not be added to the API inspection list");
		}
		} catch (InspectionNotValidException e) {
			System.out.println("Viable inspection " + viableInsp.getId() + " is not valid, so will not be added to the API inspection list");			
		}
		return testScheduledInspections;
	}
	
	protected Boolean validTravelTime(long travelDur,Inspection insp) {
		return startingTime.plusMinutes(travelDur).isBefore(insp.getLateStart());
	}
	
	public ArrayList<ScheduledInspection> calculateRatios(ArrayList<ScheduledInspection> inspections) {
		
		for(ScheduledInspection insp: inspections) {
			//Double score = Math.pow(insp.getInspectionValue(), 2);
			Double score = (double) ((double) insp.getInspectionValue()/(double) insp.getTimeCalcs().getOverallDuration());
			insp.setRatio(score);
			//System.out.println("Ratio for " + insp.getId() + " is " + insp.getRatio());
		}
		return inspections;
	}
	
	private Integer getAreaDuration(Inspection insp) {
		
		AreaInspection areaInsp = ai.get(insp.getAreaId());
		if(!areaInsp.alreadySelected()) {
			return areaInsp.getDuration();
		}
		return 0;
	}
	public int getRemainingViableInspectionsNumber() {
		if(possibleInspections == null) {
			return 0;
		}
		return possibleInspections.size();
	}
}
