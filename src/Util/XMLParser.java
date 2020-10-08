package Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class XMLParser {

			/* ---< FIELDS >--- */
	/**
	 * The root of the XML-file data tree.
	 */
	public XMLNode root;
	
	/**
	 * The contents of the input XML file.
	 */
	List<String> file;
	
	
			/* ---< NESTED >--- */
	public static class XMLNode {
		
				/* ---< FIELDS >--- */
		/**
		 * The ID of this node.
		 */
		public String ID;
		
		/**
		 * The value of this node. A node can either have a value or childs.
		 */
		public String value;
		
		/**
		 * The childs of this node. A node can either have a value or childs.
		 */
		public List<XMLNode> children = new ArrayList();
		
		
				/* ---< CONSTRUCTORS >--- */
		public XMLNode(List<String> in) {
			this.parse(in);
		}
		
		public XMLNode(String ID, String value) {
			this.ID = ID;
			this.value = value;
		}
		
		public XMLNode(String ID) {
			this.ID = ID;
		}
		
		
				/* ---< METHODS >--- */
		/**
		 * Creates recursiveley the data tree structure of the XML file.
		 */
		public void parse(List<String> in) {
			this.ID = in.get(0).substring(1, in.get(0).length() - 1);
			in = in.subList(1, in.size() - 1);
			
			String open = null;
			List<String> sub = new ArrayList();
			int openCnt = 0;
			for (String s : in) {
				s = s.trim();
				if (s.equals(""))continue;
				
				if (open == null) {
					if (s.contains("/")) {
						String [] sp = s.split("[>\\<]");
						this.children.add(new XMLNode(sp [1], sp [2]));
						continue;
					}
					else {
						open = s.substring(1, s.length() - 1);
						openCnt++;
					}
				}
				else if (s.equals("<" + open + ">"))openCnt++;
				
				sub.add(s);
				
				if (s.equals("</" + open + ">")) {
					openCnt--;
					if (openCnt == 0) {
						this.children.add(new XMLNode(sub));
						open = null;
						sub.clear();
					}
				}
				
			}
		}
		
		/**
		 * Returns the node with given ID.
		 */
		public XMLNode getNode(String ID) {
			return this.getNode(ID, this);
		}
		
		private XMLNode getNode(String ID, XMLNode n) {
			if (n.ID.equals(ID))return n;
			else {
				for (XMLNode n0 : n.children) {
					XMLNode n1 = this.getNode(ID, n0);
					if (n1 != null)return n1;
				}
				return null;
			}
		}
		
		/**
		 * Returns the value of the node with given ID.
		 */
		public String getValue(String ID) {
			if (this.getNode(ID) == null)return null;
			else return this.getNode(ID).value;
		}
		
		/**
		 * Prints the tree structure starting from given node recursiveley.
		 * @param n The node to start from.
		 */
		public void printNode() {
			this.printNode(this, "");
		}
		
		private void printNode(XMLNode n, String off) {
			if (n.value != null)return;
			else for (XMLNode n0 : n.children)this.printNode(n0, off + "  ");
		}
		
		/**
		 * Creates a XML Representation of the current data tree.
		 */
		public List<String> writeXML() {
			return this.writeXML(new ArrayList(), this, "");
		}
		
		private List<String> writeXML(List<String> out, XMLNode n, String off) {
			if (n.value != null) {
				out.add(off + "<" + n.ID + ">" + n.value + "</" + n.ID + ">");
				return out;
			}
			else {
				out.add(off + "<" + n.ID + ">");
				for (XMLNode n0 : n.children)out = this.writeXML(out, n0, off + "    ");
				out.add(off + "</" + n.ID + ">");
				return out;
			}
		}
	}
	
	
			/* ---< CONSTRUCTORS >--- */
	public XMLParser(File f) {
		this.file = Util.readFile(f);
		this.root = new XMLNode(this.file);
	};
	
} 
