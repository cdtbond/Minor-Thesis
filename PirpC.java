package Algorithms;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import Calculations.Search;
import Calculations.TimeCalculations;
import Calculations.Sort.ValueSort;
import Calculations.TravelCalculations.TravelCalc;
import Objects.AreaInspection;
import Objects.Inspection;
import Objects.Location;
import Objects.Property;
import Objects.ScheduledInspection;
import Objects.TravelType;

public class PirpC extends PirpAlgorithms {
	private ArrayList<Inspection> unscheduledInspectionSortedByValue = new ArrayList<Inspection>();
	
	
	//private Double threshold = 0.15;
	//private Integer thresholdNumber = thresholdInspections();
	
	public PirpC(HashMap<String, Inspection> inspections, ArrayList<Inspection> unscheduledInspections, HashMap<String,AreaInspection> areaInspections, LocalDate inspectionDate, Integer areaInspDuration,TravelType tt, Location loc) {
		super(inspections,unscheduledInspections,areaInspections,inspectionDate,areaInspDuration,tt,loc);
	}


	@Override
	public void construct() {

		//currentInspection
		Property prop = getStartFinishLocation();
		LocalTime start = getDayStart();
		// Construct stage of the algorithm
		
		//Starting at position 1, using the StartFinish location
		//Boolean viableInspectionsBool = true;
		try {
		while(true) {
			
			
			// Set the unscheduled inspections for 
			Competitors compete = new Competitors(unscheduledInspections, start, prop,tc,tt,areaInspections,unscheduledInspections.get(0).getDuration());
			
			compete.calculateViable();
			//This will terminate the loop if no more inspections can be added after the current inspection

			ScheduledInspection bestInsp = compete.getBestInspection();
			makeInspectionScheduled(bestInsp);
			start = bestInsp.getScheduledFinish();
			prop = bestInsp.getProperty();
			areaInspections.get(bestInsp.getAreaId()).setSelected(true);
		}
		} catch(InspectionNotValidException e) {
			//System.out.println("No more valid inspections remain.");
		}
		//Select window of next options
			//Within an hour
			//10 or less
		//Conduct Duration Calculations
			//Reduce using euclidean distance-time
			//Check the list is viable
		
		//Add finish location
		//lastInspection();
		ValueSort qs = new ValueSort();
		unscheduledInspectionSortedByValue = qs.sort(unscheduledInspections);
		//sortScheduledInspectionByValue();
		//for (ScheduledInspection insp :	scheduledInspectionsSortedByOrder) {
		//	System.out.println("Inspection: " + insp.getId() +" Ratio: "+ insp.getRatio() +" Start:  " + insp.getScheduledStart() + " Finish: "+ insp.getScheduledFinish());
		//}
	}
	
	
	private ScheduledInspection lastInspection() {
		int size = scheduledInspectionsSortedByOrder.size();
		if(size == 0) {
			return null;
		}
		return scheduledInspectionsSortedByOrder.get(size-1);
	}

	
	

	
	public void improveInsertC() {
		// look at top 30% of the unscheduled inspections
		int noOfInspections = unscheduledInspectionSortedByValue.size();
		noOfInspections *= 0.3;
		
		for (int i = 0 ; i < noOfInspections; i++) {
			//check whether this is better than a neighbourhood of inspections
			// note that removing an areaInspection is not an option
			try {
				insertC(unscheduledInspectionSortedByValue.get(i),tc);
			} catch (InspectionNotValidException e) {
				// TODO Auto-generated catch block
				//System.out.println(unscheduledInspectionSortedByValue.get(i).getId() + " skipped.");
				}
		}
	}
	
