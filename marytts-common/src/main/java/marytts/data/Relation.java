package marytts.data;

import java.util.ArrayList;
import java.util.List;


import marytts.data.utils.IntegerPair;
import marytts.data.utils.SequenceTypePair;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.linalg.Algebra;

import marytts.data.item.Item;


/**
 * This class is designing the concept of Relation. A relation is just a binary matrix between 2
 * sequences which indicates which item of the sequence
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Relation
{
    private static int id_cpt = 0;
    private int id;
    private Sequence<? extends Item> m_source_sequence;
    private Sequence<? extends Item> m_target_sequence;
    private SparseDoubleMatrix2D m_relation_matrix;

    public Relation(Sequence<? extends Item> source_sequence,
                    Sequence<? extends Item> target_sequence,
                    SparseDoubleMatrix2D relation_matrix)
    {
        id = id_cpt++;
        setSource(source_sequence);
        setTarget(target_sequence);
        setRelations(relation_matrix);
    }

    public Relation(Sequence<? extends Item> source_sequence,
                    Sequence<? extends Item> target_sequence,
                    int[][] relation_matrix)
    {
        id = id_cpt++;
        setSource(source_sequence);
        setTarget(target_sequence);
        setRelations(relation_matrix);
    }


    public Relation(Sequence<? extends Item> source_sequence,
                    Sequence<? extends Item> target_sequence,
                    List<IntegerPair> relation_matrix)
    {
        id = id_cpt++;
        setSource(source_sequence);
        setTarget(target_sequence);
        setRelations(relation_matrix);
    }


    public Relation(Sequence<? extends Item> source_sequence,
                    Sequence<? extends Item> target_sequence)
    {
        id = id_cpt++;
        setSource(source_sequence);
        setTarget(target_sequence);
    }

    /********************************************************************************************
     ** Getters/setters
     ********************************************************************************************/
    protected void setSource(Sequence<? extends Item> source_sequence)
    {
        assert source_sequence.size() > 0;
        m_source_sequence = source_sequence;
        m_source_sequence.addSourceRelationReference(this);
    }

    public Sequence<? extends Item> getSource()
    {
        return m_source_sequence;
    }

    protected void setTarget(Sequence<? extends Item> target_sequence)
    {
        assert target_sequence.size() > 0;
        m_target_sequence = target_sequence;
        m_target_sequence.addTargetRelationReference(this);
    }

    public Sequence<? extends Item> getTarget()
    {
        return m_target_sequence;
    }

    public void setRelations(SparseDoubleMatrix2D relation_matrix)
    {
        m_relation_matrix = relation_matrix;
    }

    public void setRelations(int[][] relation_matrix)
    {

        assert relation_matrix.length == getSource().size();
        assert relation_matrix.length > 0;
        assert relation_matrix[0].length == getTarget().size();

        SparseDoubleMatrix2D gen_matrix = new SparseDoubleMatrix2D(getSource().size(),
                                                                   getTarget().size());

        for (int i=0; i<relation_matrix.length; i++)
            for (int j=0;j<relation_matrix[i].length; j++)
                if (relation_matrix[i][j] > 0)
                    gen_matrix.setQuick(i, j, 1.0);

        setRelations(gen_matrix);
    }

    /**
     * FIXME: sizes not checked !
     *
     */
    public void setRelations(List<IntegerPair> relation_pairs)
    {

        SparseDoubleMatrix2D gen_matrix = new SparseDoubleMatrix2D(getSource().size(),
                                                                   getTarget().size());

        for (IntegerPair indexes:relation_pairs)
        {
            gen_matrix.setQuick(indexes.getLeft(), indexes.getRight(), 1.0);
        }
        setRelations(gen_matrix);
    }

    private SparseDoubleMatrix2D getRelations()
    {
        return m_relation_matrix;
    }

    /********************************************************************************************
     ** Getters/setters
     ********************************************************************************************/
    /**
     * FIXME: not really happy with the int[]
     *
     */
    public int[] getRelatedIndexes(int source_index)
    {
        assert (source_index >= 0) && (source_index < getRelations().rows());

        IntArrayList row = new IntArrayList();
        getRelations().viewRow(source_index).getNonZeros(row, null);

        int[] indexes = new int[row.size()];
        for (int i=0; i<row.size(); i++)
            indexes[i] = row.get(i);

        return indexes;
    }

    public ArrayList<? extends Item> getRelatedItems(int source_index)
    {
        int[] indexes = getRelatedIndexes(source_index);
        ArrayList<Item> target_items = new ArrayList<Item>();
        for (int i=0; i<indexes.length; i++)
        {
            target_items.add(getTarget().get(indexes[i]));
        }

        return target_items;
    }

    public Relation getReverse()
    {
        return new Relation(getTarget(), getSource(),
                            (SparseDoubleMatrix2D) (new Algebra()).transpose(getRelations()));
    }


    public static Relation compose(Relation rel1, Relation rel2)
    {
        return new Relation(rel1.getSource(), rel2.getTarget(),
                            (SparseDoubleMatrix2D) (new Algebra()).mult(rel1.getRelations(),
                                                                        rel2.getRelations()));
    }

	@Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        if (obj == null)
            return false;

        if (getClass() != obj.getClass())
            return false;

        return (getSource().equals(((Relation) obj).getSource()) &&
                getTarget().equals(((Relation) obj).getTarget()));
    }

    @Override
    public String toString()
    {
        String message = "source = " + getSource().toString() + "\n";
        message += "target = " + getTarget().toString() + "\n";

        return message;
    }

    @Override
    public int hashCode()
    {
        return id;
    }
}
