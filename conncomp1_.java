// Name: Steven Patrick
// HW 9 (ECE 572)
// Description: Create bounding boxes on an image


// Sidenote: beyond confused about u,v vs. x,y
// Comment: comments are only on the parts of code not taken from canvas

import ij.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.process.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Arrays;
import java.util.TreeMap;

public class conncomp1_ implements ExtendedPlugInFilter, DialogListener {

	private int flags = DOES_8G;
	private PlugInFilterRunner pfr = null;

	private int lowThreshold = 128;	// min object graylevel
	private int highThreshold = 255;	// max object graylevel
	private int minPix = 200; 		// min number of pixels to be considered an object

	private int nextLabel = 1;		// next object label
	private int Nobjects = 0;		// num objects created

	private Boxes[] lineList = null;
	private int lineWidth = 2;

	private int disjointSetParent[];	// disjoint set label
	private int disjointSetRank[];	    	// disjoint set rank

	@Override
	public int setup(String arg, ImagePlus imp) {
		return flags;
	}

	@Override
	public void run(ImageProcessor ip) {

		ImageProcessor ip_overlay = ip.duplicate();
		int Nrow = ip.getHeight();
		int Ncol = ip.getWidth();

		nextLabel = 1;
		Nobjects = 0;

		disjointSetParent = newDisjointSetArray(Nrow*Ncol, -1);
		disjointSetRank = newDisjointSetArray(Nrow*Ncol, 0);

		int[][] label = new int[Ncol][Nrow];

		for (int v=0; v<Nrow; v++) {
			IJ.showProgress(v, Nrow);
			for (int u=0; u<Ncol; u++) {
				int Iuv = ip.get(u, v);

				label[u][v] = 0; // default label: background
				if (Iuv < lowThreshold || highThreshold < Iuv)
					continue;

				int S = (u==0) ? 0 : findLabel(label[u-1][v]);
				int T = (v==0) ? 0 : findLabel(label[u][v-1]);

				if (S==0 && T==0) { 
					Nobjects++;
					label[u][v] = nextLabel++;
				} else 
					if (S!=0 && T==0) {
						label[u][v] = S;
					} else 
						if (S==0 && T!=0) {
							label[u][v] = T;
						} else {
							label[u][v] = mergeLabels(S, T);
						}
			}
		}

		TreeMap<Integer,Integer> setLabel = new TreeMap<Integer,Integer>();
		TreeMap<Integer,Boxes> bbLabel = new TreeMap<Integer, Boxes>();

		for (int v=0; v<Nrow; v++) {
			IJ.showProgress(v, Nrow);
			for (int u=0; u<Ncol; u++) {
				int Luv = findLabel(label[u][v]);
				if (setLabel.containsKey(Luv) == false){
					setLabel.put(Luv, setLabel.size());
					bbLabel.put(Luv, new Boxes(v, v+1, u, u+1));
				}

				// Check pixel value to see if it increases the bounding box size
				if (bbLabel.get(Luv).minX > v){
					bbLabel.get(Luv).setMinX(v);
				}
				else if(bbLabel.get(Luv).maxX < v){
					bbLabel.get(Luv).setMaxX(v);
				}

				if (bbLabel.get(Luv).minY > u){
					bbLabel.get(Luv).setMinY(u);
				}
				else if(bbLabel.get(Luv).maxY < u){
					bbLabel.get(Luv).setMaxY(u);
				}

				ip.set(u, v, setLabel.get(Luv));
			}
		}

		// Create the boxes by copying the map
		lineList = new Boxes[setLabel.size()];
		int lmnop = 0;
		for(Map.Entry<Integer, Boxes> entry: bbLabel.entrySet()){
			lineList[lmnop] = entry.getValue();
			lmnop = lmnop + 1;
		}

		new WaitForUserDialog("It crashes in lineOverlay");
		lineOverlay(ip_overlay);
	}

	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		this.pfr = pfr;

		GenericDialog gd = new GenericDialog("Connected Components");

		gd.addNumericField("low threshold:", lowThreshold, 0);
		gd.addNumericField("high threshold:", highThreshold, 0);
		gd.addNumericField("min size object:", minPix, 0);

		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.showDialog();

		if (gd.wasCanceled())
			return DONE;