	public void improveNeighbourhoodReplace() {
		int noOfInspections = unscheduledInspectionSortedByValue.size();
		noOfInspections *= 0.3;
		
		for (int i = 0 ; i < noOfInspections; i++) {
			//check whether this is better than a neighbourhood of inspections
			// note that removing an areaInspection is not an option
			try {
				neighbourhoodReplace(unscheduledInspectionSortedByValue.get(i),tc);
			} catch (InspectionNotValidException e) {
				System.out.println(unscheduledInspectionSortedByValue.get(i).getId() + " has error, it is now skipped.");
			}
		}
	}
	
	
	private void insertC(Inspection inspection, TravelCalc tc) throws InspectionNotValidException {
		Search search = new Search();
		int i = search.indexOfLastOccurance(scheduledInspectionsSortedByOrder, inspection.getLateStart());
		Boolean startsEarlyEnough = scheduledInspectionsSortedByOrder.get(i).getScheduledFinish().isBefore(inspection.getLateStart());
		Boolean finishesEarlyEnough = false;
		if(scheduledInspectionsSortedByOrder.size()-1 > i+1) {
			finishesEarlyEnough	= scheduledInspectionsSortedByOrder.get(i+1).getScheduledStart().isAfter(inspection.getLateStart());
		}
		// if both tests are passed, then proceed
		if(startsEarlyEnough && finishesEarlyEnough) {
			insertAfter(i,scheduledInspectionsSortedByOrder,inspection, tc);
		}
		

	}


	private void insertAfter(int i, ArrayList<ScheduledInspection> scheduledInspectionsSortedByOrder, Inspection inspection, TravelCalc tc) throws InspectionNotValidException {
		
		double tt = tc.apiTravelTime(scheduledInspectionsSortedByOrder.get(i).getProperty().getStreetAddress(), inspection.getProperty().getStreetAddress(), scheduledInspectionsSortedByOrder.get(i).getScheduledFinish());
		scheduledInspectionsSortedByOrder.get(i);
		int areaDur = 0;
		if(!areaInspections.get(inspection.getAreaId()).alreadySelected()) {
			areaDur = areaInspections.get(inspection.getAreaId()).getDuration();
		} 
		TimeCalculations timeCalcs = new TimeCalculations(scheduledInspectionsSortedByOrder.get(i).getScheduledFinish(),(int) tt,inspection.getLateStart(),inspection.getEarlyStart(),areaDur,inspection.getDuration());
		ScheduledInspection scheduled = new ScheduledInspection(inspection,timeCalcs.getScheduledStart(),timeCalcs);
		
		Double score = (double) ((double) scheduled.getInspectionValue()/(double) scheduled.getTimeCalcs().getOverallDuration());
		scheduled.setRatio(score);

		ScheduledInspection next = scheduledInspectionsSortedByOrder.get(i+1);
		//Do the calculations for the next scheduled item
		tt = tc.apiTravelTime(scheduled.getProperty().getStreetAddress(), next.getProperty().getStreetAddress(), scheduled.getScheduledFinish());
		TimeCalculations nextTimeCalcs = new TimeCalculations(scheduled.getScheduledFinish(),(int) tt,next.getLateStart(),next.getEarlyStart(),areaDur,next.getDuration());

		//do the start times match? i.e. has the added inspection delayed the finish?
		Boolean timesMatch = next.getScheduledStart().equals(scheduledInspectionsSortedByOrder.get(i).getScheduledStart());
		
		if(timesMatch) {
			next = new ScheduledInspection(next,next.getScheduledStart(),nextTimeCalcs);
			makeInspectionScheduled(scheduled,next,i);
			System.out.println(scheduled.getId() + " inserted before " + next.getId());
		} 
		//otherwise do not add to the inspections
		
	}


