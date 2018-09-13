// Name: Steven Patrick
// Assignment: HW 5 (ECE 572)
// Description: Apply unsharp masking with different kernels (Gaussian, Mexican Hat, Laplacian).
// 		Also, make the filter have changeable run-time parameters using a GUI.

import ij.plugin.filter.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class unsharp_masking implements ExtendedPlugInFilter, DialogListener {
	
	//Only do 8-bit gray-scale images and convert the image to a float
	private int flags = DOES_8G + CONVERT_TO_FLOAT;
	private PlugInFilterRunner pfr = null;

	//Set default parameters
	private int kernelType = 0;
	private float lambda = 0.25f;
	private float sigma = (float)Math.sqrt(2.0);

	@Override
	public int setup(String arg, ImagePlus imp){
		return flags;
	}

	@Override
	public void run(ImageProcessor ip) {

		//Get Kernel Based on parameters
		float[] kernel = new float[3*3];
		switch(kernelType){
			case 0: kernel = laplacianKernel(); break;
			case 1: kernel = mexicanhatKernel(3,sigma); break;
			case 2: kernel = gaussianKernel(3,sigma); break;
			default: kernel = gaussianKernel(3,sigma); break;
		}

		ImageProcessor I0 = ip.duplicate();
		ImageProcessor I1 = ip.duplicate();
		
		// Convolve on one of the two duplicates
		Convolver cv = new Convolver();

		cv.setNormalize(false);
		cv.convolve(I1, kernel, 3, 3);

		// If Gaussian, then add this term
		if(kernelType == 2){
			I0.multiply(1+lambda);
		}

		// Multiply the convolved image by tuneable parameter lambda
		I1.multiply(lambda);

		// Get the new image
		I0.copyBits(I1, 0, 0, Blitter.SUBTRACT);
		ip.insert(I0.convertToByte(false), 0, 0);
	}

	//Dialog box for tuneable parameters
	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		this.pfr = pfr;

		// Radio Button options
		java.lang.String[] itms = new java.lang.String[3];
		itms[0] = "Laplacian"; itms[1] = "Mexican Hat"; itms[2] = "Gaussian";
		
		//Create Dialog Box
		GenericDialog gd = new GenericDialog("Unsharp Masking");
		gd.addRadioButtonGroup("Kernels",itms,3,1,itms[0]);
		gd.addNumericField("lambda:", lambda,2);
		gd.addNumericField("sigma:", sigma,2);
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.showDialog();

		if(gd.wasCanceled())
			return DONE;

		return flags;
	}	

	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e){
		//Extract Values
		String kT = (String)gd.getNextRadioButton();
		lambda = (float)gd.getNextNumber();
		sigma = (float)gd.getNextNumber();
		
		//Assign appropriate kernelType
		if(kT == "Laplacian") kernelType = 0;
		else if(kT == "MH") kernelType = 1;
		else kernelType = 2;

		if(gd.wasCanceled())
			return false;

		//Check that the parameters are valid
		return (0.0f < lambda) && (0.0f < sigma);
	}


	//Not quite sure what this is for
	@Override
	public void setNPasses(int nPasses) {
		;
	}

	//Kernel Functions
	float[] mexicanhatKernel(int N, float sigma){
		float[] kernel = new float[N*N];

		float s2 = sigma*sigma;
		float s4 = s2*s2;
		float sPI = 1.0f/(float)(2.0*Math.PI*s4);

		int K = (N-1)/2;

		for (int m=-K; m<=K; m++){
			float m2 = (float)(m*m);
			for(int n=-K; n<=K; n++){
				float n2 = (float)(n*n);

				kernel[(K+m)*N+(K+n)] = sPI*((m2+n2)/s2 - 2.0f)*(float)Math.exp(-0.5*(m2+n2)/s2);
			}
		}

		return kernel;
	}

	float[] laplacianKernel() {
		float[] kernel = {
			0.0f,  1.0f, 0.0f,
			1.0f, -4.0f, 1.0f,
			0.0f,  1.0f, 0.0f};

		return kernel;
	}

	float[] gaussianKernel(int N, float sigma){
		float[] kernel = new float[N*N];

		float s2 = sigma*sigma;
		float sPI = 1.0f/(float)Math.sqrt(2.0*Math.PI*s2);

		int K = (N-1)/2;
		for(int m=-K; m<=K; m++){
			float m2 = m*m;
			for(int n=-K; n<=K; n++){
				float n2 = n*n; 
				kernel[(K+m)*N+(K+n)] = (float)Math.exp(-(m2+n2)/(2.0*s2)) / sPI;
			}
		}

		float sum = 0.0f;
		for(int i = 0; i < N*N; i++){
			sum = sum+kernel[i];
		}
		for(int i = 0; i < N*N; i++){
			kernel[i] = kernel[i]/sum;
		}

		return kernel;
	}
}
