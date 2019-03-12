/**
Test Utils ImageCompare
Copyright (C) 2019

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
**/

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class ImageCompare {

	/**
	 * Simple image compare on coordinates.
	 * 
	 * If exact match, then return true.
	 * In any other case, then return false.
	 * 
	 * Size not equal --> False
	 * One pixel deviation --> False
	 * 
	 * If equal size and all pixels equal --> True
	 * 
	 * @param expectedImage -- BufferedImage
	 * @param actualImage -- BufferedImage
	 * @return boolean
	 */
	public static boolean imagesAreExactMatches(BufferedImage expectedImage, BufferedImage actualImage) {
		int width = expectedImage.getWidth();
		int height = expectedImage.getHeight();

		if(width != actualImage.getWidth() || height != actualImage.getHeight()) {
			return false;
		} else {
			for(int y=0; y<height; y++) {
				for(int x=0; x<width; x++) {
					if(expectedImage.getRGB(x, y) != actualImage.getRGB(x, y)) {
						return false;
					}
				}
			}
		}
		return true;
	}


	/**
	 * <h3>Simple image compare on pixel matching.</h3>
	 * 
	 * Credits to Stackoverflow contributors (I did not list them all) and geeksforgeeks!
	 * @see <a href="https://www.geeksforgeeks.org/image-processing-java-set-14-comparison-two-images/">https://www.geeksforgeeks.org/image-processing-java-set-14-comparison-two-images/</a>
	 *
	 * Returns java.lang.double match (1 = perfect; 0 = entirely different)
	 * 
	 * The result returned is 1 minues the difference calculated taking into account the fullRgb and isDistance parameters.
	 * 
	 * <h3>fullRgb:</h3>
	 * <ul>
	 *     <li>True: Look at Red, green and blue separately</li>
	 *     <li>False: Take the RGB dot as one</li>
	 * </ul>
	 * 
	 * <h3>isDistance</h3>
	 * <ul>
	 *     <li>True: If not equal, <i>add absolute value of difference</i> between values to pixel difference. End result is divided by 255 (2<sup>8</sup> - 1)</li>
	 *     <li>False: If Not equal, <i>count pixel</i> as different</li>
	 * </ul>
	 * 
	 * @param expectedImage -- BufferedImage
	 * @param actualImage -- BufferedImage
	 * @param fullRgb -- boolean
	 * @param isDistance -- boolean
	 * 
	 * @return double
	 */
	public static double imagesPixelIdenticalRatio(BufferedImage expectedImage,
			BufferedImage actualImage,
			fullRgb,
			isDistance) {
		/*
		 * Initialize variables.
		 * Set width and height,
		 * Initialize total count and different counts to zero.
		 */
		int width = expectedImage.getWidth();
		int height = expectedImage.getHeight();

		double theTotal = 0;
		double theDifferentOnes = 0;

		/*
		 * If the sizes ar enot equal, then return 0.
		 * Now both for full rgb and dot per dot.
		 * TODO -- Add the height / width different in pixels to the diff pixels.
		 */
		if(width != actualImage.getWidth() || height != actualImage.getHeight()) {
			return 0;
			/*
			 * Else iterate over the co-ordinates.
			 * If full RGB, then fail if rgb not equal.
			 * If not full RGB, judge on a per color basis.
			 * Because this takes into account more nuance,
			 * it is more tolerant.
			 * TODO account for alpha in addition to red, green and blue
			 */
		} else {
			for(int y=0; y<height; y++) {
				for(int x=0; x < width; x++) {

					//Get RGB
					int rgbExpected = expectedImage.getRGB(x, y);
					int rgbActual = actualImage.getRGB(x, y);

					if(fullRgb) {
						//Get per color value by bit-shifting
						int redExpected = (rgbExpected >> 16) & 0xff;
						int greenExpected = (rgbExpected >> 8) & 0xff;
						int blueExpected = (rgbExpected) & 0xff;

						int redActual = (rgbActual >> 16) & 0xff;
						int greenActual = (rgbActual >> 8) & 0xff;
						int blueActual = (rgbActual) & 0xff;

						if(isDistance) {
							//Add difference
							theDifferentOnes += Math.abs(redExpected - redActual);
							theDifferentOnes += Math.abs(greenExpected - greenActual);
							theDifferentOnes += Math.abs(blueExpected - blueActual);
						} else {
							//Add counter if different
							if(Math.abs(redExpected - redActual) > 0) {
								theDifferentOnes += 1;
							}
							if(Math.abs(greenExpected - greenActual) > 0) {
								theDifferentOnes += 1;
							}
							if(Math.abs(blueExpected - blueActual) > 0) {
								theDifferentOnes += 1;
							}
						}
					} else {
						if(isDistance) {
							theDifferentOnes += Math.abs(rgbExpected - rgbActual);
						} else {
							if(expectedImage.getRGB(x, y) != actualImage.getRGB(x, y)) {
								theDifferentOnes += 1;
							}
						}
					}
				}
			}

			System.out.println("The diff count we have now is: " + theDifferentOnes.toString());

			/*
			 * Calculate total.
			 * If full rgb, then surface times three (3 colors)
			 * If not full rgb, then just surface
			 */
			if(fullRgb) {
				theTotal = width * height * 3;
			} else {
				theTotal = width * height;
			}

			System.out.println("The total is " + theTotal.toString());

			double avg_diff_pixels;

			avg_diff_pixels = (theDifferentOnes / theTotal);

			System.out.println("The avg diff pixels equals " + avg_diff_pixels.toString());
			System.out.println("The difference count equals " + theDifferentOnes.toString());
			System.out.println("The the total equals " + theTotal.toString());

			double percDiff;
			percDiff = 0.0;

			if(fullRgb) {
				if(isDistance) {
					//(2^8)-1 -- 8bit int
					percDiff = avg_diff_pixels / (255);
				} else {
					percDiff = avg_diff_pixels;
				}
			} else {
				if(isDistance) {
					//(2^24)-1 -- 24bit int
					percDiff = avg_diff_pixels / (16777215)
				} else {
					percDiff = avg_diff_pixels;
				}
			}

			return (1.0 - percDiff);
		}
		/*If for some reason everything goes to ****,
		 * then return 0.
		 */
		return 0;
	}
}
