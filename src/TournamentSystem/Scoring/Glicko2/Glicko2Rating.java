/*
 * Copyright (C) 2013 Jeremy Gooch <http://www.linkedin.com/in/jeremygooch/>
 *
 * The licence covering the contents of this file is described in the file LICENCE.txt,
 * which should have been included as part of the distribution containing this file.
 */
package TournamentSystem.Scoring.Glicko2;

import java.io.Serializable;

/**
 * Holds an individual's Glicko-2 rating.
 *
 * <p>Glicko-2 ratings are an average skill value, a standard deviation and a volatility (how consistent the player is).
 * Prof Glickman's paper on the algorithm allows scaling of these values to be more directly comparable with existing rating
 * systems such as Elo or USCF's derivation thereof. This implementation outputs ratings at this larger scale.</p>
 *
 * @author Jeremy Gooch
 */
public class Glicko2Rating implements Serializable {
	/**
	 * change the version ID for serialization only if a newer version is no longer
	 * compatible with an older one (older .tsr.zip will become unreadable or you have
	 * to provide a special version transformation)
	 */
	private static final long serialVersionUID = 1L;

	private String uid; // not actually used by the calculation engine but useful to track whose rating is whose
	private double rating;
	private double ratingDeviation;
	private double volatility;
	private int numberOfResults = 0; // the number of results from which the rating has been calculated

	 // the following variables are used to hold values temporarily whilst running calculations
	private double workingRating;
	private double workingRatingDeviation;
	private double workingVolatility;
	
	/**
	 * 
	 * @param uid           An value through which you want to identify the rating (not actually used by the algorithm)
	 * @param ratingSystem  An instance of the Glicko2RatingCalculator object
	 */
	public Glicko2Rating(String uid, Glicko2RatingCalculator ratingSystem) {
		this.uid = uid;
		this.rating = ratingSystem.getDefaultRating();
		this.ratingDeviation = ratingSystem.getDefaultRatingDeviation();
		this.volatility = ratingSystem.getDefaultVolatility();
	}

	public Glicko2Rating(String uid, Glicko2RatingCalculator ratingSystem, double initRating, double initRatingDeviation, double initVolatility) {
		this.uid = uid;
		this.rating = initRating;
		this.ratingDeviation = initRatingDeviation;
		this.volatility = initVolatility;
	}

	/**
	 * Return the average skill value of the player.
	 * 
	 * @return double
	 */
	public double getRating() {
		return this.rating;
	}

	public void setRating(double rating) {
		this.rating = rating;
	}

	/**
	 * Return the average skill value of the player scaled down
	 * to the scale used by the algorithm's internal workings.
	 * 
	 * @return double
	 */
	public double getGlicko2Rating() {
		return Glicko2RatingCalculator.convertRatingToGlicko2Scale(this.rating);
	}

	/**
	 * Set the average skill value, taking in a value in Glicko2 scale.
	 * 
	 * @param rating as double
	 */
	public void setGlicko2Rating(double rating) {
		this.rating = Glicko2RatingCalculator.convertRatingToOriginalGlickoScale(rating);
	}

	public double getVolatility() {
		return volatility;
	}

	public void setVolatility(double volatility) {
		this.volatility = volatility;
	}

	public double getRatingDeviation() {
		return ratingDeviation;
	}

	public void setRatingDeviation(double ratingDeviation) {
		this.ratingDeviation = ratingDeviation;
	}

	/**
	 * Return the rating deviation of the player scaled down
	 * to the scale used by the algorithm's internal workings.
	 * 
	 * @return double
	 */
	public double getGlicko2RatingDeviation() {
		return Glicko2RatingCalculator.convertRatingDeviationToGlicko2Scale( ratingDeviation );
	}

	/**
	 * Set the rating deviation, taking in a value in Glicko2 scale.
	 * 
	 * @param ratingDeviation as double
	 */
	public void setGlicko2RatingDeviation(double ratingDeviation) {
		this.ratingDeviation = Glicko2RatingCalculator.convertRatingDeviationToOriginalGlickoScale( ratingDeviation );
	}

	/**
	 * Used by the calculation engine, to move interim calculations into their "proper" places.
	 * 
	 */
	public void finaliseRating() {
		this.setGlicko2Rating(workingRating);
		this.setGlicko2RatingDeviation(workingRatingDeviation);
		this.setVolatility(workingVolatility);
		
		this.setWorkingRatingDeviation(0);
		this.setWorkingRating(0);
		this.setWorkingVolatility(0);
	}
	
	/**
	 * Returns a formatted rating for inspection
	 * 
	 * @return {ratingUid} / {ratingDeviation} / {volatility} / {numberOfResults}
	 */
	@Override
	public String toString() {
		return uid + " / " +
				rating + " / " +
				ratingDeviation + " / " +
				volatility + " / " +
				numberOfResults;
	}
	
	public int getNumberOfResults() {
		return numberOfResults;
	}

	public void incrementNumberOfResults(int increment) {
		this.numberOfResults = numberOfResults + increment;
	}

	public String getUid() {
		return uid;
	}

	public void setWorkingVolatility(double workingVolatility) {
		this.workingVolatility = workingVolatility;
	}

	public void setWorkingRating(double workingRating) {
		this.workingRating = workingRating;
	}

	public void setWorkingRatingDeviation(double workingRatingDeviation) {
		this.workingRatingDeviation = workingRatingDeviation;
	}
}
