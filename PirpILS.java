package Algorithms;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

import Calculations.TimeCalculations;
import Calculations.Sort.ValueSort;
import Calculations.TravelCalculations.TravelCalc;
import Objects.AreaInspection;
import Objects.Inspection;
import Objects.Location;
import Objects.Property;
import Objects.ScheduledInspection;
import Objects.TravelType;

public class PirpILS extends PirpAlgorithms {
	
	private ArrayList<Inspection> unscheduledInspectionSortedByValue = new ArrayList<Inspection>();
	private ArrayList<ScheduledInspection> testImproveInspections = new ArrayList<ScheduledInspection>(); //use to manipulate and store the intermediate value
	
	
	//private Double threshold = 0.15;
	//private Integer thresholdNumber = thresholdInspections();
	
	public PirpILS(HashMap<String, Inspection> inspections, ArrayList<Inspection> unscheduledInspections, HashMap<String,AreaInspection> areaInspections, LocalDate inspectionDate, Integer areaInspDuration,TravelType tt, Location loc) {
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
			BuildILS ils = new BuildILS(unscheduledInspections, start, prop,tc,tt,areaInspections,unscheduledInspections.get(0).getDuration());
			
			ils.calculateViable();

			ScheduledInspection rouletteInsp = ils.getRouletteWheelInspection();
			makeInspectionScheduled(rouletteInsp);
			start = rouletteInsp.getScheduledFinish();
			prop = rouletteInsp.getProperty();
			areaInspections.get(rouletteInsp.getAreaId()).setSelected(true);

		}
		} catch(InspectionNotValidException e) {

		}
		
		ValueSort qs = new ValueSort();
		unscheduledInspectionSortedByValue = qs.sort(unscheduledInspections);
	}
	
	
	@Override
	public void present(String filename,ArrayList<String> prevDetails) {
		WriteToFile write = new WriteToFile(filename.replace(".csv", "OutputPirpILS.txt") );
		String s = "";
		for(String string : prevDetails) {
			s += string;
		}
		write.writeToFile(s);
	}


	@Override
	public String intermediateValues(String s) {
		for(int i = 0; i < scheduledInspectionsSortedByOrder.size(); i++) {
			s += scheduledInspectionsSortedByOrder.get(i).toString() + "\n";
		}
		return s;
	}
	
	
	// PIRP-ILS	
	// Step 2 - Greedy Build
	// keep inserting properties until one will not go into the property		
	

	
	// Step 3 - Improvement
	public void improve() {
		long startTime = System.nanoTime();
		long currentTime = System.nanoTime();
		long endTime = (long) 60 * 1000000000;
		
		while((currentTime - startTime) < endTime) {
			scheduledCopiedIntoTest();
			System.out.println("Starting improve with scheduled inspection size of " + testImproveInspections.size());
			
			perturbationPIRP();
			try {
				swapPIRP();
				twoOptPIRP();
			} catch (InspectionNotValidException e) {
				System.out.println("Swap/2-Opt Failed");
			}
			improveInsertPIRP();
			improveReplacePIRP();
			
			if(getSummaryScores(testImproveInspections) > getSummaryScores(scheduledInspectionsSortedByOrder)) {
				testCopiedIntoScheduled();
			} 
			currentTime = System.nanoTime();
			System.out.println("Current time is " + (currentTime - startTime) / 1000000000);
			
		}
		
	}
	
	private void testCopiedIntoScheduled() {
		scheduledInspectionsSortedByOrder.clear();
		for(ScheduledInspection insp : testImproveInspections) {
			scheduledInspectionsSortedByOrder.add(insp);
		}
	}
	
	private void scheduledCopiedIntoTest() {
		testImproveInspections.clear();
		for(ScheduledInspection insp : scheduledInspectionsSortedByOrder) {
			testImproveInspections.add(insp);
		}
	}
	

	
	
	private int getSummaryScores(ArrayList<ScheduledInspection> insps) {
		int score = 0;
		for(ScheduledInspection insp : insps) {
			score += insp.getInspectionValue();
		}
		return score;
		
	}
	
	public void improveInsertPIRP() {
		// look at top 30% of the unscheduled inspections
		int noOfInspections = unscheduledInspectionSortedByValue.size();
		noOfInspections *= 0.3;
		
		for (int i = 0 ; i < noOfInspections; i++) {
			//check whether this is better than a neighbourhood of inspections
			// note that removing an areaInspection is not an option
			try {
				insertPIRP(unscheduledInspectionSortedByValue.get(i),tc);
			} catch (InspectionNotValidException e) {
				// TODO Auto-generated catch block
				//System.out.println(unscheduledInspectionSortedByValue.get(i).getId() + " skipped.");
				}
		}
	}
	
	public void improveReplacePIRP() {
		// look at top 30% of the unscheduled inspections
		int noOfInspections = unscheduledInspectionSortedByValue.size();
		noOfInspections *= 0.3;
		
		for (int i = 0 ; i < noOfInspections; i++) {
			//check whether this is better than a neighbourhood of inspections
			// note that removing an areaInspection is not an option
			try {
				replace(unscheduledInspectionSortedByValue.get(i),tc);
			} catch (InspectionNotValidException e) {
				// TODO Auto-generated catch block
				//System.out.println(unscheduledInspectionSortedByValue.get(i).getId() + " skipped.");
				}
		}
	}

	
	
	private void insertPIRP(Inspection inspection, TravelCalc tc) throws InspectionNotValidException {
		//Search search = new Search();
		//int i = search.indexOfLastOccurance(scheduledInspectionsSortedByOrder, inspection.getLateStart());
		
		for(int i = 0; i<testImproveInspections.size();i++) {
			
			Boolean startsEarlyEnough = testImproveInspections.get(i).getScheduledFinish().isBefore(inspection.getLateStart());
			Boolean finishesEarlyEnough = false;
			if(testImproveInspections.size()-1 > i+1) {
				finishesEarlyEnough	= testImproveInspections.get(i+1).getScheduledStart().isAfter(inspection.getLateStart());
			}
			// if both tests are passed, then proceed
			if(startsEarlyEnough && finishesEarlyEnough) {
				insertAfter(i,testImproveInspections,inspection, tc);
				break;
			}
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
	
	
	
	private void replace(Inspection insp, TravelCalc tc) throws InspectionNotValidException {
		ILSReplace ilsReplace = new ILSReplace(testImproveInspections,tc,areaInspections);
		testImproveInspections = ilsReplace.loopList(insp);
	}

	//randomly remove either 1 or a string of inspections to make space for new inspections
	private void perturbationPIRP() {
		int upperLimit = testImproveInspections.size();
		upperLimit -= 1;
		
		System.out.println("Lower bound is 1, upper bound is " + upperLimit);
		int randStart = ThreadLocalRandom.current().nextInt(1, upperLimit);
		//get the remaining inspections until the end
		int randEnd = upperLimit - randStart;
		// get a random number from 1 to end
		randEnd = ThreadLocalRandom.current().nextInt(0, randEnd);
		//get upper bound
		randEnd = randStart + randEnd;
		
		for(int i = randEnd-1; i > randStart; i--) {
			testImproveInspections.remove(i);
		}
	}
	
	//randomly swap any two scheduled inspections
	private void swapPIRP() throws InspectionNotValidException {
		
		int upperLimit = testImproveInspections.size();
		upperLimit -= 1;

		System.out.println("Lower bound is 1, upper bound is " + upperLimit);
		
		if(upperLimit == 1) {
			return;
		}		
		int randFirst = ThreadLocalRandom.current().nextInt(1, upperLimit);
		//get the remaining inspections until the end, avoid start and finish inspections
		int randSecond = ThreadLocalRandom.current().nextInt(1, upperLimit);
		
		if( randFirst == randSecond) {
			return;
		}
		ILSReplace ilsR = new ILSReplace(testImproveInspections,tc,areaInspections);
		
		ScheduledInspection firstPrev = testImproveInspections.get(randFirst-1);
		ScheduledInspection firstInsp = testImproveInspections.get(randFirst);
		ScheduledInspection firstNext = testImproveInspections.get(randFirst+1);
		ScheduledInspection secondPrev = testImproveInspections.get(randSecond-1);
		ScheduledInspection secondInsp = testImproveInspections.get(randSecond);
		ScheduledInspection secondNext = testImproveInspections.get(randSecond+1);
		
		//check that the two nominated nodes can swap
		if(ilsR.timeValid(secondInsp,firstPrev,firstNext) && ilsR.timeValid(firstInsp,secondPrev,secondNext)) {
			testImproveInspections = ilsR.replaceSpecificNode(randFirst,secondInsp);
			testImproveInspections = ilsR.replaceSpecificNode(randSecond, firstInsp);
		}
		
	}
	
	//swap two scheduled inspections that are in sequence
	private void twoOptPIRP() throws InspectionNotValidException {
		
		int randFirst = ThreadLocalRandom.current().nextInt(0, testImproveInspections.size());
		//get the remaining inspections until the end, avoid start and finish inspections
		int randSecond = randFirst + 1;
		
		if( randSecond +1  >= testImproveInspections.size() || randFirst -1 <= 0 ) {
			return;
		}
		ILSReplace ilsR = new ILSReplace(testImproveInspections,tc,areaInspections);
		
		ScheduledInspection firstInsp = testImproveInspections.get(randFirst);
		ScheduledInspection secondInsp = testImproveInspections.get(randSecond);
		
		
		ScheduledInspection firstPrev = secondInsp;
		ScheduledInspection firstNext = testImproveInspections.get(randSecond+1);
		ScheduledInspection secondPrev = testImproveInspections.get(randFirst-1);
		ScheduledInspection secondNext = firstPrev;

		
		
		//check that the two nominated nodes can swap
		if(ilsR.timeValid(secondInsp,firstPrev,firstNext) && ilsR.timeValid(firstInsp,secondPrev,secondNext)) {
			testImproveInspections = ilsR.replaceSpecificNode(randFirst,secondInsp);
			testImproveInspections = ilsR.replaceSpecificNode(randSecond, firstInsp);
		}
		
	}
	
	
}