	private void neighbourhoodReplace(Inspection insp, TravelCalc tc) throws InspectionNotValidException {
		//calculate the slot that this inspection could fit in
		Search search = new Search();
		int i = search.indexOfLastOccurance(scheduledInspectionsSortedByOrder, insp.getLateStart());
		// i must be min of 1 and max of (.size() - 3) or else it will exceed the bounds of the array
		if(i<1 || i > scheduledInspectionsSortedByOrder.size()-3) {
			//Skip this one
		}
		else {
			try {
			BuildNeighbourhood bn = new BuildNeighbourhood(i,scheduledInspectionsSortedByOrder,getDayStart(),getDayFinish(),startFinishLocation,tc,areaInspections);
			ArrayList<ScheduledInspection> tempSched = new ArrayList<ScheduledInspection>();
			tempSched.addAll(scheduledInspectionsSortedByOrder);
			if(bn.step1Valid(insp)) {
				//Skip prevPrevInsp
				tempSched.remove(i);
				tempSched.remove(i);
				tempSched.remove(i);
				tempSched.remove(i);
				
				tempSched.addAll(i, bn.returnStep1Result(insp));
				System.out.println("New Step 1 Neighbourhood starting at"+ tempSched.get(i).getId());				
			} else if (bn.step1Valid(insp)) {
				tempSched.remove(i);
				tempSched.remove(i);
				tempSched.remove(i);
				tempSched.remove(i);
				
				tempSched.addAll(i, bn.returnStep2Result(insp));
				System.out.println("New Step 2 Neighbourhood starting at"+ tempSched.get(i).getId());				
			} else if (bn.step3Valid(insp)) {
				tempSched.remove(i);
				tempSched.remove(i);
				tempSched.remove(i);
				tempSched.remove(i);
				
				tempSched.addAll(i, bn.returnStep3Result(insp));
				System.out.println("New Step 3 Neighbourhood starting at"+ tempSched.get(i).getId());				
			} else if (bn.step4Valid(insp)) {
				tempSched.remove(i);
				tempSched.remove(i);
				tempSched.remove(i);
				tempSched.remove(i);
				
				tempSched.addAll(i, bn.returnStep4Result(insp));
				
				System.out.println("New Step 4 Neighbourhood starting at"+ tempSched.get(i).getId());
			} else {
				return;
			}
				scheduledInspectionsSortedByOrder = tempSched;
			} catch(InspectionNotValidException e) {
				//Skip if invalid
				System.out.println("Neighbourhood inspection not valid");
			}
			
		}
		
	}

	@Override
	public void present(String filename,ArrayList<String> prevDetails) {
		WriteToFile write = new WriteToFile(filename.replace(".csv", "OutputPirpC.txt") );
		String s = "";
		for(String string : prevDetails) {
			s += string;
		}
		//for(int i = 0; i < scheduledInspectionsSortedByOrder.size(); i++) {
		//	s += scheduledInspectionsSortedByOrder.get(i).toString() + "\n";
		//}
		write.writeToFile(s);
	}


