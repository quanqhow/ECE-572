// Name: Steven Patrick
// Assignment: HW2 (ECE 572) 
// Description: Equalize the histogram of an 8-bit image

// Import Libraries
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class histeq_  implements PlugInFilter {
	ImagePlus imp;

	//Setup so it only works for 8-bit images
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G;
	}

	//Implementation
	public void run(ImageProcessor ip) {
		
		// Get Image stats
		int w = ip.getWidth();
		int h = ip.getHeight();
		int K = 256;
		int[] H = ip.getHistogram();
		
		// Sum of the square roots of all the previous bins
		H[0] = (int)(Math.sqrt(H[0]));
		for(int j = 1; j < H.length; j++){
			H[j] = (int)Math.sqrt(H[j])+H[j-1];
		}

		// Normalize lookup table
		for(int j = 0; j < H.length; j++){
			H[j] = (K-1)*H[j] / H[K-1];
		}

		//Apply lookup table
		ip.applyTable(H);
	}
}
