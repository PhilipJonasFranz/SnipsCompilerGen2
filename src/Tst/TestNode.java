package Tst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import Util.Pair;
import Util.Logging.Message;

public class TestNode {
		
	public TestNode parent;
	
	public String testPackage;
	
	public HashMap<String, TestNode> childPackages = new HashMap();
	
	public List<TestNode> childs = new ArrayList();
	
	public List<Pair<String, List<Message>>> tests = new ArrayList();
	
	public TestNode(List<String> files) {
		this.testPackage = "";
		
		List<String> cut = files.stream().map(x -> x.substring(testPackage.length())).collect(Collectors.toList());
		
		for (String file : cut) {
			this.addTest(file);
		}
	}
	
	public TestNode(TestNode parent, String testPackage) {
		this.parent = parent;
		this.testPackage = testPackage;
	}
	
	public String getPackagePath() {
		if (this.parent == null) return "";
		else return this.parent.getPackagePath() + this.testPackage + "/";
	}
	
	public void addTest(String file) {
		if (!file.contains("\\")) {
			this.tests.add(new Pair<String, List<Message>>(file, new ArrayList()));
		}
		else {
			String subPackage = "";
			while (!file.startsWith("\\")) {
				subPackage += "" + file.charAt(0);
				file = file.substring(1);
			}
			file = file.substring(1);
			
			if (!this.childPackages.containsKey(subPackage)) {
				TestNode node = new TestNode(this, subPackage);
				this.childPackages.put(subPackage, node);
				this.childs.add(node);
			}
			
			if (this.childPackages.containsKey(subPackage)) {
				this.childPackages.get(subPackage).addTest(file);
			}
		}
	}
	
	public void print(int d) {
		String s = "";
		for (int i = 0; i < d; i++) s += " ";
		System.out.println(s + this.testPackage);
		for (Pair<String, List<Message>> tests : this.tests) {
			System.out.println(s + "    " + "Test: " + tests.getFirst());
		}
		
		for (Entry<String, TestNode> entry : this.childPackages.entrySet()) {
			entry.getValue().print(d + 4);
		}
	}
	
} 
