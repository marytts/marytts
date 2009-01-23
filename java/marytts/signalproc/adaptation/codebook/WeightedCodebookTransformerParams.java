/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.signalproc.adaptation.codebook;

import marytts.signalproc.adaptation.BaselineTransformerParams;

/**
 * Parameters of weighted codebook based transformation
 *
 * @author Oytun T&uumlrk
 */
public class WeightedCodebookTransformerParams extends BaselineTransformerParams {

    public String codebookFile; //Codebook file
    
    public WeightedCodebookMapperParams mapperParams;
    
    public boolean isContextBasedPreselection;
    public int totalContextNeighbours;
    
    public WeightedCodebookTransformerParams()
    {
        super();
        
        codebookFile = "";
            
        mapperParams = new WeightedCodebookMapperParams();
        
        isContextBasedPreselection = false;
        totalContextNeighbours = 0;
    }
    
    public WeightedCodebookTransformerParams(WeightedCodebookTransformerParams existing)
    {
        super((BaselineTransformerParams)existing);
        
        codebookFile = existing.codebookFile;
            
        mapperParams = new WeightedCodebookMapperParams(existing.mapperParams);
        
        isContextBasedPreselection = existing.isContextBasedPreselection;
        totalContextNeighbours = existing.totalContextNeighbours;
    }
}


