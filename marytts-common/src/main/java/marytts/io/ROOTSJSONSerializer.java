package marytts.io;

import marytts.data.item.phonology.Phoneme;
import marytts.data.item.Item;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.features.FeatureMap;
import marytts.features.Feature;
import marytts.data.Utterance;
import marytts.io.MaryIOException;
import marytts.data.SupportedSequenceType;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.io.File;


import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ROOTSJSONSerializer implements Serializer
{
    private final static int SB_INIT_CAP = 1000;
    public ROOTSJSONSerializer()
    {
    }

    public Utterance load(File file)
        throws MaryIOException
    {
        throw new UnsupportedOperationException();
    }

    public void save(File file, Utterance utt)
        throws MaryIOException
    {
        throw new UnsupportedOperationException();
    }

    public String toString(Utterance utt)
        throws MaryIOException
    {
        StringBuilder sb = new StringBuilder(SB_INIT_CAP);
        sb.append("{\n");
        sb.append("\t\"sequences\": {\n");

        SupportedSequenceType cur_type = null;
        Object[] types =  utt.listAvailableSequences().toArray();
        for (int t=0; t<types.length; t++)
        {
            cur_type = ((SupportedSequenceType) types[t]);
            sb.append("\t\t\"" + cur_type +"\": [\n");
            Sequence<Item> seq = (Sequence<Item>) utt.getSequence(cur_type);
            for (int i=0; i<(seq.size()-1); i++)
                sb.append("\t\t\t\"" + seq.get(i) + "\",\n");
            sb.append("\t\t\t\"" + seq.get(seq.size() - 1) + "\"\n");

            if (t < (types.length - 1))
                sb.append("\t\t],\n");
            else
                sb.append("\t\t]\n");
        }
        sb.append("\t},\n");

        sb.append("\t\"relations\": [\n");
        Object[] relations = utt.listAvailableRelations().toArray();
        ImmutablePair<SupportedSequenceType, SupportedSequenceType> cur_rel_id;
        SparseDoubleMatrix2D cur_rel;

        for (int r=0; r<relations.length; r++)
        {
            sb.append("\t\t{\n");
            cur_rel_id = (ImmutablePair<SupportedSequenceType, SupportedSequenceType>) relations[r];
            sb.append("\t\t\t\"source\" : \"" +cur_rel_id.left + "\",\n");
            sb.append("\t\t\t\"target\" : \"" + cur_rel_id.right + "\",\n");

            cur_rel = utt.getRelation(cur_rel_id.left, cur_rel_id.right).getRelations();
            sb.append("\t\t\t \"matrix\" : [\n");
            for (int j=0; j<cur_rel.rows(); j++)
            {
                sb.append("\t\t\t\t");
                for (int k=0; k<cur_rel.columns(); k++)
                    sb.append(cur_rel.get(j, k) + " ");sb.append(" ");
                sb.append("\n");
            }
            sb.append("\t\t\t]\n");

            if (r < (relations.length-1))
                sb.append("\t\t},\n");
            else
                sb.append("\t\t}\n");
        }
        sb.append("\t}\n");
        sb.append("}\n");

        return sb.toString();
    }

    public Utterance fromString(String content)
        throws MaryIOException
    {
        throw new UnsupportedOperationException();
    }

}


/* ROOTSJSONSerializer.java ends here */
