package trees;

import java.util.*;

/**
 * class to prune a transmission tree to only the sampled nodes
 * @author Samantha Lycett
 * @created 1 July 2013
 * @version 1 July 2013
 * @version 2 July 2013
 * @author Emma Hodcroft
 * @version 1 Nov 2013 - Added params/functions to create Nexus files (based off the Newick functions). Call TransmissionTree.toNexus
 */
public class TreePruner {

	boolean				pruned 				= false;
	TransmissionTree 	tt;
	String				fullNewick			= null;
	String				prunedNewick		= null;
	String				binaryPrunedNewick 	= null;
	
	String				fullNexus			= null;
	String				prunedNexus			= null;
	String				binaryPrunedNexus 	= null;
	
	public TreePruner(TransmissionTree tt) {
		this.tt = tt;
	}
	
	//////////////////////////////////////////////////////////////
	
	public void prune() {
		
		if (!pruned) {
			// record full newick of unpruned tree
			fullNewick 		= tt.toNewick();
		
			// remove the unsampled tips, note this changes the transmission tree forever
			tt.removeUnsampledTips();
			prunedNewick 	= tt.toNewick();
		
			// remove the one child nodes, note this changes the transmission tree forever
			tt.removeOneChildNodes();		// internals
			tt.removeOneChildNodes();		// connected to sampled
			binaryPrunedNewick = tt.toNewick();
			
			pruned 			= true;
		}
	}
	
	/**
	 * Emma Hodcroft - 1 Nov 2013
	 */
	public void pruneNexus() {
		
		if (!pruned) {
			// record full nexus of unpruned tree
			fullNexus 		= tt.toNexus();
		
			// remove the unsampled tips, note this changes the transmission tree forever
			tt.removeUnsampledTips();
			prunedNexus 	= tt.toNexus();
		
			// remove the one child nodes, note this changes the transmission tree forever
			tt.removeOneChildNodes();		// internals
			tt.removeOneChildNodes();		// connected to sampled
			binaryPrunedNexus = tt.toNexus();
			
			pruned 			= true;
		}
	}
	
	/**
	 * returns full unpruned newick string of the transmission tree
	 * @return
	 */
	public String getFullNewick() {
		if (fullNewick == null) {
			prune();
		}
		return fullNewick;
	}
	
	/**
	 * returns newick with sampled tips only, but note that there are 1 child nodes (OK for FigTree not OK for R-ape).
	 * @return
	 */
	public String getPrunedNewick() {
		if (prunedNewick == null) {
			prune();
		}
		return prunedNewick;
	}
	
	/**
	 * returns binary tree with sampled tips only.
	 * @return
	 */
	public String getBinaryPrunedNewick() {
		if (binaryPrunedNewick == null) {
			prune();
		}
		return binaryPrunedNewick;
	}
	
	
	/////////Nexus versions
	
	/**
	 * returns full unpruned nexus string of the transmission tree
	 * Emma Hodcroft - 1 Nov 2013
	 * @return
	 */
	public String getFullNexus() {
		if (fullNexus == null) {
			pruneNexus();
		}
		return fullNexus;
	}
	
	/**
	 * returns nexus with sampled tips only, but note that there are 1 child nodes (OK for FigTree not OK for R-ape).
	 * Emma Hodcroft - 1 Nov 2013
	 * @return
	 */
	public String getPrunedNexus() {
		if (prunedNexus == null) {
			pruneNexus();
		}
		return prunedNexus;
	}
	
	/**
	 * returns binary nexus tree with sampled tips only.
	 * Emma Hodcroft - 1 Nov 2013
	 * @return
	 */
	public String getBinaryPrunedNexus() {
		if (binaryPrunedNexus == null) {
			pruneNexus();
		}
		return binaryPrunedNexus;
	}
	
}
