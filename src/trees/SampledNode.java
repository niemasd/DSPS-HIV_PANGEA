package trees;


import individualBasedModel.Host;

import java.util.List;

public class SampledNode extends TransmissionNode {

	public SampledNode(Host h) {
		super(h);
		if(host.getVirus().getIsMigrant()==true)
			name	  = new String( host.getNameWithDeme() + "_Migrant_" + super.uid + "_sampled");	// do like this because deme of host can change later on
		else
			name	  = new String( host.getNameWithDeme() + "_" + super.uid + "_sampled");			// do like this because deme of host can change later on
	}
	
	public List<Node> getChildren() {
		return null;
	}
	
	public int getNumberOfChildren() {
		return 0;
	}
	
	public void setChildren(List<Node> children) {
		System.out.println("SampledNode.setChildren - WARNING - cannot have children");
	}
	
	public void addChild(TransmissionNode child) {
		System.out.println("SampledNode.addChild - WARNING - cannot have children");
	}
	
	
	
}