		return flags;
	}

	// Called after each modification to the dialog. 
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		lowThreshold = (int)gd.getNextNumber();
		highThreshold = (int)gd.getNextNumber();
		minPix = (int)gd.getNextNumber();

		if (gd.wasCanceled())
			return false;

		return (0.0f <= lowThreshold && lowThreshold < highThreshold && highThreshold <= 255.0 && minPix > 0);
	}

	@Override
	public void setNPasses(int nPasses) {
		;
	}

	private int[] newDisjointSetArray(int M, int value) {
		int[] disjointSetArray = new int[M];

		for (int i=0; i<M; i++)
			disjointSetArray[i] = value;

		return disjointSetArray;
	}

	private int mergeLabels(int i1, int i2) {
		i1 = findLabel(i1);
		i2 = findLabel(i2);

		// union-by-rank merging of sets
		if (i1 != i2) {
			if (disjointSetRank[i1] > disjointSetRank[i2])
				disjointSetParent[i2] = i1;
			else
				if (disjointSetRank[i1] < disjointSetRank[i1])
					disjointSetParent[i1] = i2;
				else {
					disjointSetParent[i2] = i1;
					disjointSetRank[i1] += 1;
				}

			Nobjects -= 1;
		}

		return findLabel(i1);
	}

	private int findLabel(int i) {
		if (disjointSetParent[i] == -1)
			return i;

		// recursive path compression
		disjointSetParent[i] = findLabel(disjointSetParent[i]);
		return disjointSetParent[i];
	}

	private void lineOverlay(ImageProcessor ip) {
		ColorProcessor ip_overlay = ip.convertToColorProcessor();

		ip_overlay.setColor(Color.yellow);

		for (int i=1; i<lineList.length; i++) {
			if (lineList[i].count == -1)
				break;
			lineList[i].draw(ip_overlay, lineWidth);
		}

		ImagePlus imp_overlay = new ImagePlus("Bounding Boxes", ip_overlay);

		imp_overlay.show();

		if (IJ.showMessageWithCancel("Bounding Boxes", "Keep overlay?") == false)
			imp_overlay.close();
	} 

	public class Boxes implements Comparable<Boxes> {
		public int minX = 0;
		public int maxX = 0;
		public int minY = 0;
		public int maxY = 0;

		private int count = 0;

		// Constructors
		private Boxes() {
			this(0, 10, 0, 10);
		}

		public Boxes(int minX, int maxX, int minY, int maxY) {
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
			this.count = 10;
		}

		// Functions to set values of box
		public void setMinX(int val){
			this.minX = val;
		}
		
		public void setMinY(int val){
			this.minY = val;
		}
		public void setMaxX(int val){
			this.maxX = val;
		}
		public void setMaxY(int val){
			this.maxY = val;
		}

		public int getCount() {
			return count;
		}

		public int compareTo (Boxes rhs){
			// greater-than order
			if (count < rhs.count)
				return +1;
			else
				if (count > rhs.count)
					return -1;
				else
					return 0;
		}

		// Draws the box
		public void draw(ImageProcessor ip, int thickness) {
			int dmax = thickness / 2;

			// Checks if the bounding box is large enough
			int width = maxX-minX;
			int height = maxY-minY;
			if(width * height < minPix){
				return;
			}

			// Draws bottom horizontal line?
			for (int v = minY-dmax; v < maxY+dmax; v++) {
				for (int u = minX-dmax; u < minX+dmax; u++) {
					ip.drawPixel(v, u);
				}
			}

			// Draws top horizontal line?
			for (int v = minY-dmax; v < maxY+dmax; v++) {
				for (int u = maxX-dmax; u < maxX+dmax; u++) {
					ip.drawPixel(v, u);
				}
			}

			// Draws left vertical line?
			for (int v = minY-dmax; v < minY+dmax; v++) {
				for (int u = minX-dmax; u < maxX+dmax; u++) {
					ip.drawPixel(v, u);
				}
			}

			// Draws right vertical line?
			for (int v = maxY-dmax; v < maxY+dmax; v++) {
				for (int u = minX-dmax; u < maxX+dmax; u++) {
					ip.drawPixel(v, u);
				}
			}
		}
	}
}
