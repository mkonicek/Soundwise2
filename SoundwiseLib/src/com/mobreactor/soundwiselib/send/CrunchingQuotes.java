package com.mobreactor.soundwiselib.send;

import java.util.Random;

/** Provides random funny quotes for the "Crunching numbers" progress bar.
 *  Quotes appear when the progress bar reaches around 60%. */
public class CrunchingQuotes {

	String[] randomQuotes = new String[] {
			//"Man, soo many numbers",
			//"Crunching chips...",
			//"All your base are belong to us."
			"Crunching numbers..."		// don't change the message
	};
	
	Random rand = new Random();
	int changeThreshold = 50;
	int quoteReturned = 0;
	
	public CrunchingQuotes()
	{
		changeThreshold = 50 + rand.nextInt(20);
	}
	
	public boolean hasNewQuote(int progress) {
		if (quoteReturned == 0 && progress > changeThreshold) {
			quoteReturned = 1;
			return true;
		}
		return false;
	}

	public String getNewQuote() {
		return randomQuotes[rand.nextInt(randomQuotes.length)];
	}

}
