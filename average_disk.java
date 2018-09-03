// Name: Steven Patrick
// Assignment: HW 3 (ECE 572)
// Description: Blur an 8-bit grayscale image using the disk 
// 		kernel ethod h(m,n) = 1/sqrt(m^2+n^2).

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class average_disk implements PlugInFilter {
	ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		//Only use 8-bit images
		return DOES_8G;
	}

	public void run(ImageProcessor ip) {

		//Set kernel size
		int N = 5;
		int K = (N-1)/2;

		//Get the kernel
		float[][] kern = makeDiskKernel(N);

		//Get image properties
		int imgWidth = ip.getWidth();
		int imgHeight = ip.getHeight();

		//Get a copy of the image to use as a reference
		ImageProcessor copy = ip.duplicate();

		//First two for loops go through image
		for(int u = 0; u < imgWidth; u++){
			for(int v = 0; v < imgHeight; v++){
				
				//Initialize new pixel value
				float sum = 0;
				for(int m = -K; m <= K; m++){
					//Get the row pixel value where the kernel should be applied
					int uActual;

					//If the kernel is too far left, then go to the right side of the image
					if(u+m < 0){
						uActual = u+m+imgWidth;
					}

					//If the kernel is too far right, then go to the left side of the image
					else if(u+m > imgWidth){
						uActual = u+m-imgWidth;
					}

					//Else, just get the pixel at the current kernel space
					else{
						uActual = u+m;
					}
					for(int n = -K; n <= K; n++){

						//Same process as the row pixel but for the column
						int vActual;
						if(v+n < 0){
							vActual = v+n+imgHeight;
						}
						else if(v+n > imgHeight){
							vActual = v+n-imgHeight;
						}
						else{
							vActual = v+n;
						}

						//Add the product of the pixel value and the kernel value to the sum
						//This assumes the kernel is already normalized.
						sum = sum + ((float) copy.getPixel(uActual, vActual)) * kern[N/2+m][N/2+n];
				       	}
				}

				//Convert the sum to a valid pixel value
				int finalSum = (int) Math.round(sum);
				if(finalSum < 0) finalSum = 0;
				if(finalSum > 255) finalSum = 255;

				//Apply new pixel value to the image
				ip.putPixel(u,v,finalSum);
			}
		}
	}

	float[][] makeDiskKernel(int N){
		float[][] kernel = new float[N][N];
		int K = (N-1)/2;
		float sum = 0;

		//Apply h(m,n) = 1/sqrt(m^2+n^2)
		for(int m = -K; m <= K; m++){
			for(int n = -K; n <= K; n++){
				if(n == 0 && m == 0){
					kernel[N/2][N/2] = 1;
				}
				else{
					float val = 1/(float)Math.sqrt(m*m+n*n);
					kernel[N/2+m][N/2+n] = val;
					sum = sum + kernel[N/2+m][N/2+n];
				}
			}
		}

		//Normalize kernel by dividing all values by the sum of all the entries
		for(int i = 0; i < N; i++){
			for(int j = 0; j < N; j++){
				kernel[i][j] = kernel[i][j]/sum;
			}
		}
		
		return kernel;
	}

}
