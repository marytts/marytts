package marytts.data;

import java.util.ArrayList;

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
    private Sequence<Item> source_sequence;
    private Sequence<Item> target_sequence;
    private SparseDoubleMatrix2D relation_matrix;

    public Relation(Sequence<Item> source_sequence, Sequence<Item> target_sequence,
                    SparseDoubleMatrix2D relation_matrix)
    {
        setSource(source_sequence);
        setTarget(target_sequence);
        setRelations(relation_matrix);
    }

    public Relation(Sequence<Item> source_sequence, Sequence<Item> target_sequence,
                    int[][] relation_matrix)
    {
        setSource(source_sequence);
        setTarget(target_sequence);
        setRelations(relation_matrix);
    }


    public Relation(Sequence<Item> source_sequence, Sequence<Item> target_sequence)
    {
        setSource(source_sequence);
        setTarget(target_sequence);
    }

    /********************************************************************************************
     ** Getters/setters
     ********************************************************************************************/
    protected void setSource(Sequence<Item> source_sequence)
    {
        this.source_sequence = source_sequence;
    }

    public Sequence<Item> getSource()
    {
        return source_sequence;
    }

    protected void setTarget(Sequence<Item> target_sequence)
    {
        this.target_sequence = target_sequence;
    }

    public Sequence<Item> getTarget()
    {
        return target_sequence;
    }

    public void setRelations(SparseDoubleMatrix2D relation_matrix)
    {
        this.relation_matrix = relation_matrix;
    }

    public void setRelations(int[][] relation_matrix)
    {

        assert relation_matrix.length == source_sequence.size();
        assert relation_matrix.length > 0;
        assert relation_matrix[0].length == target_sequence.size();

        SparseDoubleMatrix2D gen_matrix = new SparseDoubleMatrix2D(source_sequence.size(),
                                                                   target_sequence.size());

        for (int i=0; i<relation_matrix.length; i++)
            for (int j=0;j<relation_matrix[i].length; j++)
                if (relation_matrix[i][j] > 0)
                    gen_matrix.setQuick(i, j, 1.0);

        setRelations(gen_matrix);
    }

    private SparseDoubleMatrix2D getRelations()
    {
        return relation_matrix;
    }

    /********************************************************************************************
     ** Getters/setters
     ********************************************************************************************/
    public int[] getRelatedIndexes(int source_index)
    {
        assert (source_index >= 0) && (source_index < relation_matrix.rows());

        IntArrayList row = new IntArrayList();
        getRelations().viewRow(source_index).getNonZeros(row, null);

        return row.elements();

    }

    public ArrayList<Item> getRelatedItems(int source_index)
    {
        int[] indexes = getRelatedIndexes(source_index);
        ArrayList<Item> target_items = new ArrayList<Item>();
        for (int i=0; i<indexes.length; i++)
        {
            target_items.add(getTarget().get(i));
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
}
