package marytts.data;

import java.util.ArrayList;
import java.util.List;

import marytts.data.utils.IntegerPair;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.list.IntArrayList;
import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.linalg.Algebra;

import marytts.data.item.Item;

/**
 * This class is designing the concept of Relation. A relation is just a binary
 * matrix between 2 sequences which indicates which item of the sequence
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Relation {
    /** The counter to identify a relation */
    private static int id_cpt = 0;

    /** The identifier of the relation */
    private int id;

    /** The source sequence of the relation */
    private Sequence<? extends Item> m_source_sequence;

    /** The target sequence of the relation */
    private Sequence<? extends Item> m_target_sequence;

    /** The relation matrix */
    private SparseDoubleMatrix2D m_relation_matrix;

    /**
     * The relation constructor using a matrix formatted in
     * SparseDoubleMatrix2D.
     *
     * @param source_sequence
     *            the source sequence
     * @param target_sequence
     *            the target sequence
     * @param relation_matrix
     *            the relation matrix formatted in SparseDoubleMatrix2D
     */
    public Relation(Sequence<? extends Item> source_sequence, Sequence<? extends Item> target_sequence,
                    SparseDoubleMatrix2D relation_matrix) {
        id = id_cpt++;
        setSource(source_sequence);
        setTarget(target_sequence);
        setRelations(relation_matrix);
    }

    /**
     * The relation constructor using a matrix formatted in an array of array of
     * integer.
     *
     * @param source_sequence
     *            the source sequence
     * @param target_sequence
     *            the target sequence
     * @param relation_matrix
     *            the relation matrix formatted in an array of array of integer
     */
    public Relation(Sequence<? extends Item> source_sequence, Sequence<? extends Item> target_sequence,
                    int[][] relation_matrix) {
        id = id_cpt++;
        setSource(source_sequence);
        setTarget(target_sequence);
        setRelations(relation_matrix);
    }

    /**
     * The relation constructor using a matrix formatted in an array of array of
     * double.
     *
     * @param source_sequence
     *            the source sequence
     * @param target_sequence
     *            the target sequence
     * @param relation_matrix
     *            the relation matrix formatted in an array of array of double
     */
    public Relation(Sequence<? extends Item> source_sequence, Sequence<? extends Item> target_sequence,
                    double[][] relation_matrix) {
        id = id_cpt++;
        setSource(source_sequence);
        setTarget(target_sequence);
        setRelations(relation_matrix);
    }

    /**
     * The relation constructor using a matrix formatted in a list of integer
     * pair. This pair contains the indexes of the source and target items in
     * relation.
     *
     * @param source_sequence
     *            the source sequence
     * @param target_sequence
     *            the target sequence
     * @param relation_matrix
     *            the relation matrix formatted in a sparse list of integer
     *            pair.
     */

    public Relation(Sequence<? extends Item> source_sequence, Sequence<? extends Item> target_sequence,
                    List<IntegerPair> relation_matrix) {
        id = id_cpt++;
        setSource(source_sequence);
        setTarget(target_sequence);
        setRelations(relation_matrix);
    }

    /**
     * The relation constructor of an empty relation. Therefore the matrix is
     * empty
     *
     * @param source_sequence
     *            the source sequence
     * @param target_sequence
     *            the target sequence
     */
    public Relation(Sequence<? extends Item> source_sequence,
                    Sequence<? extends Item> target_sequence) {
        id = id_cpt++;
        setSource(source_sequence);
        setTarget(target_sequence);
        setRelations(new SparseDoubleMatrix2D(source_sequence.size(), target_sequence.size()));
    }

    /********************************************************************************************
     ** Accessors
     ********************************************************************************************/
    /**
     * Internal setter to define the source sequence
     *
     * @param source_sequence
     *            the source sequence
     */
    protected void setSource(Sequence<? extends Item> source_sequence) {
        assert source_sequence.size() > 0;
        m_source_sequence = source_sequence;
        m_source_sequence.addSourceRelationReference(this);
    }

    /**
     * Method to get the source sequence
     *
     * @return the source sequence
     */
    public Sequence<? extends Item> getSource() {
        return m_source_sequence;
    }

    /**
     * Internal setter to define the target sequence
     *
     * @param target_sequence
     *            the target sequence
     */
    protected void setTarget(Sequence<? extends Item> target_sequence) {
        assert target_sequence.size() > 0;
        m_target_sequence = target_sequence;
        m_target_sequence.addTargetRelationReference(this);
    }

    /**
     * Method to get the target sequence
     *
     * @return the target sequence
     */
    public Sequence<? extends Item> getTarget() {
        return m_target_sequence;
    }

    /**
     * Internal method to set the relation matrix
     *
     * @param relation_matrix
     *            the relation matrix
     */
    protected void setRelations(SparseDoubleMatrix2D relation_matrix) {
        m_relation_matrix = relation_matrix;
    }

    /**
     * Method to set the relation matrix using an array of array of integer
     * format
     *
     * @param relation_matrix
     *            the relation matrix
     */
    public void setRelations(int[][] relation_matrix) {

        assert relation_matrix.length == getSource().size();
        assert relation_matrix.length > 0;
        assert relation_matrix[0].length == getTarget().size();

        SparseDoubleMatrix2D gen_matrix = new SparseDoubleMatrix2D(getSource().size(), getTarget().size());

        for (int i = 0; i < relation_matrix.length; i++)
            for (int j = 0; j < relation_matrix[i].length; j++)
                if (relation_matrix[i][j] > 0) {
                    gen_matrix.setQuick(i, j, 1.0);
                }

        setRelations(gen_matrix);
    }

    /**
     * Method to set the relation matrix using an array of array of double
     * format
     *
     * @param relation_matrix
     *            the relation matrix
     */
    public void setRelations(double[][] relation_matrix) {

        assert relation_matrix.length == getSource().size();
        assert relation_matrix.length > 0;
        assert relation_matrix[0].length == getTarget().size();

        SparseDoubleMatrix2D gen_matrix = new SparseDoubleMatrix2D(getSource().size(), getTarget().size());

        for (int i = 0; i < relation_matrix.length; i++)
            for (int j = 0; j < relation_matrix[i].length; j++)
                if (relation_matrix[i][j] > 0) {
                    gen_matrix.setQuick(i, j, 1.0);
                }

        setRelations(gen_matrix);
    }

    /**
     * Method to set the relation matrix using a list of pairs (source,
     * sequence) of elements which are related
     *
     * FIXME: sizes not checked !
     *
     * @param relation_pairs
     *            the list of related (source, sequence) elements indexes the
     *            relation matrix
     */
    public void setRelations(List<IntegerPair> relation_pairs) {

        SparseDoubleMatrix2D gen_matrix = new SparseDoubleMatrix2D(getSource().size(), getTarget().size());

        for (IntegerPair indexes : relation_pairs) {
            gen_matrix.setQuick(indexes.getLeft(), indexes.getRight(), 1.0);
        }
        setRelations(gen_matrix);
    }

    /**
     * Method to get the relation matrix.
     *
     * @return the relation matrix
     */
    public SparseDoubleMatrix2D getRelations() {
        return m_relation_matrix;
    }

    /**
     * Method to get the indexes of the related items of the target sequence
     * knowing the source item index.
     *
     * FIXME: not really happy with the int[]
     *
     * @param source_index
     *            the source index
     * @return an array of indexes
     */
    public int[] getRelatedIndexes(int source_index) {
        assert(source_index >= 0) && (source_index < getRelations().rows());

        IntArrayList row = new IntArrayList();
        getRelations().viewRow(source_index).getNonZeros(row, null);

        int[] indexes = new int[row.size()];
        for (int i = 0; i < row.size(); i++) {
            indexes[i] = row.get(i);
        }

        return indexes;
    }

    /**
     * Method to get the related items of the target sequence knowing the source
     * item index.
     *
     * @param source_index
     *            the source index
     * @return an array of items
     */
    public ArrayList<? extends Item> getRelatedItems(int source_index) {
        int[] indexes = getRelatedIndexes(source_index);
        ArrayList<Item> target_items = new ArrayList<Item>();
        for (int i = 0; i < indexes.length; i++) {
            target_items.add(getTarget().get(indexes[i]));
        }

        return target_items;
    }

    /**
     * Method to get the indexes of the related items of the source sequence
     * knowing the target item index.
     *
     * FIXME: not really happy with the int[]
     *
     * @param target_index
     *            the target index
     * @return an array of indexes
     */
    public int[] getSourceRelatedIndexes(int target_index) {
        assert(target_index >= 0) && (target_index < getRelations().columns());

        IntArrayList column = new IntArrayList();
        getRelations().viewColumn(target_index).getNonZeros(column, null);

        int[] indexes = new int[column.size()];
        for (int i = 0; i < column.size(); i++) {
            indexes[i] = column.get(i);
        }

        return indexes;
    }

    /********************************************************************************************
     ** Basic relation operations
     ********************************************************************************************/
    /**
     * Method to compute the reverse relation from the current one. The matrix
     * is transpose to achieve this.
     *
     * @return the reverse relation
     */
    public Relation getReverse() {
        Relation reverse = new Relation(getTarget(), getSource(),
                                        (new Algebra()).transpose(getRelations()).toArray());

        this.getTarget().addSourceRelationReference(reverse);
        this.getSource().addTargetRelationReference(reverse);

        return reverse;
    }

    /**
     * Static method to compose 2 given relations and return the results (a 3rd
     * one)
     *
     * @param rel1
     *            a relation
     * @param rel2
     *            a relation
     * @return the composed relation rel1 x rel2
     */
    public static Relation compose(Relation rel1, Relation rel2) {
        Relation composed = new Relation(rel1.getSource(), rel2.getTarget(),
                                         (new Algebra()).mult(rel1.getRelations(), rel2.getRelations()).toArray());
        rel1.getSource().addSourceRelationReference(composed);
        rel2.getTarget().addTargetRelationReference(composed);
        return composed;
    }

    /**
     * Compose the current relation with a given one
     *
     * @param rel
     *            the given relation
     * @return the composed relation this x rel
     */
    public Relation compose(Relation rel) {
        return Relation.compose(this, rel);
    }

    /********************************************************************************************
     ** Update operations
     ********************************************************************************************/
    /**
     * Remove relation between two items
     *
     * @param source_idx
     *            the index of the item in the source sequence
     * @param target_idx
     *            the index of the item in the target sequence
     */
    public void removeRelation(int source_idx, int target_idx) {
        getRelations().set(source_idx, target_idx, 0);
    }

    /**
     * Add relation between two items
     *
     * @param source_idx
     *            the index of the item in the source sequence
     * @param target_idx
     *            the index of the item in the target sequence
     */
    public void addRelation(int source_idx, int target_idx) {
        getRelations().set(source_idx, target_idx, 1);
    }

    /**
     * Method to remove a source item knowing its index
     *
     * /!\ Only to meant to be called by the sequence class /!\
     *
     * @param source_idx
     *            the index of the source item to remove
     */
    public void removeSourceItem(int source_idx) {
        double[][] val = getRelations().toArray();
        assert(getSource().size() > source_idx);
        assert(val.length > source_idx);

        // Reset the current column just to be sure
        for (int j = 0; j < val[source_idx].length; j++) {
            getRelations().setQuick(source_idx, j, 0);
        }

        // Translate the impacted part of the matrix
        for (int i = (source_idx + 1); i < val.length; i++) {
            for (int j = 0; j < val[i].length; j++) {
                if (val[i][j] > 0) {
                    // Reset current cell value
                    getRelations().setQuick(i, j, 0);

                    // Update the previous "source" index relation
                    getRelations().setQuick(i - 1, j, 1);
                }
            }
        }
    }

    /**
     * Adapt the matrix to take into account a new item on the source sequence
     * at the specified index
     *
     * /!\ Only to meant to be called by the sequence class /!\
     *
     * @param new_item_idx
     *            the index of the new item in the source sequence
     */
    public void addSourceItem(int new_item_idx) {
        addSourceItem(new_item_idx, false);
    }

    /**
     * Adapt the matrix to take into account a new item on the source sequence
     * at the specified index
     *
     * /!\ Only to meant to be called by the sequence class /!\
     *
     * @param new_item_idx
     *            the index of the new item in the source sequence
     * @param expand_relation
     *            true leads to the expansion of the relation, false doesn't
     *            change the <b>shape</b> relation
     */
    public void addSourceItem(int new_item_idx, boolean expand_relation) {
        double[][] val = getRelations().toArray();
        SparseDoubleMatrix2D new_matrix = new SparseDoubleMatrix2D(getSource().size(), getTarget().size());

        // Copy the unmodified part
        for (int i = 0; i < new_item_idx; i++)
            for (int j = 0; j < val[i].length; j++)
                if (val[i][j] > 0) {
                    new_matrix.setQuick(i, j, 1);
                }

        // If the user wants to insert an item and duplicate relation
        if (expand_relation) {
            for (int j = 0; j < val[new_item_idx].length; j++) {
                if (new_item_idx == val.length) {
                    new_matrix.setQuick(new_item_idx, j, val[new_item_idx - 1][j]);
                } else if (val[new_item_idx][j] > 0) {
                    new_matrix.setQuick(new_item_idx, j, 1);
                }
            }
        }

        if (new_item_idx >= 0) {
            // Translate the impacted part of the matrix
            for (int i = new_item_idx; i < val.length; i++) {
                for (int j = 0; j < val[i].length; j++) {
                    if (val[new_item_idx][j] > 0) {
                        new_matrix.setQuick(i + 1, j, 1);
                    }
                }
            }
        }

        setRelations(new_matrix);
    }

    /**
     * Method to remove a target item knowing its index
     *
     * /!\ Only to meant to be called by the sequence class /!\
     *
     * @param target_idx
     *            the index of the target item to remove
     */
    public void removeTargetItem(int target_idx) {
        double[][] val = getRelations().toArray();
        assert(val.length > 0) && (val[0].length > target_idx) && (getTarget().size() > target_idx);

        // Reset the current row just to be sure
        for (int i = 0; i < val.length; i++) {
            getRelations().setQuick(i, target_idx, 0);
        }

        // Translate the impacted part of the matrix
        for (int i = 0; i < val.length; i++) {
            for (int j = target_idx + 1; j < val[i].length; j++) {
                if (val[i][j] > 0) {
                    // Reset current cell value
                    getRelations().setQuick(i, j, 0);

                    // Update the previous "source" index relation
                    getRelations().setQuick(i - 1, j, 1);
                }
            }
        }
    }

    /**
     * Adapt the matrix to take into account a new item on the target sequence
     * at the specified index. The relation is not expanded in this case
     *
     * /!\ Only to meant to be called by the sequence class /! \
     *
     * @param new_item_idx
     *            the index of the new item in the target sequence
     */
    public void addTargetItem(int new_item_idx) {
        addTargetItem(new_item_idx, false);
    }

    /**
     * Adapt the matrix to take into account a new item on the target sequence
     * at the specified index
     *
     * /!\ Only to meant to be called by the sequence class /! \
     *
     * @param new_item_idx
     *            the index of the new item in the target sequence
     * @param expand_relation
     *            true leads to the expansion of the relation, false doesn't
     *            change the <b>shape</b> relation
     */
    public void addTargetItem(int new_item_idx, boolean expand_relation) {
        double[][] val = getRelations().toArray();
        SparseDoubleMatrix2D new_matrix = new SparseDoubleMatrix2D(getSource().size(), getTarget().size());

        // Copy the unmodified part
        for (int i = 0; i < val.length; i++)
            for (int j = 0; j < new_item_idx; j++)
                if (val[i][j] > 0) {
                    new_matrix.setQuick(i, j, 1);
                }

        // If the user wants to insert an item and duplicate relation
        if (expand_relation) {
            for (int i = 0; i < val.length; i++) {
                if (new_item_idx == val[i].length) {
                    new_matrix.setQuick(i, new_item_idx, val[i][new_item_idx - 1]);
                } else if (val[i][new_item_idx] > 0) {
                    new_matrix.setQuick(i, new_item_idx, 1);
                }
            }
        }

        // Translate the impacted part of the matrix
        if (new_item_idx >= 0) {
            for (int i = 0; i < val.length; i++) {
                for (int j = new_item_idx; j < val[i].length; j++) {
                    if (val[i][j] > 0) {
                        new_matrix.setQuick(i, j + 1, 1);
                    }
                }
            }
        }

        setRelations(new_matrix);
    }

    /********************************************************************************************
     ** Object method overriding
     ********************************************************************************************/
    /**
     * Method to generate a hash for the relation. The hash is actually the id
     * (see the id property) of the relation
     *
     * @return a hash representation of the utterance
     */
    @Override
    public int hashCode() {
        return id;
    }

    /**
     * Method to compare an object to the current relation
     *
     * @param obj
     *            the object to compare
     * @return true if obj is a relation and the hashcode are equals, false else
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        return (getSource().hashCode() == (((Relation) obj).getSource().hashCode())
                && getTarget().hashCode() == (((Relation) obj).getTarget().hashCode()));
    }

    /**
     * Method to generate a string representation of the current relation
     *
     * @return the string representation of the current relation
     */
    @Override
    public String toString() {
        String message = "source = " + getSource().toString() + "\n";
        message += "target = " + getTarget().toString() + "\n";

        message += "Relation now = " + "\n";
        message += getRelations().toString();
        return message;
    }
}
