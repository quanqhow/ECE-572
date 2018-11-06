// Name: Steven Patrick
// Assignment: HW 11 (ECE 572)
// Description: Implement a notch filter to get rid of sinusoidal noise in bike image

import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

/*************************************************************************
 * Butterworth filter: Ideal smoothing filter which is maximally flat in 
 * the passband region. Input and output are complex Fourier transforms.
 *************************************************************************/

public class fit_bwnotch implements ExtendedPlugInFilter, DialogListener {

	private int flags = DOES_32 + DOES_STACKS;
	private PlugInFilterRunner pfr = null;

	private int filterOrder = 1;
	private float usub0 = 0.5f;
	private float vsub0 = 0.5f;
	private float W = 0.5f;

	private int Nrow = 0;
	private int Ncol = 0;

	@Override
	public int setup(String arg, ImagePlus imp) {
		if (imp.getStackSize() != 2) {
			IJ.error("Complex image required");
			return DONE;
		}

		return flags;
	}

	@Override
	public void run(ImageProcessor ip) {

		Nrow = ip.getHeight();
		Ncol = ip.getWidth();

		int N = (int)Math.max(Nrow, Ncol)/2;

		for (int v=Nrow/2; v<Nrow; v++) {
			IJ.showProgress(v, 2*Nrow);

			int v0 = v;
			int v1 = Nrow - v;

			for (int u=Ncol/2; u<Ncol; u++) {
				int u0 = u;
				int u1 = Ncol-u;

				float unotch = (float)(u-Ncol/2)/N;
				float vnotch = (float)(v-Nrow/2)/N;

				float Huv = Notch(unotch,vnotch);

				ip.setf(u0, v0, Huv*ip.getf(u0, v0));
				ip.setf(u0, v1, Huv*ip.getf(u0, v1));
				ip.setf(u1, v0, Huv*ip.getf(u1, v0));
				ip.setf(u1, v1, Huv*ip.getf(u1, v1));
			}
		}
	}

	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		this.pfr = pfr;

		GenericDialog gd = new GenericDialog("FFT Filtering");

		gd.addNumericField("Filter order", filterOrder, 0);
		gd.addNumericField("U_0", usub0, 2);
		gd.addNumericField("V_0", vsub0, 2);
		gd.addNumericField("Width", W, 0);

		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.showDialog();

		if (gd.wasCanceled())
			return DONE;

		return flags;
	}

	// Called after each modification to the dialog. 
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		filterOrder = (int)gd.getNextNumber();
		usub0 = (float)gd.getNextNumber();
		vsub0 = (float)gd.getNextNumber();
		W = (float)gd.getNextNumber();

		if (gd.wasCanceled())
			return false;

		if (filterOrder <= 0 || W < 0.0f || W > 1.0f || usub0 > 1.0f || usub0 < 0.0f || vsub0 > 1.0f || vsub0 < 0.0f)
			return false;

		return true;
	}

	@Override
	public void setNPasses(int nPasses) {
		;
	}

	float Notch(float u, float v){
		float diffu = u-usub0;
		float diffv = v-vsub0;
		float sumu =  u+usub0;
		float sumv =  v+vsub0;

		float D1 = (float)Math.sqrt(diffu*diffu+diffv*diffv);
		float D2 = (float)Math.sqrt(sumu*sumu+sumv*sumv);
		
		return (float)(1.0/Math.sqrt(1.0+Math.pow(W*W/(D1*D2),filterOrder)));
	}
}