	@Override
	public String intermediateValues(String s) {
		for(int i = 0; i < scheduledInspectionsSortedByOrder.size(); i++) {
			s += scheduledInspectionsSortedByOrder.get(i).toString() + "\n";
		}
		return s;
	}
	
	
	
	
	
	
	
//	// PIRP-C
//	// Step 2 - Greedy Build
//	public void run() {	
//		
//		//Potentially change 15 to a percentage of the total number of inspections with a floor 
//		
//		ArrayList<Inspection> nextProperties = new ArrayList<Inspection>();
//		//Or there are no nodes left with an early finish time before cutoff (automatically remove these from the list)
//		
//		// Not started 
//		//if(scheduledInspectionsSortedByOrder.size()==0) {
//			// Then use the start time of all inspections to navigate
//		nextProperties = getNextProperties(new ScheduledInspection(getDayStart(),getStartFinishLocation()),thresholdNumber);
//		//} 
//		
//		//get the last added Inspection
//		ScheduledInspection lastScheduledInspection = getLastScheduledInspection();
//		//Start loop here until no more scheduled inspections can be added
//		//Either no nodes left to add
//		
//		//ArrayList<ScheduledInspection> schedInspections = testScheduleInspection(lastScheduledInspection,nextProperties);
//		
//		//While there are scheduled inspections remaining
//		//while(schedInspections.size()>0) {
//			
//			//stop if zero
//			//if(nextProperties.size() == 0) {
//			//	break;
//			//}
//			
//			//Also stop loop if there are no valid inspections in the "next 15 inspections"
//
//			//Or there are no nodes left that can be added validly (maybe an earlier inspection means the late finish is later than cutoff and TT makes this the only valid time)
//			
//			
//			//Insert the highest value of the remaining properties
//			
//			
//			
//			// use the finish time of inspection to kick off nextFifteen comparison
//		//	nextProperties = getNextProperties(lastScheduledInspection,thresholdNumber);
//		//	schedInspections = testScheduleInspection(lastScheduledInspection,nextProperties);
//		//}
//		
//		
//		
//			
//
//
//		
//		
//		// Look at next 15 late start properties (without break)
//		
//		
//		
//		// keep inserting valid properties until one will not go into the property		
//		
//		
//		// Are there no nodes left?
//		// if yes exit
//
//		// Step 3 - Improvement
//
//		
//	}
//	
//	
//
//	
//	//The number of inspections (based on a percentage of total inspections)
//	private Integer thresholdInspections() {
//		return (int) Math.round(inspections.size()*threshold);
//	}
//	
//	//This is the starting point for the next
//	private ScheduledInspection getLastScheduledInspection() {
//		return scheduledInspectionsSortedByOrder.get(scheduledInspectionsSortedByOrder.size()-1);
//	}
//	
//
//	 
//	 
//	 //These are competing properties
//	private ArrayList<Inspection> getNextProperties(ScheduledInspection prevInspection, Integer threshold) {
//		int firstIndex = Search.indexOfFirstOccurance(unscheduledInspections,prevInspection,travelTimeCalc);
//		// Find first instance of late start that is after the specified time
//		
//		//Check if there is a gap between the first available inspection 
//		//and subsequent ones and whether this allows both to be attended
//		Inspection secondInspection = unscheduledInspections.get(firstIndex);		
//		
//		// Ensure we don't overrun the end of the array or get negative value in short array
//		Integer lastIndex = Math.min(firstIndex + (threshold-1),Math.max(unscheduledInspections.size()-threshold,0));
//		
//		ArrayList<Inspection> subsequentInspections = new ArrayList<Inspection>(unscheduledInspections.subList(firstIndex, lastIndex));
//		//timeAfterTravel is 
//		LocalTime secondInspectionStart = defineScheduledStart(prevInspection,secondInspection);
//		LocalTime secondInspectionFinish = secondInspectionStart.plusMinutes(secondInspection.getDuration());
//		
//		// use this because we don't want to officially set the Area Inspection
//		AreaInspection areaInsp = areaInspections.get(secondInspection.getAreaId());
//		
//		if(!areaInsp.alreadySelected()) {
//			secondInspectionFinish.plusMinutes(areaInsp.getDuration());
//		} 
//		//This is now the start time for each inspection
//		ArrayList<Inspection> removeInsp = new ArrayList<Inspection>();
//		//Loop through every subsequent inspection (apart from the first) to check whether they have a gap, and in this case remove from the list.
//		for(int i = 1; i<subsequentInspections.size();i++) {
//			if(!isValid(secondInspectionFinish,subsequentInspections.get(i))) {
//				//If it isn't valid then remove from the list
//				removeInsp.add(subsequentInspections.get(i));
//			}			
//		}
//		subsequentInspections.removeAll(removeInsp);
//		return subsequentInspections;
//	}
//		
//	//wait time incorporated here
//	private LocalTime defineScheduledStart(ScheduledInspection i1, Inspection i2) {
//		//pretend the second inspection has been selected
//		int tt = getTravelTime(i1,i2);
//		// will be 0 if not required
//		tt += i1.getAreaDuration();
//		//timeAfterTravel is 
//		LocalTime finishTime = i1.getScheduledFinish().plusMinutes(tt);
//		if(i2.getEarlyStart().isBefore(finishTime)) {
//			// Then no waiting is required
//			if(i2.getLateStart().isBefore(finishTime)) {
//				//Then invalid
//				return null;
//			} else {
//				//No wait time
//				return finishTime;
//			}
//		} else {
//			// wait time experienced
//			return i2.getEarlyStart();
//		}	
//	}
//	
//	
//	private boolean isValid(LocalTime startTime, Inspection inspection) {
//		//use the start  time to calculate whether the start time is after the late start time
//		return !startTime.isAfter(inspection.getLateStart());
//	}
//	
//	//TO DO
//	private Integer getTravelTime(Inspection inspection1, Inspection inspection2) {
//		
//		
//		return 1;
//	}



	

		
	
	
}
