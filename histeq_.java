import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class histeq_  implements PlugInFilter {
	ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G;
	}

	public void run(ImageProcessor ip) {
		int w = ip.getWidth();
		int h = ip.getHeight();
		int M = w*h;
		int K = 256;

		int[] H = ip.getHistogram();
		//H[0] = (int)(Math.sqrt(H[0]));
		for(int j = 0; j < H.length; j++){
			H[j] = (int)(Math.sqrt(H[j]));
		}
		for(int j = 1; j < H.length; j++){
			H[j] = H[j]+H[j-1];
		}
		for(int j = 0; j < H.length; j++){
			H[j] = (K-1)*H[j] / H[K-1];
		}
		ip.applyTable(H);

	}

}
