package io;

import trees.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Samantha Lycett
 * @created sometime? 2012?
 * @author Emma Hodcroft
 * @version 1 Nov 2013 - added copies of Newick functions to print Nexus files, calls TreePruner.getPrunedNexus.. etc
 */

public class PrunedTreeWriter {

	String 	   rootname;
	String	   ext 			= ".nwk";
	
	TreePruner pruner;
	String	   fullExt		= "_full";
	String	   prunedExt	= "_pruned";
	String 	   bin_pruExt	= "_binaryPruned";
	
	boolean	   echo			= true;
	
	public PrunedTreeWriter(String rootname, TransmissionTree tt) {
		this.rootname = rootname;
		pruner 		  = new TreePruner(tt);
	}
	
	/**
	 * writes full unpruned tree to file, returns file name
	 */
	public String writeFullNewick() {
		
		try {
			String fname		   = rootname + fullExt + ext;
			BufferedWriter outFile = new BufferedWriter(new FileWriter(fname));
			String line 		   = pruner.getFullNewick();
			if (!line.endsWith(";")) {
				line = line + ";";
			}
			outFile.write(line);
			outFile.newLine();
			outFile.close();
			
			if (echo) {
				System.out.println("Full Tree to "+fname);
				System.out.println(line);
			}
			
			return fname;
			
		} catch (IOException e) {
			System.out.println(e.toString());
			
			return null;
		}
	}
	
	/**
	 * writes pruned tree to file, returns file name
	 */
	public String writePrunedNewick() {
		
		try {
			String fname		   = rootname + prunedExt + ext;
			BufferedWriter outFile = new BufferedWriter(new FileWriter(fname));
			String line 		   = pruner.getPrunedNewick();
			if (!line.endsWith(";")) {
				line = line + ";";
			}
			outFile.write(line);
			outFile.newLine();
			outFile.close();
			
			if (echo) {
				System.out.println("Pruned Tree to "+fname);
				System.out.println(line);
			}
			
			return fname;
		} catch (IOException e) {
			System.out.println(e.toString());
			
			return null;
		}
	}
	
	/**
	 * writes binary pruned tree to file, returns file name
	 */
	public String writeBinaryPrunedNewick() {
		
		try {
			String fname		   = rootname + bin_pruExt + ext;
			BufferedWriter outFile = new BufferedWriter(new FileWriter(fname));
			String line 		   = pruner.getBinaryPrunedNewick();
			if (!line.endsWith(";")) {
				line = line + ";";
			}
			outFile.write(line);
			outFile.newLine();
			outFile.close();
			
			if (echo) {
				System.out.println("Binary Pruned Tree to "+fname);
				System.out.println(line);
			}
			
			return fname;
		} catch (IOException e) {
			System.out.println(e.toString());
			
			return null;
		}
	}
	
	/**
	 * writes full unpruned nexus tree to file, returns file name
	 * Emma Hodcroft - 1 Nov 2013
	 */
	public String writeFullNexus() {
		
		try {
			String fname		   = rootname + fullExt + ".nexus";
			BufferedWriter outFile = new BufferedWriter(new FileWriter(fname));
			String line 		   = pruner.getFullNexus();
			if (!line.endsWith(";")) {
				line = line + ";";
			}
			outFile.write(line);
			outFile.newLine();
			outFile.close();
			
			if (echo) {
				System.out.println("Full Tree to "+fname);
				System.out.println(line);
			}
			
			return fname;
			
		} catch (IOException e) {
			System.out.println(e.toString());
			
			return null;
		}
	}
	
	/**
	 * writes pruned nexus tree to file, returns file name
	 * Emma Hodcroft - 1 Nov 2013
	 */
	public String writePrunedNexus() {
		
		try {
			String fname		   = rootname + prunedExt + ".nexus";
			BufferedWriter outFile = new BufferedWriter(new FileWriter(fname));
			String line 		   = pruner.getPrunedNexus();
			if (!line.endsWith(";")) {
				line = line + ";";
			}
			outFile.write(line);
			outFile.newLine();
			outFile.close();
			
			if (echo) {
				System.out.println("Pruned Tree to "+fname);
				System.out.println(line);
			}
			
			return fname;
		} catch (IOException e) {
			System.out.println(e.toString());
			
			return null;
		}
	}
	
	/**
	 * writes binary pruned nexus tree to file, returns file name
	 * Emma Hodcroft - 1 Nov 2013
	 */
	public String writeBinaryPrunedNexus() {
		
		try {
			String fname		   = rootname + bin_pruExt + ".nexus";
			BufferedWriter outFile = new BufferedWriter(new FileWriter(fname));
			String line 		   = pruner.getBinaryPrunedNexus();
			if (!line.endsWith(";")) {
				line = line + ";";
			}
			outFile.write(line);
			outFile.newLine();
			outFile.close();
			
			if (echo) {
				System.out.println("Binary Pruned Tree to "+fname);
				System.out.println(line);
			}
			
			return fname;
		} catch (IOException e) {
			System.out.println(e.toString());
			
			return null;
		}
	}
	

	/**
	 * writes full, pruned and binary pruned trees, returns file names
	 */
	public List<String> writeNewickTrees() {
		
		List<String> fnames = new ArrayList<String>();
		
		fnames.add( writeFullNewick() );
		fnames.add( writePrunedNewick() );
		fnames.add( writeBinaryPrunedNewick() );
		
		return fnames;
	}
	
	/**
	 * Emma hodcroft - 1 Nov 2013
	 * @return
	 */
	public List<String> writeNexusTrees() {
		
		List<String> fnames = new ArrayList<String>();
		
		fnames.add( writeFullNexus() );
		fnames.add( writePrunedNexus() );
		fnames.add( writeBinaryPrunedNexus() );
		
		return fnames;
	}
	
}
