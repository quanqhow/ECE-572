// Name: Steven Patrick
// Assignment: Test 1 #8 (ECE 572)
// Description: Make a GUI that implements a type of alpha trimmed mean filter

import ij.plugin.filter.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class Test_1 implements ExtendedPlugInFilter, DialogListener {
	
	//Only do 8-bit gray-scale images
	private int flags = DOES_8G;
	private PlugInFilterRunner pfr = null;

	//Set default parameters
	private float alpha = 0.25f;
	private int N = 5;

	@Override
	public int setup(String arg, ImagePlus imp){
		return flags;
	}

	@Override
	public void run(ImageProcessor ip) {

		// Get variables for finding the mean
		int K = (N-1)/2;
	        int[] kernel = new int[N*N];

        	for(int i = 0; i < kernel.length; i++)
			kernel[i] = 0;

		ImageProcessor ip_copy = ip.duplicate();

    		int h = ip.getHeight();
    		int w = ip.getWidth();

		// The first two for loops go through the image
    		for (int v=0; v<h; v++) {
      			IJ.showProgress((double)v/(double)h);
			for (int u=0; u<w; u++) {
                		int M = 0;

				// These two for loops go through the neighborhood of the kernel
                		for (int m=-K; m<=K; m++) {
                  			for (int n=-K; n<=K; n++) {

						// if statement that is equivalent to zero padding
                        			if (0 <= v-m && v-m < h && 0 <= u-n && u-n < w){
                          				kernel[M++] = ip_copy.get(u-n,v-m);
						}
                  			}
                		}

				// Create a new kernel so old values do not effect the mean when sorting
				int[] newKernel = new int[M];
				for(int i = 0; i < M; i++){
					newKernel[i] = kernel[i];
				}
                		Arrays.sort(newKernel);

				// Find the uppper and lower limit with the given alpha value
                		int lowerLimit = (int)(alpha*(float)M);
				int upperLimit = M-2*lowerLimit;
				if(upperLimit <= lowerLimit){
					upperLimit = lowerLimit+1;
				}

				// Find the weighted average
                		int sum = 0;
                		for (int i=lowerLimit; i<upperLimit; i++){
                  			sum += newKernel[i];
				}
				
                		int Ipuv = sum/(upperLimit-lowerLimit);

				// Assign the new value
                		ip.set(u, v, Ipuv);
          		}
    		}
  	}

		
	//Dialog box for tuneable parameters
	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		this.pfr = pfr;

		//Create Dialog Box
		GenericDialog gd = new GenericDialog("Alpha-Mean");
		gd.addNumericField("alpha:", alpha,2);
		gd.addNumericField("M    :", N,0);
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.showDialog();

		if(gd.wasCanceled())
			return DONE;

		return flags;
	}	

	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e){
		//Extract Values
		alpha = (float)gd.getNextNumber();
		N = (int)gd.getNextNumber();
		
		if(gd.wasCanceled())
			return false;

		//Check that the parameters are valid
		return (0.0f <= alpha) && (.5f >= alpha) && (0 < N) && (N % 2 == 1);
	}

	@Override
	public void setNPasses(int nPasses){
		;
	}
}
