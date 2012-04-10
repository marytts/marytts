/**
 * Copyright 2002 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.language.sv;

import java.util.*;

/**
 * Class used by sv_CompoundSplitter to store intermediary results.
 * 
 * @author Erik Sterneberg
 *
 */
public class SplitItem {
	protected String string;
	protected Integer start;
	protected ArrayList<String[]> parts;
	
	public SplitItem(String s, Integer start){
		this.string = s;
		this.start = start;
		this.parts = new ArrayList<String[]>();
	}
	
	public SplitItem(String s, Integer start, ArrayList<String[]> parts){
		this.string = s;
		this.start = start;
		this.parts = parts;
	}	
		
	public SplitItem(SplitItem oldItem, String[] newPart){
		// Making a new item out of an old one, the new string should be as much shorter as the newPart[1] is long
		
		//System.out.println("<-------- New Item --------->");
		this.string = oldItem.string.substring(newPart[1].length());
		//System.out.println("string: " + this.string);
		this.start = oldItem.start + newPart[1].length();
		//System.out.println("start: " + this.start);
		this.parts = new ArrayList<String[]>(oldItem.parts);
		//System.out.println("new part: " + newPart[1]);
		this.parts.add(newPart);
		//System.out.println("Nbr of parts: " + this.parts.size());
		/*
		System.out.println("+++++++++++++++++++++++++++++++++++++++++++");
		for (int i=0; i<this.parts.size(); i++){			
			System.out.print(this.parts.get(i)[1] + " ");
		}
		System.out.println();
		System.out.println("<---------------------------->");
		*/
	}
	
	public void printItem(){
		for (int i=0; i<this.parts.size(); i++){			
			System.out.print(this.parts.get(i)[1] + " ");
		}
		System.out.println();
	}
}
